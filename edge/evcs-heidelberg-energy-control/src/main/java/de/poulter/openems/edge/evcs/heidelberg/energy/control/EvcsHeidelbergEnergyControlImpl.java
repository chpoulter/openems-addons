/*
 *   OpenEMS EVCS Heidelberg Energy Control bundle
 *
 *   Written by Christian Poulter
 *   Copyright (C) 2025 Christian Poulter <devel(at)poulter.de>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *   SPDX-License-Identifier: AGPL-3.0-or-later
 *
 */

package de.poulter.openems.edge.evcs.heidelberg.energy.control;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

import java.time.Instant;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.CalculateEnergySession;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(
    name = "Evcs.Heidelberg.Energy.Control",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@EventTopics({
    EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE,
    EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
    EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE
})
public class EvcsHeidelbergEnergyControlImpl extends AbstractOpenemsModbusComponent implements
    EvcsHeidelbergEnergyControl, ManagedEvcs, Evcs, ElectricityMeter, ModbusComponent, EventHandler, OpenemsComponent
{

    private static final Logger log = LoggerFactory.getLogger(EvcsHeidelbergEnergyControlImpl.class);
    private static final String MODBUS_SETTER_REFERENCE = "Modbus";
    private static final int STANDBY_FUNCTION_CONTROL = 0x04;    // disables standby function 

    @Reference
    private EvcsPower evcsPower;

    @Reference
    private ConfigurationAdmin configurationAdmin;

    private Instant nextConfigurationApply = Instant.MAX;
    private Config config;
    private PhaseRotation phaseRotation;

    private final WriteHandler writeHandler;
    private final ChargeStateHandler chargeStateHandler;
    private final CalculateEnergySession calculateEnergySession;

    public EvcsHeidelbergEnergyControlImpl() {
        super(
            OpenemsComponent.ChannelId.values(),
            ModbusComponent.ChannelId.values(),
            ElectricityMeter.ChannelId.values(),
            Evcs.ChannelId.values(),
            ManagedEvcs.ChannelId.values(),
            EvcsHeidelbergEnergyControl.ChannelId.values()
        );

        calculateEnergySession = new CalculateEnergySession(this);
        writeHandler = new WriteHandler(this);
        chargeStateHandler = new ChargeStateHandler(this);

        ElectricityMeter.calculateAverageVoltageFromPhases(this);
        Evcs.calculatePhasesFromActivePowerAndPhaseCurrents(this);
        Evcs.addCalculatePowerLimitListeners(this);
        CalculateUsedPhasesFromVoltage.add(this);
        ChargingStateProcessor.add(this);

        getActivePowerChannel().onSetNextValue( power -> _setChargePower(power.get()) );
    }

    @Override
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsException {
        this.config = config;

        phaseRotation = config.phaseRotation();
        nextConfigurationApply = Instant.MIN;

        _setMinimumPower(EvcsUtils.milliampereToWatt(config.minCurrent(), Phases.THREE_PHASE.getValue()));
        _setMaximumPower(EvcsUtils.milliampereToWatt(config.maxCurrent(), Phases.THREE_PHASE.getValue()));
        _setChargingType(ChargingType.AC);

        super.activate(
            context,
            config.id(),
            config.alias(),
            config.enabled(),
            config.modbusUnitId(),
            configurationAdmin,
            MODBUS_SETTER_REFERENCE,
            config.modbus_id()
        );
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();

        writeHandler.cancelChargePower();
    }

    @Override
    public PhaseRotation getPhaseRotation() {
        return phaseRotation;
    }

    @Override
    public MeterType getMeterType() {
        if (config.readOnly()) {
            return MeterType.CONSUMPTION_METERED;
        } else {
            return MeterType.MANAGED_CONSUMPTION_METERED;
        }
    }

    @Override
    public EvcsPower getEvcsPower() {
        return evcsPower;
    }

    @Override
    public ChargeStateHandler getChargeStateHandler() {
        return chargeStateHandler;
    }

    @Override
    public boolean getConfiguredDebugMode() {
        return config.debugMode();
    }

    @Override
    public int getMinimumTimeTillChargingLimitTaken() {
        return config.minimumTimeTillChargingLimitTaken();
    }

    @Override
    public void applyConfigurationNow() {
        nextConfigurationApply = Instant.MIN;
    }

    @Override
    public void handleEvent(Event event) {
        if (!isEnabled()) return;

        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
                handleTopicCycleAfterProcessImage();
                break;

            case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
                handleTopicCycleExecuteWrite();
                break;

        }
    }

    private void handleTopicCycleAfterProcessImage() {
        calculateEnergySession.update(getPlug().isConnected());

        _setChargingstationCommunicationFailed(getModbusCommunicationFailed());
        _setError(getStatus() == Status.ERROR);

        applyEvcsConfiguration();
    }

    private void handleTopicCycleExecuteWrite() {
        if (config.readOnly()) return;

        writeHandler.run();
    }

    private void applyEvcsConfiguration() {
        if (Instant.now().isBefore(nextConfigurationApply)) return;
        nextConfigurationApply = Instant.now().plusSeconds(config.configurationIntervall());

        logDebug("Applying configuration.");

        try {
            setWatchdogTimeout(config.watchdogTimeout() * 1000);
            setFailsafeCurrent(config.failsafeCurrent());
            setStandby(STANDBY_FUNCTION_CONTROL);

        } catch (OpenemsError.OpenemsNamedException ex) {
            log.error("Could not apply configuration settings.", ex);
        }
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {

        return new ModbusProtocol(this,

            new FC4ReadInputRegistersTask(5, Priority.HIGH,
                m(EvcsHeidelbergEnergyControl.ChannelId.CHARGING_STATE,       new UnsignedWordElement( 5), ElementToChannelConverters.CHARGING_STATE),
                m(phaseRotation.channelCurrentL1(),                           new UnsignedWordElement( 6), SCALE_FACTOR_2),
                m(phaseRotation.channelCurrentL2(),                           new UnsignedWordElement( 7), SCALE_FACTOR_2),
                m(phaseRotation.channelCurrentL3(),                           new UnsignedWordElement( 8), SCALE_FACTOR_2),
                m(EvcsHeidelbergEnergyControl.ChannelId.PCB_TEMPERATURE,      new SignedWordElement  ( 9), SCALE_FACTOR_MINUS_1),
                m(phaseRotation.channelVoltageL1(),                           new UnsignedWordElement(10), SCALE_FACTOR_3),
                m(phaseRotation.channelVoltageL2(),                           new UnsignedWordElement(11), SCALE_FACTOR_3),
                m(phaseRotation.channelVoltageL3(),                           new UnsignedWordElement(12), SCALE_FACTOR_3),
                m(EvcsHeidelbergEnergyControl.ChannelId.EXTERN_LOCK_STATE,    new UnsignedWordElement(13), ElementToChannelConverters.LOCK_STATE),
                m(ElectricityMeter.ChannelId.ACTIVE_POWER,                    new UnsignedWordElement(14)),
                new DummyRegisterElement(15),
                new DummyRegisterElement(16),
                m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(17))
            ),

            new FC4ReadInputRegistersTask(100, Priority.LOW,
                m(Evcs.ChannelId.FIXED_MAXIMUM_HARDWARE_POWER, new UnsignedWordElement(100), ElementToChannelConverters.AMPERE_TO_WATT),
                m(Evcs.ChannelId.FIXED_MINIMUM_HARDWARE_POWER, new UnsignedWordElement(101), ElementToChannelConverters.AMPERE_TO_WATT)
            ),

            new FC3ReadRegistersTask(261, Priority.HIGH,
                m(EvcsHeidelbergEnergyControl.ChannelId.MAX_CURRENT, new UnsignedWordElement(261), SCALE_FACTOR_2)
            ),

            new FC3ReadRegistersTask(257, Priority.LOW,
                m(EvcsHeidelbergEnergyControl.ChannelId.WATCHDOG_TIMEOUT, new UnsignedWordElement(257)),
                m(EvcsHeidelbergEnergyControl.ChannelId.STANDBY,          new UnsignedWordElement(258)),
                new DummyRegisterElement(259, 261),
                m(EvcsHeidelbergEnergyControl.ChannelId.FAILSAFE_CURRENT, new UnsignedWordElement(262))
            ),

            new FC6WriteRegisterTask(257, m(EvcsHeidelbergEnergyControl.ChannelId.WATCHDOG_TIMEOUT, new UnsignedWordElement(257))),
            new FC6WriteRegisterTask(258, m(EvcsHeidelbergEnergyControl.ChannelId.STANDBY,          new UnsignedWordElement(258))),
            new FC6WriteRegisterTask(261, m(EvcsHeidelbergEnergyControl.ChannelId.MAX_CURRENT,      new UnsignedWordElement(261), SCALE_FACTOR_2)),
            new FC6WriteRegisterTask(262, m(EvcsHeidelbergEnergyControl.ChannelId.FAILSAFE_CURRENT, new UnsignedWordElement(262)))
        );
    }


    @Override
    public int getConfiguredMinimumHardwarePower() {
        Value<Integer> fixedMinimumHardwarePower = getFixedMinimumHardwarePower();

        return fixedMinimumHardwarePower.isDefined() ?
            fixedMinimumHardwarePower.get()
            :
            EvcsUtils.milliampereToWatt(config.minCurrent(), getPhasesAsInt());
    }

    @Override
    public int getConfiguredMaximumHardwarePower() {
        Value<Integer> fixedMaximumHardwarePower = getFixedMaximumHardwarePower();

        return fixedMaximumHardwarePower.isDefined() ?
            fixedMaximumHardwarePower.get()
            :
            EvcsUtils.milliampereToWatt(config.maxCurrent(), getPhasesAsInt());
    }

    @Override
    public boolean applyChargePowerLimit(int power) throws Exception {
        if (config.readOnly()) return false;

        int current = EvcsUtils.wattToMilliampere(power, getPhasesAsInt());

        setMaxCurrent(current);
        _setSetChargePowerLimit(power);

        logInfo(log, "applyChargePowerLimit ------------------> " + power + "W => "+ current + "mA");

        return true;
    }

    @Override
    public boolean pauseChargeProcess() throws Exception {
        logInfo(log, "pauseChargeProcess");

        setMaxCurrent(0);
        _setSetChargePowerLimit(0);

        return true;
    }

    @Override
    public boolean applyDisplayText(String text) throws OpenemsException {
        logInfo(log, "applyDisplayText: " + text);

        return true;
    }

    @Override
    public void logDebug(String message) {
        logDebug(log, message);
    }

    @Override
    public String debugLog() {
        return "Limit: " + getSetChargePowerLimit().orElse(null) + "|Power: " + getActivePower().orElse(null) + "|Status: " + getStatus().getName();
    }

}