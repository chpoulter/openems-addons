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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

import de.poulter.openems.lib.mean.WeightedMean;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
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
@EventTopics({ EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS })
public class SolarEdgeSe9kPvInverterImpl extends AbstractSunSpecPvInverter implements
    SolarEdgeSe9kPvInverter, SunSpecPvInverter, ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave
{
    private static final Logger log = LoggerFactory.getLogger(SolarEdgeSe9kPvInverterImpl.class);

    // SunSpec models supported by SE9K
    private static final int READ_FROM_MODBUS_BLOCK = 1;
    private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
        .put(DefaultSunSpecModel.S_1, Priority.LOW)
        .put(DefaultSunSpecModel.S_101, Priority.HIGH)
        .put(DefaultSunSpecModel.S_102, Priority.HIGH)
        .put(DefaultSunSpecModel.S_103, Priority.HIGH)
        .build();

    // for limit handling
    private WeightedMean activePowerLimitWeightedMean = new WeightedMean(15d, 15d, 15d, 15d, 20d, 30d, 40d, 50d, 75d, 100d);

    @Reference
    private ConfigurationAdmin cm;

    private boolean readOnly;
    private boolean debugMode;
    private float reactivePowerRef;
    private float cosPhiRef;

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

        // map channels
        this.<Channel<Float>>channel(SolarEdgeSe9kPvInverter.ChannelId.EPC_MAX_ACTIVE_POWER).onSetNextValue(nextValue -> {
            nextValue.ifPresent(value -> _setMaxActivePower(value.intValue()));
        });

        this.<Channel<Float>>channel(SolarEdgeSe9kPvInverter.ChannelId.EPC_MAX_REACTIVE_POWER).onSetNextValue(nextValue -> {
            nextValue.ifPresent(value -> _setMaxReactivePower(value.intValue()));
        });
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
        this.reactivePowerRef = config.reactivePowerRef();
        this.cosPhiRef = config.cosPhiRef();
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
                break;

            case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
                processTopicCycleAfterControllers();
                break;

            default:
                break;
        }
    }

    private void processTopicCycleAfterControllers() {
        Optional<Float> activePowerLimitValue = this.getOverrideActivePowerChannel().getNextWriteValueAndReset();
        if (activePowerLimitValue.isPresent()) {
            _setOverrideActivePower(activePowerLimitValue.get());
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
            applyEpcDynamicLimits(activePowerLimitValue);
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

        logInfo(log, "Initializing SE9K specific registers...");

        getModbusProtocol().addTasks(
            TaskBuilder.buildFC3ReadRegistersTask(
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_RRCR_STATE,              new UnsignedWordElement(0xF000)),
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_ACTIVE_POWER_LIMIT,      new UnsignedWordElement(0xF001)),
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_COS_PHI,                 new FloatDoublewordElement(0xF002).wordOrder(WordOrder.LSWMSW))
            ),

            TaskBuilder.buildFC3ReadRegistersTask(
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_PWR_FRQ_DERATING_CONFIG, new SignedDoublewordElement(0xF102).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_REACTIVE_PWR_CONFIG,     new SignedDoublewordElement(0xF104).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_REACT_PW_ITER_TIME,      new UnsignedDoublewordElement(0xF106).wordOrder(WordOrder.LSWMSW))
            ),

            TaskBuilder.buildFC3ReadRegistersTask(
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_ADVANCED_PWR_CONTROL_EN, new SignedDoublewordElement(0xF142).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.PC_FRT_EN,                  new SignedDoublewordElement(0xF144).wordOrder(WordOrder.LSWMSW))
            ),

            TaskBuilder.buildFC3ReadRegistersTask(
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_ENABLE_DPC,             new UnsignedWordElement(0xF300)),
                new DummyRegisterElement(0xF301, 0xF303),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_MAX_ACTIVE_POWER,       new FloatDoublewordElement(0xF304).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_MAX_REACTIVE_POWER,     new FloatDoublewordElement(0xF306).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_ACTIVE_REACTIVE_PREF,   new UnsignedWordElement(0xF308)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_COSPHI_Q_PREF,          new UnsignedWordElement(0xF309)),
                new DummyRegisterElement(0xF30A, 0xF30B),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_ACTIVE_POWER_LIMIT,     new FloatDoublewordElement(0xF30C).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_REACTIVE_POWER_LIMIT,   new FloatDoublewordElement(0xF30E).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_COMMAND_TIMEOUT,        new UnsignedDoublewordElement(0xF310).wordOrder(WordOrder.LSWMSW))
            )
        );

        getModbusProtocol().addTasks(
            TaskBuilder.buildFC3ReadRegistersTaskFC16WriteRegistersTask(
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_DYNAMIC_ACTIVE_POWER_LIMIT, new FloatDoublewordElement(0xF322).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_DYNAMIC_REACTIVE_POWER_REF, new FloatDoublewordElement(0xF324).wordOrder(WordOrder.LSWMSW)),
                m(SolarEdgeSe9kPvInverter.ChannelId.EPC_DYNAMIC_COSPHI_REF,         new FloatDoublewordElement(0xF326).wordOrder(WordOrder.LSWMSW))
            )
        );

        logInfo(log, "Initializing SE9K specific registers finished.");
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

    DiffTimeApply<Float> diffTimeApplyEpcDynamicActivePowerLimit = new DiffTimeApply<>(0.5d) {
        @Override
        public void accept(Float value) throws OpenemsNamedException {
            _setEpcDynamicActivePowerLimit(value);
            setEpcDynamicActivePowerLimit(value);
        }
    };

    DiffTimeApply<Float> diffTimeApplyEpcDynamicReactivePowerLimit = new DiffTimeApply<>(0.5d) {
        @Override
        public void accept(Float value) throws OpenemsNamedException {
            _setEpcDynamicReactivePowerLimit(value);
            setEpcDynamicReactivePowerLimit(value);
        }
    };

    DiffTimeApply<Float> diffTimeApplyaEpcDynamicCosPhiRef = new DiffTimeApply<>(0.01d) {
        @Override
        public void accept(Float value) throws OpenemsNamedException {
            _setEpcDynamicCosPhiRef(value);
            setEpcDynamicCosPhiRef(value);
        }
    };

    public void applyEpcDynamicLimits(Optional<Integer> activePowerLimitValue) throws OpenemsNamedException {
        applyEpcDynamicActivePowerLimit(activePowerLimitValue);
        applyEpcDynamicReactivePowerLimit();
        applyEpcDynamicCosPhiRef();
    }

    public void applyEpcDynamicActivePowerLimit(Optional<Integer> activePowerLimitValue) throws OpenemsNamedException {

        // apply override
        Value<Float> overrideActivePowerValue = getOverrideActivePower();
        float overrideActivePower = overrideActivePowerValue.orElse(-1f);
        if (overrideActivePower >= 0) {
            overrideActivePower = TypeUtils.fitWithin(0.0f, 100.0f, overrideActivePower);
            diffTimeApplyEpcDynamicActivePowerLimit.nextValue(getEpcCommandTimeout().asOptional(), overrideActivePower);
            return;
        }

        // calculate limit
        double epcDynamicActivePowerLimit = 100.0d;
        if (activePowerLimitValue.isPresent()) {

            // get hardware max
            Value<Float> epcMaxActivePowerValue = getEpcMaxActivePower();
            if (!epcMaxActivePowerValue.isDefined()) {
                logWarn(log, getEpcMaxActivePowerChannel().channelId() + " has no value.");
            }

            float activePowerLimit = activePowerLimitValue.get();
            float epcMaxActivePower = epcMaxActivePowerValue.orElse(activePowerLimit);
            epcDynamicActivePowerLimit = activePowerLimit * 100 / epcMaxActivePower;
        }
        logDebug("epcDynamicActivePowerLimit " + epcDynamicActivePowerLimit);

        // fit in boundaries
        epcDynamicActivePowerLimit = TypeUtils.fitWithin(1.0d, 100.0d, epcDynamicActivePowerLimit);
        logDebug("epcDynamicActivePowerLimit " + epcDynamicActivePowerLimit);

        // apply mean
        activePowerLimitWeightedMean.addValue(epcDynamicActivePowerLimit, 3, 1);
        epcDynamicActivePowerLimit = activePowerLimitWeightedMean.getMean();
        logDebug("epcDynamicActivePowerLimit " + epcDynamicActivePowerLimit);

        diffTimeApplyEpcDynamicActivePowerLimit.nextValue(getEpcCommandTimeout().asOptional(), (float)epcDynamicActivePowerLimit);
    }

    private void applyEpcDynamicReactivePowerLimit() throws OpenemsNamedException {
        float epcDynamicReactivePowerLimit = TypeUtils.fitWithin(0.0f, 100.0f, reactivePowerRef);
        diffTimeApplyEpcDynamicReactivePowerLimit.nextValue(getEpcCommandTimeout().asOptional(), epcDynamicReactivePowerLimit);
    }

    private void applyEpcDynamicCosPhiRef() throws OpenemsNamedException {
        float epcDynamicCosPhiRef = TypeUtils.fitWithin(-1.0f, 1.0f, cosPhiRef);
        diffTimeApplyaEpcDynamicCosPhiRef.nextValue(getEpcCommandTimeout().asOptional(), epcDynamicCosPhiRef);
    }


    public void logDebug(String message) {
        if (debugMode) {
            logInfo(log, message);
        }
    }


    // Although values can be read from theses fields the SolarEdge Se9k 
    // documentation does not mention them and values seem to be not
    // useful. For this reason they are blacklisted here to avoid false
    // alerts in OpenEMS.

    private static final Set<SunSpecPoint> BLACKLIST = Set.of(
        DefaultSunSpecModel.S101.EVT1,
        DefaultSunSpecModel.S101.EVT2,
        DefaultSunSpecModel.S101.EVT_VND1,
        DefaultSunSpecModel.S101.EVT_VND2,
        DefaultSunSpecModel.S101.EVT_VND3,
        DefaultSunSpecModel.S101.EVT_VND4,

        DefaultSunSpecModel.S102.EVT1,
        DefaultSunSpecModel.S102.EVT2,
        DefaultSunSpecModel.S102.EVT_VND1,
        DefaultSunSpecModel.S102.EVT_VND2,
        DefaultSunSpecModel.S102.EVT_VND3,
        DefaultSunSpecModel.S102.EVT_VND4,

        DefaultSunSpecModel.S103.EVT1,
        DefaultSunSpecModel.S103.EVT2,
        DefaultSunSpecModel.S103.EVT_VND1,
        DefaultSunSpecModel.S103.EVT_VND2,
        DefaultSunSpecModel.S103.EVT_VND3,
        DefaultSunSpecModel.S103.EVT_VND4
    );

    @Override
    protected List<ModbusElement> addModbusElementAndChannels(int startAddress, SunSpecModel model, SunSpecPoint sunSpecPoint) {
        if (BLACKLIST.contains(sunSpecPoint)) {
            int length = sunSpecPoint.get().type.getLength();

            return List.of(new DummyRegisterElement(startAddress, startAddress + length - 1));
        }

        return super.addModbusElementAndChannels(startAddress, model, sunSpecPoint);
    }
}
