/*
 *   OpenEMS IO bestep bundle
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

package de.poulter.openems.edge.io.bestep;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC2ReadInputsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(
    name = "IO.Bestep",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@EventTopics({
    EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
    EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE
})
public class IoBestepImpl extends AbstractOpenemsModbusComponent
    implements IoBestep, DigitalOutput, DigitalInput, ModbusComponent, OpenemsComponent, ModbusSlave, EventHandler
{

    private static final Logger log = LoggerFactory.getLogger(IoBestepImpl.class);

    private static final String MODBUS_SETTER_REFERENCE = "Modbus";

    @Reference
    private ConfigurationAdmin cm;

    private final BooleanReadChannel[] digitalInputChannels;
    private final BooleanWriteChannel[] digitalOutputChannels;

    private boolean channelMapper;

    public IoBestepImpl() {
        super(
            OpenemsComponent.ChannelId.values(),
            ModbusComponent.ChannelId.values(),
            DigitalOutput.ChannelId.values(),
            DigitalInput.ChannelId.values(),
            IoBestep.ChannelId.values()
        );

        digitalInputChannels = new BooleanReadChannel[] {
            channel(IoBestep.ChannelId.INPUT_1),
            channel(IoBestep.ChannelId.INPUT_2),
            channel(IoBestep.ChannelId.INPUT_3),
            channel(IoBestep.ChannelId.INPUT_4)
        };

        digitalOutputChannels = new BooleanWriteChannel[] {
            channel(IoBestep.ChannelId.RELAY_1),
            channel(IoBestep.ChannelId.RELAY_2),
            channel(IoBestep.ChannelId.RELAY_3),
            channel(IoBestep.ChannelId.RELAY_4)
        };
    }

    @Override
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsException {
        super.activate(
            context,
            config.id(), config.alias(), config.enabled(), config.modbusUnitId(),
            this.cm,
            MODBUS_SETTER_REFERENCE,
            config.modbus_id()
        );

        this.channelMapper = config.channelMapper();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public String debugLog() {
        BiFunction<BooleanReadChannel[], String[], String> mapper = (values, symbols) -> Arrays.stream(values)
            .map(channel -> channel.value().asOptional())
            .map(value -> value.isPresent() ? (value.get() ? symbols[0] : symbols[1]) : symbols[2])
            .collect(Collectors.joining(""));

        return "Output:"
            + mapper.apply(digitalOutputChannels, new String[]{"X", "-", "?"})
            + "|Input:"
            + mapper.apply(digitalInputChannels, new String[]{"I", "O", "?"});
    }

    @Override
    public void handleEvent(Event event) {
        if (!this.isEnabled()) return;

        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
                processTopicCycleExecuteWrite();
                break;

            case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
                handleCycleBeforeProcessImage();
                break;

        }
    }

    private void processTopicCycleExecuteWrite() {

        // consume the nextwritevalues although we do not need them
        for (BooleanWriteChannel digitalOutputChannel : digitalOutputChannels) {
            digitalOutputChannel.getNextWriteValueAndReset();
        }
    }

    private void handleCycleBeforeProcessImage() {

        if (channelMapper) {
            runChannelMapper();
        }
    }

    private void runChannelMapper() {
        for (int i = 0; i < digitalInputChannels.length; i++) {
            Optional<Boolean> nextWriteValue = digitalOutputChannels[i].getNextWriteValue();
            Value<Boolean> currentValue = digitalOutputChannels[i].value();
            Value<Boolean> inputValue = digitalInputChannels[i].value();

            if (nextWriteValue.isPresent()) continue;
            if (!inputValue.isDefined()) continue;
            if (currentValue.isDefined() && (currentValue.get() == inputValue.get())) continue;

            try {
                digitalOutputChannels[i].setNextWriteValue(inputValue.get());

            } catch (OpenemsNamedException ex) {
                logError(log, "Could not set value on channel " + digitalOutputChannels[i].channelId().name() + ": " + ex.getMessage());
            }
        }
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {

        ModbusProtocol modbusProtocol = new ModbusProtocol(this,
            new FC2ReadInputsTask(0x00, Priority.HIGH,
                m(IoBestep.ChannelId.INPUT_1, new CoilElement(0x00)),
                m(IoBestep.ChannelId.INPUT_2, new CoilElement(0x01)),
                m(IoBestep.ChannelId.INPUT_3, new CoilElement(0x02)),
                m(IoBestep.ChannelId.INPUT_4, new CoilElement(0x03)),
                new CoilElement(0x04),
                new CoilElement(0x05),
                new CoilElement(0x06),
                new CoilElement(0x07)
            ),

            new FC1ReadCoilsTask(0x00, Priority.LOW,
                m(IoBestep.ChannelId.RELAY_1, new CoilElement(0x00)),
                m(IoBestep.ChannelId.RELAY_2, new CoilElement(0x01)),
                m(IoBestep.ChannelId.RELAY_3, new CoilElement(0x02)),
                m(IoBestep.ChannelId.RELAY_4, new CoilElement(0x03)),
                new CoilElement(0x04),
                new CoilElement(0x05),
                new CoilElement(0x06),
                new CoilElement(0x07)
            ),

            new FC5WriteCoilTask(0x00, m(IoBestep.ChannelId.RELAY_1, new CoilElement(0x00))),
            new FC5WriteCoilTask(0x01, m(IoBestep.ChannelId.RELAY_2, new CoilElement(0x01))),
            new FC5WriteCoilTask(0x02, m(IoBestep.ChannelId.RELAY_3, new CoilElement(0x02))),
            new FC5WriteCoilTask(0x03, m(IoBestep.ChannelId.RELAY_4, new CoilElement(0x03)))

        );

        return modbusProtocol;
    }

    @Override
    public BooleanWriteChannel[] digitalOutputChannels() {
        return digitalOutputChannels;
    }

    @Override
    public BooleanReadChannel[] digitalInputChannels() {
        return digitalInputChannels;
    }

    @Override
    public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {

        return new ModbusSlaveTable(
            OpenemsComponent.getModbusSlaveNatureTable(accessMode),
            ModbusSlaveNatureTable.of(IoBestep.class, accessMode, 100)
                .channel(0, IoBestep.ChannelId.RELAY_1, ModbusType.UINT16)
                .channel(1, IoBestep.ChannelId.RELAY_2, ModbusType.UINT16)
                .channel(2, IoBestep.ChannelId.RELAY_3, ModbusType.UINT16)
                .channel(3, IoBestep.ChannelId.RELAY_4, ModbusType.UINT16)
                .channel(4, IoBestep.ChannelId.INPUT_1, ModbusType.UINT16)
                .channel(5, IoBestep.ChannelId.INPUT_2, ModbusType.UINT16)
                .channel(6, IoBestep.ChannelId.INPUT_3, ModbusType.UINT16)
                .channel(7, IoBestep.ChannelId.INPUT_4, ModbusType.UINT16)
                .build()
        );
    }
}
