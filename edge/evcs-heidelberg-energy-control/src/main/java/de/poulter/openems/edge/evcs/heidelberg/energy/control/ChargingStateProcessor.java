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

import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.evcs.api.Status;

public class ChargingStateProcessor implements Consumer<Value<Integer>> {

    private static final Logger log = LoggerFactory.getLogger(CalculateUsedPhasesFromVoltage.class);

    private final EvcsHeidelbergEnergyControl evcs;

    private ChargingStateProcessor(EvcsHeidelbergEnergyControl evcs) {
        this.evcs = evcs;
    }

    @Override
    public void accept(Value<Integer> value) {
        ChargingState chargingState = OptionsEnum.getOption(ChargingState.class, value.get());
        Status currentState = evcs.getStatus();
        Status nextState = (chargingState == null) ? Status.UNDEFINED : chargingState.getStatus();

        if (Status.ERROR.equals(currentState) || Status.UNDEFINED.equals(currentState)) {
            if (!Objects.equals(currentState, nextState)) {
                if (log.isDebugEnabled()) {
                    log.debug("State change detected: {} -> {}", currentState, nextState);
                }
                evcs.applyConfigurationNow();
            }
        }

        evcs._setPlug(chargingState.getPlug());
        evcs._setStatus(nextState);
    }


    public static ChargingStateProcessor add(EvcsHeidelbergEnergyControl evcs) {
        ChargingStateProcessor chargingStateProcessor = new ChargingStateProcessor(evcs);
        evcs.getChargingStateChannel().onUpdate(chargingStateProcessor);

        return chargingStateProcessor;
    }
}
