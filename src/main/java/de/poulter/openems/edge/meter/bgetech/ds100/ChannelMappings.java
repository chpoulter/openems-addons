/*
 *   OpenEMS Meter B+G E-Tech DS100 bundle
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

package de.poulter.openems.edge.meter.bgetech.ds100;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.meter.api.ElectricityMeter;

enum ChannelMappings {
    
    TOTAL_FORWARD_ACTIVE_ENERGY     (ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,        ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY),
    TOTAL_REVERSE_ACTIVE_ENERGY     (ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,       ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY),
    TOTAL_FORWARD_REACTIVE_ENERGY   (MeterBgeTechDS100.ChannelId.REACTIVE_PRODUCTION_ENERGY,     MeterBgeTechDS100.ChannelId.REACTIVE_CONSUMPTION_ENERGY),
    TOTAL_REVERSE_REACTIVE_ENERGY   (MeterBgeTechDS100.ChannelId.REACTIVE_CONSUMPTION_ENERGY,    MeterBgeTechDS100.ChannelId.REACTIVE_PRODUCTION_ENERGY),
    
    A_PHASE_FORWARD_ACTIVE_ENERGY   (ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1,     ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1),
    A_PHASE_REVERSE_ACTIVE_ENERGY   (ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1,    ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1),
    A_PHASE_FORWARD_REACTIVE_ENERGY (MeterBgeTechDS100.ChannelId.REACTIVE_PRODUCTION_ENERGY_L1,  MeterBgeTechDS100.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L1),
    A_PHASE_REVERSE_REACTIVE_ENERGY (MeterBgeTechDS100.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L1, MeterBgeTechDS100.ChannelId.REACTIVE_PRODUCTION_ENERGY_L1),
    
    B_PHASE_FORWARD_ACTIVE_ENERGY   (ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2,     ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2),
    B_PHASE_REVERSE_ACTIVE_ENERGY   (ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2,    ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2),
    B_PHASE_FORWARD_REACTIVE_ENERGY (MeterBgeTechDS100.ChannelId.REACTIVE_PRODUCTION_ENERGY_L2,  MeterBgeTechDS100.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L2),
    B_PHASE_REVERSE_REACTIVE_ENERGY (MeterBgeTechDS100.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L2, MeterBgeTechDS100.ChannelId.REACTIVE_PRODUCTION_ENERGY_L2),
    
    C_PHASE_FORWARD_ACTIVE_ENERGY   (ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3,     ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3),
    C_PHASE_REVERSE_ACTIVE_ENERGY   (ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3,    ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3),
    C_PHASE_FORWARD_REACTIVE_ENERGY (MeterBgeTechDS100.ChannelId.REACTIVE_PRODUCTION_ENERGY_L3,  MeterBgeTechDS100.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L3),
    C_PHASE_REVERSE_REACTIVE_ENERGY (MeterBgeTechDS100.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L3, MeterBgeTechDS100.ChannelId.REACTIVE_PRODUCTION_ENERGY_L3);

    private ChannelId channelId;
    private ChannelId channelIdInverted;        

    ChannelMappings(ChannelId channelId, ChannelId channelIdInverted) {
        this.channelId = channelId;
        this.channelIdInverted = channelIdInverted;
    }
    
    public ChannelId channelId(boolean inverted) {
        return inverted ? channelIdInverted : channelId;
    }
}
