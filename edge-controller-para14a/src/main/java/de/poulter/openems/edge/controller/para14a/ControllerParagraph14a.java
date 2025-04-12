/*
 *   OpenEMS Paragraph 14a Controller
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

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerParagraph14a extends Controller, OpenemsComponent{

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        PRODUCTION_MANAGEMENT(Doc.of(ProductionManagment.values())
            .text("Current production managment mode.")),
        CONSUMPTION_MANAGEMENT(Doc.of(ConsumptionManagment.values())
            .text("Current consumption managment mode.")),

        PVINVERTER_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)
            .unit(Unit.WATT)
            .text("Active power of the pv inverter")),
        PVINVERTER_MAX_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)
            .unit(Unit.WATT)
            .text("Maximum active power of pv inverter")),
        PVINVERTER_ACTIVE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER)
            .unit(Unit.WATT)
            .text("Limit for active power of pv inverter")),

        EVCS_COUNT(Doc.of(OpenemsType.INTEGER)
            .unit(Unit.NONE)
            .text("Amount of managed EVCS.")),
        EVCS_CLUSTER_MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE(Doc.of(OpenemsType.INTEGER)
            .unit(Unit.WATT)
            .text("Allowed power to distribute by EVCS clusters.")),

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


    public default IntegerReadChannel getPvInverterActivePowerChannel() {
        return this.channel(ChannelId.PVINVERTER_ACTIVE_POWER);
    }

    public default void _setPvInverterActivePower(Integer value) {
        this.getPvInverterActivePowerChannel().setNextValue(value);
    }


    public default IntegerReadChannel getPvInverterMaxActivePowerChannel() {
        return this.channel(ChannelId.PVINVERTER_MAX_ACTIVE_POWER);
    }

    public default void _setPvInverterMaxActivePower(Integer value) {
        this.getPvInverterMaxActivePowerChannel().setNextValue(value);
    }


    public default IntegerReadChannel getPvInverterActivePowerLimitChannel() {
        return this.channel(ChannelId.PVINVERTER_ACTIVE_POWER_LIMIT);
    }

    public default void _setPvInverterActivePowerLimit(Integer value) {
        this.getPvInverterActivePowerLimitChannel().setNextValue(value);
    }


    public default IntegerReadChannel getEvcsCountChannel() {
        return this.channel(ChannelId.EVCS_COUNT);
    }

    public default void _setEvcsCount(Integer value) {
        this.getEvcsCountChannel().setNextValue(value);
    }


    public default IntegerReadChannel getEvcsClusterMaximumAllowedPowerToDistributeChannel() {
        return this.channel(ChannelId.EVCS_CLUSTER_MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE);
    }

    public default void _setEvcsClusterMaximumAllowedPowerToDistribute(Integer value) {
        this.getEvcsClusterMaximumAllowedPowerToDistributeChannel().setNextValue(value);
    }

}
