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

import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.Phases;

public class ElementToChannelConverters {

    public static final ElementToChannelConverter AMPERE_TO_WATT = new ElementToChannelConverter( current -> {
        Integer currentAsInteger = TypeUtils.getAsType(OpenemsType.INTEGER, current);
        if (currentAsInteger == null) return null;

        return EvcsUtils.ampereToWatt(currentAsInteger, Phases.THREE_PHASE.getValue());
    });

    public static final ElementToChannelConverter CHARGING_STATE = new ElementToChannelConverter( stateCode -> {
        Integer stateCodeAsInteger = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, stateCode);
        if (stateCodeAsInteger == null) return null;

        return OptionsEnum.getOption(ChargingState.class, stateCodeAsInteger);
    });

    public static final ElementToChannelConverter LOCK_STATE = new ElementToChannelConverter( stateCode -> {
        Integer stateCodeAsInteger = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, stateCode);
        if (stateCodeAsInteger == null) return null;

        return OptionsEnum.getOption(LockState.class, stateCodeAsInteger);
    });
}
