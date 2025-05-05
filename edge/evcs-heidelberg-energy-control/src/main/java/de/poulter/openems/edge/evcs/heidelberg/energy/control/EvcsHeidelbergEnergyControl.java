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

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsHeidelbergEnergyControl extends ManagedEvcs, Evcs, ElectricityMeter, OpenemsComponent {

    public void applyConfigurationNow();

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        ERROR(Doc.of(Level.FAULT).persistencePriority(PersistencePriority.HIGH)),
        CHARGING_STATE(Doc.of(ChargingState.values()).persistencePriority(PersistencePriority.HIGH).accessMode(AccessMode.READ_ONLY)),
        PCB_TEMPERATURE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
        EXTERN_LOCK_STATE(Doc.of(LockState.values()).accessMode(AccessMode.READ_ONLY)),

        WATCHDOG_TIMEOUT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_WRITE)),
        STANDBY(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        MAX_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).persistencePriority(PersistencePriority.HIGH).accessMode(AccessMode.READ_WRITE)),
        FAILSAFE_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_WRITE)),

        CHARGE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        PLUG(Doc.of(Plug.values()).accessMode(AccessMode.READ_ONLY)),

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

    // ERROR
    public default StateChannel getErrorChannel() {
        return this.channel(ChannelId.ERROR);
    }

    public default void _setError(boolean value) {
        this.getErrorChannel().setNextValue(value);
    }

    // CHARGING_STATE
    public default EnumReadChannel getChargingStateChannel() {
        return this.channel(ChannelId.CHARGING_STATE);
    }

    // WATCHDOG_TIMEOUT
    public default WriteChannel<Integer> getWatchdogTimeoutChannel() {
        return this.channel(ChannelId.WATCHDOG_TIMEOUT);
    }

    public default void setWatchdogTimeout(int value) throws OpenemsError.OpenemsNamedException {
        this.getWatchdogTimeoutChannel().setNextWriteValue(value);
    }

    // STANDBY
    public default WriteChannel<Integer> getStandbyChannel() {
        return this.channel(ChannelId.STANDBY);
    }

    public default void setStandby(int value) throws OpenemsError.OpenemsNamedException {
        this.getStandbyChannel().setNextWriteValue(value);
    }

    // MAX_CURRENT
    public default WriteChannel<Integer> getMaxCurrentChannel() {
        return this.channel(ChannelId.MAX_CURRENT);
    }

    public default void setMaxCurrent(int value) throws OpenemsError.OpenemsNamedException {
        this.getMaxCurrentChannel().setNextWriteValue(value);
    }

    // FAILSAFE_CURRENT
    public default WriteChannel<Integer> getFailsafeCurrentChannel() {
        return this.channel(ChannelId.FAILSAFE_CURRENT);
    }

    public default void setFailsafeCurrent(int value) throws OpenemsError.OpenemsNamedException {
        this.getFailsafeCurrentChannel().setNextWriteValue(value);
    }

    // CHARGE_POWER
    public default IntegerReadChannel getChargePowerChannel() {
        return this.channel(ChannelId.CHARGE_POWER);
    }

    public default void _setChargePower(Integer value) {
        this.getChargePowerChannel().setNextValue(value);
    }

    // PLUG
    public default Channel<Plug> getPlugChannel() {
        return this.channel(ChannelId.PLUG);
    }

    public default Plug getPlug() {
        return this.getPlugChannel().value().asEnum();
    }

    public default void _setPlug(Plug value) {
        this.getPlugChannel().setNextValue(value);
    }

}
