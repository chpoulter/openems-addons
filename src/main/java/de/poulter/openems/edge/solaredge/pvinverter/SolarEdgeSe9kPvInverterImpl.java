/*
 *   OpenEMS PvInverter SolarEdge Se9k bundle
 *
 *   Written by Christian Poulter.
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

package de.poulter.openems.edge.solaredge.pvinverter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

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

import com.google.common.collect.ImmutableMap;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.sunspec.AbstractSunSpecPvInverter;
import io.openems.edge.pvinverter.sunspec.SunSpecPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(
    name = "PvInverter.SolarEdge.Se9k",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = { "type=PRODUCTION" }
)
@EventTopics({ EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE })
public class SolarEdgeSe9kPvInverterImpl extends AbstractSunSpecPvInverter implements
    SolarEdgeSe9kPvInverter, SunSpecPvInverter, ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave
{

    private static final Logger log = LoggerFactory.getLogger(SolarEdgeSe9kPvInverterImpl.class);

    // SunSpec models supported by SE9K
    private static final int READ_FROM_MODBUS_BLOCK = 1;
    private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
        .put(DefaultSunSpecModel.S_1, Priority.LOW)
        .put(DefaultSunSpecModel.S_101, Priority.LOW)
        .put(DefaultSunSpecModel.S_102, Priority.LOW)
        .put(DefaultSunSpecModel.S_103, Priority.LOW)
        .build();

    // for limit handling
    private Instant lastHandleActivePowerLimit = Instant.MIN;
    private WeightedMean weightedMean = new WeightedMean(15d, 15d, 15d, 15d, 20d, 30d, 40d, 50d, 75d, 100d);

    @Reference
    private ConfigurationAdmin cm;

    private boolean readOnly;
    private boolean debugMode;

    @Override
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public SolarEdgeSe9kPvInverterImpl() {
        super(
            ACTIVE_MODELS,
            OpenemsComponent.ChannelId.values(),
            ModbusComponent.ChannelId.values(),
            ElectricityMeter.ChannelId.values(),
            ManagedSymmetricPvInverter.ChannelId.values(),
            SunSpecPvInverter.ChannelId.values(),
            SolarEdgeSe9kPvInverter.ChannelId.values()
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsException {
        super.activate(
            context,
            config.id(), config.alias(), config.enabled(), config.readOnly(), config.modbusUnitId(),
            this.cm, "Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK, config.phase()
        );

        this.readOnly = config.readOnly();
        this.debugMode = config.debugMode();
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (!(isEnabled() && isSunSpecInitializationCompleted())) {
            _setPvLimitFailed(false);
            _setReadOnlyModePvLimitFailed(false);
            return;
        }

        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
                processTopicCycleExecuteWrite();

            default:
                break;
        }
    }

    private void processTopicCycleExecuteWrite() {
        Optional<Integer> activePowerLimitValue = getActivePowerLimitChannel().getNextWriteValueAndReset();

        if (readOnly) {
            logWarn(log, "Cannot set active power limit on read only pv-inverter.");
            _setReadOnlyModePvLimitFailed(activePowerLimitValue.isPresent());
            return;
        }

        _setReadOnlyModePvLimitFailed(false);

        try {
            handleActivePowerLimit(activePowerLimitValue);
            _setPvLimitFailed(false);

        } catch (OpenemsNamedException ex) {
            log.warn("Could not handle active power limit.", ex);
            logWarn(log, "Could not handle active power limit.");
            _setPvLimitFailed(true);
        }
    }

    @Override
    public String debugLog() {
        return "L:" + this.getActivePower().asString() + "|pc_rrcr_state: " + getPcRrcrState().asString();
    }

    @Override
    protected void onSunSpecInitializationCompleted() {
        super.onSunSpecInitializationCompleted();

        logDebug("Initializing SE9K specific registers...");

        getModbusProtocol().addTasks(
            TaskBuilder.buildFC3ReadRegistersTask(
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_RRCR_STATE, new UnsignedWordElement(0xF000)),
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_ACTIVE_POWER_LIMIT, new UnsignedWordElement(0xF001)),
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_COS_PHI, new FloatDoublewordElement(0xF002).wordOrder(WordOrder.LSWMSW))
            ),
            TaskBuilder.buildFC3ReadRegistersTask(
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_PWR_FRQ_DERATING_CONFIG, new SignedDoublewordElement(0xF102).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_REACTIVE_PWR_CONFIG, new SignedDoublewordElement(0xF104).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_REACT_PW_ITER_TIME, new UnsignedDoublewordElement(0xF106).wordOrder(WordOrder.LSWMSW))
            ),
            TaskBuilder.buildFC3ReadRegistersTask(
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_ADVANCED_PWR_CONTROL_EN, new SignedDoublewordElement(0xF142).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_FRT_EN, new SignedDoublewordElement(0xF144).wordOrder(WordOrder.LSWMSW))
            ),
            TaskBuilder.buildFC3ReadRegistersTask(Priority.HIGH,
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_ENABLE_DPC, new UnsignedWordElement(0xF300)),
                new DummyRegisterElement(0xF301, 0xF303),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_MAX_ACTIVE_POWER, new FloatDoublewordElement(0xF304).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_MAX_REACTIVE_POWER, new FloatDoublewordElement(0xF306).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_ACTIVE_REACTIVE_PREF, new UnsignedWordElement(0xF308)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_COSPHI_Q_PREF, new UnsignedWordElement(0xF309)),
                new DummyRegisterElement(0xF30A, 0xF30B),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_ACTIVE_POWER_LIMIT, new FloatDoublewordElement(0xF30C).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_REACTIVE_POWER_LIMIT, new FloatDoublewordElement(0xF30E).wordOrder(WordOrder.LSWMSW))
            )
        );

        getModbusProtocol().addTasks(
            TaskBuilder.buildFC3ReadRegistersTaskFC16WriteRegistersTask(
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_DYNAMIC_ACTIVE_POWER_LIMIT, new FloatDoublewordElement(0xF322).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_DYNAMIC_REACTIVE_POWER_REF, new FloatDoublewordElement(0xF324).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_DYNAMIC_COSPHI_REF,         new FloatDoublewordElement(0xF326).wordOrder(WordOrder.LSWMSW))
            )
        );

        logDebug("Initializing SE9K specific registers finished.");
    }

    @Override
    public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
        return new ModbusSlaveTable(
            OpenemsComponent.getModbusSlaveNatureTable(accessMode),
            ElectricityMeter.getModbusSlaveNatureTable(accessMode),
            ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode),
            ModbusSlaveNatureTable.of(SolarEdgeSe9kPvInverterImpl.class, accessMode, 100).build()
        );
    }

    public void handleActivePowerLimit(Optional<Integer> activePowerLimitOpt) throws OpenemsNamedException {

        // only run every 10 seconds
        if (Duration.between(lastHandleActivePowerLimit, Instant.now()).getSeconds() < 10) {
            return;
        }
        lastHandleActivePowerLimit = Instant.now();

        // check for hardware maximum
        Value<Float> epcMaxActivePowerValue = getEpcMaxActivePower();
        if (!epcMaxActivePowerValue.isDefined()) {
            logWarn(log, getEpcMaxActivePowerChannel().channelId() + " has no value.");
            return;
        }
        double epcMaxActivePower = getEpcMaxActivePower().orElse(0.0f);

        // calculate limit
        double epcDynamicActivePowerLimit = 100.0d;
        if (activePowerLimitOpt.isPresent()) {
            double activePowerLimit = activePowerLimitOpt.get();
            epcDynamicActivePowerLimit = activePowerLimit * 100 / epcMaxActivePower;
        }
        logDebug("epcDynamicActivePowerLimit " + epcDynamicActivePowerLimit);

        // fit in boundaries
        epcDynamicActivePowerLimit = TypeUtils.fitWithin(1.0d, 100.0d, epcDynamicActivePowerLimit);
        logDebug("epcDynamicActivePowerLimit " + epcDynamicActivePowerLimit);

        // apply mean
        weightedMean.addValue(epcDynamicActivePowerLimit);
        epcDynamicActivePowerLimit = weightedMean.getMean();
        logDebug("epcDynamicActivePowerLimit " + epcDynamicActivePowerLimit);

        setEpcDynamicActivePowerLimit((float) epcDynamicActivePowerLimit);
        setEpcDynamicReactivePowerLimit(100f);
        setEpcDynamicCosPhiRef(1f);
    }

    public void logDebug(String message) {
        if (debugMode) {
            logInfo(log, message);
        }
    }

}
