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

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.pvinverter.SunSpecPvInverter;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public interface SolarEdgeSe9kPvInverter extends SunSpecPvInverter, ManagedSymmetricPvInverter, ElectricityMeter,
    ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        PC_RRCR_STATE                  (Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY) .unit(Unit.NONE)                .persistencePriority(PersistencePriority.LOW)),
        PC_ACTIVE_POWER_LIMIT          (Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY) .unit(Unit.PERCENT)             .persistencePriority(PersistencePriority.LOW)),
        PC_COS_PHI                     (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_ONLY) .unit(Unit.NONE)                .persistencePriority(PersistencePriority.LOW)),
        PC_PWR_FRQ_DERATING_CONFIG     (Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY) .unit(Unit.NONE)                .persistencePriority(PersistencePriority.LOW)),
        PC_REACTIVE_PWR_CONFIG         (Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY) .unit(Unit.NONE)                .persistencePriority(PersistencePriority.LOW)),
        PC_REACT_PW_ITER_TIME          (Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY) .unit(Unit.SECONDS)             .persistencePriority(PersistencePriority.LOW)),
        PC_ADVANCED_PWR_CONTROL_EN     (Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY) .unit(Unit.NONE)                .persistencePriority(PersistencePriority.LOW)),
        PC_FRT_EN                      (Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY) .unit(Unit.NONE)                .persistencePriority(PersistencePriority.LOW)),

        EPC_ENABLE_DPC                 (Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY) .unit(Unit.WATT)                .persistencePriority(PersistencePriority.LOW)),
        EPC_MAX_ACTIVE_POWER           (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_ONLY) .unit(Unit.WATT)                .persistencePriority(PersistencePriority.LOW)),
        EPC_MAX_REACTIVE_POWER         (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_ONLY) .unit(Unit.VOLT_AMPERE_REACTIVE).persistencePriority(PersistencePriority.LOW)),
        EPC_ACTIVE_REACTIVE_PREF       (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_ONLY) .unit(Unit.NONE)                .persistencePriority(PersistencePriority.LOW)),
        EPC_COSPHI_Q_PREF              (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_ONLY) .unit(Unit.NONE)                .persistencePriority(PersistencePriority.LOW)),
        EPC_ACTIVE_POWER_LIMIT         (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_ONLY) .unit(Unit.WATT)                .persistencePriority(PersistencePriority.LOW)),
        EPC_REACTIVE_POWER_LIMIT       (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_ONLY) .unit(Unit.VOLT_AMPERE_REACTIVE).persistencePriority(PersistencePriority.LOW)),
        EPC_COMMAND_TIMEOUT            (Doc.of(OpenemsType.LONG)   .accessMode(AccessMode.READ_ONLY) .unit(Unit.SECONDS)             .persistencePriority(PersistencePriority.LOW)),
        EPC_DYNAMIC_ACTIVE_POWER_LIMIT (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)             .persistencePriority(PersistencePriority.HIGH)),
        EPC_DYNAMIC_REACTIVE_POWER_REF (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)             .persistencePriority(PersistencePriority.LOW)),
        EPC_DYNAMIC_COSPHI_REF         (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_WRITE).unit(Unit.NONE)                .persistencePriority(PersistencePriority.LOW)),

        OVERRIDE_ACTIVE_POWER_LIMIT    (Doc.of(OpenemsType.FLOAT)  .accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)             .persistencePriority(PersistencePriority.HIGH)),
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    // for SunSpecPvInverter
    public default StateChannel getPvLimitFailedChannel() {
        return channel(SunSpecPvInverter.ChannelId.PV_LIMIT_FAILED);
    }

    public default Value<Boolean> getPvLimitFailed() {
        return getPvLimitFailedChannel().value();
    }

    public default void _setPvLimitFailed(Boolean value) {
        getPvLimitFailedChannel().setNextValue(value);
    }


    public default StateChannel getReadOnlyModePvLimitFailedChannel() {
        return channel(SunSpecPvInverter.ChannelId.READ_ONLY_MODE_PV_LIMIT_FAILED);
    }

    public default Value<Boolean> getReadOnlyModePvLimitFailed() {
        return getReadOnlyModePvLimitFailedChannel().value();
    }

    public default void _setReadOnlyModePvLimitFailed(Boolean value) {
        getReadOnlyModePvLimitFailedChannel().setNextValue(value);
    }

    // own
    public default IntegerReadChannel getPcRrcrStateChannel() {
        return channel(ChannelId.PC_RRCR_STATE);
    }

    public default Value<Integer> getPcRrcrState() {
        return getPcRrcrStateChannel().value();
    }

    public default FloatWriteChannel getEpcDynamicActivePowerLimitChannel() {
        return channel(ChannelId.EPC_DYNAMIC_ACTIVE_POWER_LIMIT);
    }

    public default Value<Float> getEpcDynamicActivePowerLimit() {
        return getEpcDynamicActivePowerLimitChannel().value();
    }

    public default void _setEpcDynamicActivePowerLimit(Float value) {
        getEpcDynamicActivePowerLimitChannel().setNextValue(value);
    }

    public default void setEpcDynamicActivePowerLimit(Float value) throws OpenemsNamedException {
        getEpcDynamicActivePowerLimitChannel().setNextWriteValue(value);
    }

    public default FloatWriteChannel getEpcDynamicReactivePowerLimitChannel() {
        return channel(ChannelId.EPC_DYNAMIC_REACTIVE_POWER_REF);
    }

    public default Value<Float> getEpcDynamicReactivePowerLimit() {
        return getEpcDynamicReactivePowerLimitChannel().value();
    }

    public default void _setEpcDynamicReactivePowerLimit(Float value) {
        getEpcDynamicReactivePowerLimitChannel().setNextValue(value);
    }

    public default void setEpcDynamicReactivePowerLimit(Float value) throws OpenemsNamedException {
        getEpcDynamicReactivePowerLimitChannel().setNextWriteValue(value);
    }

    public default FloatWriteChannel getEpcDynamicCosPhiRefChannel() {
        return channel(ChannelId.EPC_DYNAMIC_COSPHI_REF);
    }

    public default void _setEpcDynamicCosPhiRef(Float value) {
        getEpcDynamicCosPhiRefChannel().setNextValue(value);
    }

    public default void setEpcDynamicCosPhiRef(Float value) throws OpenemsNamedException {
        getEpcDynamicCosPhiRefChannel().setNextWriteValue(value);
    }

    public default FloatReadChannel getEpcMaxActivePowerChannel() {
        return channel(ChannelId.EPC_MAX_ACTIVE_POWER);
    }

    public default Value<Float> getEpcMaxActivePower() {
        return getEpcMaxActivePowerChannel().value();
    }

    public default FloatReadChannel getEpcMaxReactivePowerChannel() {
        return channel(ChannelId.EPC_MAX_REACTIVE_POWER);
    }

    public default Value<Float> getEpcMaxReactivePower() {
        return getEpcMaxReactivePowerChannel().value();
    }

    public default FloatWriteChannel getOverrideActivePowerChannel() {
        return channel(ChannelId.OVERRIDE_ACTIVE_POWER_LIMIT);
    }

    public default Value<Float> getOverrideActivePower() {
        return getOverrideActivePowerChannel().value();
    }

    public default void _setOverrideActivePower(Float value) {
        getOverrideActivePowerChannel().setNextValue(value);
    }

    public default void setOverrideActivePower(Float value) throws OpenemsNamedException {
        getOverrideActivePowerChannel().setNextWriteValue(value);
    }

    public default LongReadChannel getEpcCommandTimeoutChannel() {
        return channel(ChannelId.EPC_COMMAND_TIMEOUT);
    }

    public default Value<Long> getEpcCommandTimeout() {
        return getEpcCommandTimeoutChannel().value();
    }

}
