/*
 *   OpenEMS Meter Paragraph 14a Controller
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

package de.poulter.openems.edge.controller.para14a;

import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.channel.Unit.NONE;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerParagraph14a extends Controller, OpenemsComponent{

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        PRODUCTION_MANAGEMENT(Doc.of(ProductionManagment.values())
            .text("Current production managment mode.")),

        CONSUMPTION_MANAGEMENT(Doc.of(ConsumptionManagment.values())
            .text("Current consumption managment mode.")),

        PVINVERTER_SUM_ACTIVE_POWER(Doc.of(INTEGER)
            .unit(WATT)
            .text("Sum of active power of all pv inverters.")),

        PVINVERTERS_HARDWARE_MAX_ACTIVE_POWER(Doc.of(INTEGER)
            .unit(WATT)
            .text("Maximum hardware active power of all pv inverters.")),

        PVINVERTERS_ACTIVE_POWER_LIMIT_SUM(Doc.of(INTEGER)
            .unit(WATT)
            .text("Allowed maximum active power for all pv inverters.")),

        EVCS_COUNT(Doc.of(INTEGER)
            .unit(NONE)
            .text("Amount of managed EVCS.")),

        EVCS_CLUSTERS_ALLOWED_POWER(Doc.of(INTEGER)
            .unit(WATT)
            .text("Allowed power to distribute by EVCS clusters."))
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

    public default IntegerReadChannel getEvcsCountChannel() {
        return this.channel(ChannelId.EVCS_COUNT);
    }

    public default Value<Integer> getEvcsCount() {
        return this.getEvcsCountChannel().value();
    }

    public default void _setEvcsCount(Integer value) {
        this.getEvcsCountChannel().setNextValue(value);
    }


    public default IntegerReadChannel getEvcsClustersAllowedPowerChannel() {
        return this.channel(ChannelId.EVCS_CLUSTERS_ALLOWED_POWER);
    }

    public default Value<Integer> getEvcsClustersAllowedPower() {
        return this.getEvcsClustersAllowedPowerChannel().value();
    }

    public default void _setEvcsClustersAllowedPower(Integer value) {
        this.getEvcsClustersAllowedPowerChannel().setNextValue(value);
    }


    public default Channel<ProductionManagment> getProductionManagmentChannel() {
        return this.channel(ChannelId.PRODUCTION_MANAGEMENT);
    }

    public default ProductionManagment getProductionManagment() {
        return this.getProductionManagmentChannel().value().asEnum();
    }

    public default void _setProductionManagment(ProductionManagment value) {
        this.getProductionManagmentChannel().setNextValue(value);
    }


    public default Channel<ConsumptionManagment> getConsumptionManagmentChannel() {
        return this.channel(ChannelId.CONSUMPTION_MANAGEMENT);
    }

    public default ConsumptionManagment getConsumptionManagment() {
        return this.getConsumptionManagmentChannel().value().asEnum();
    }

    public default void _setConsumptionManagmentChannel(ConsumptionManagment value) {
        this.getConsumptionManagmentChannel().setNextValue(value);
    }


    public default IntegerReadChannel getPvInvertersHardwareMaxActivePowerChannel() {
        return this.channel(ChannelId.PVINVERTERS_HARDWARE_MAX_ACTIVE_POWER);
    }

    public default Value<Integer> getPvInvertersHardwareMaxActivePower() {
        return this.getPvInvertersHardwareMaxActivePowerChannel().value();
    }

    public default void _setPvInvertersHardwareMaxActivePower(Integer value) {
        this.getPvInvertersHardwareMaxActivePowerChannel().setNextValue(value);
    }


    public default IntegerReadChannel getPvInvertersActivePowerLimitSumChannel() {
        return this.channel(ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM);
    }

    public default Value<Integer> getPvInvertersActivePowerLimitSum() {
        return this.getPvInvertersActivePowerLimitSumChannel().value();
    }
 
    public default void _setPvInvertersActivePowerLimitSum(Integer value) {
        this.getPvInvertersActivePowerLimitSumChannel().setNextValue(value);
    }


    public default IntegerReadChannel getPvInverterSumActivePowerChannel() {
        return this.channel(ChannelId.PVINVERTER_SUM_ACTIVE_POWER);
    }

    public default Value<Integer> getPvInverterSumActivePower() {
        return this.getPvInverterSumActivePowerChannel().value();
    }

    public default void _setPvInverterSumActivePower(Integer value) {
        this.getPvInverterSumActivePowerChannel().setNextValue(value);
    }
}
