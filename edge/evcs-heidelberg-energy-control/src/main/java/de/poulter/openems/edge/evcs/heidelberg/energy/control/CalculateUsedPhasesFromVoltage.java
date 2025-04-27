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

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.evcs.api.Evcs;

public class CalculateUsedPhasesFromVoltage implements Consumer<Value<Integer>> {

    private static final Logger log = LoggerFactory.getLogger(CalculateUsedPhasesFromVoltage.class);

    private static final int MIN_VOLTAGE_DIFF = Evcs.DEFAULT_VOLTAGE - 180;

    private final Evcs evcs;

    private CalculateUsedPhasesFromVoltage(Evcs evcs) {
        this.evcs = evcs;
    }

    @Override
    public void accept(Value<Integer> ignore) {
        int voltageL1 = evcs.getVoltageL1Channel().channelDoc().getUnit().getAsBaseUnit(evcs.getVoltageL1().orElse(0));
        int voltageL2 = evcs.getVoltageL2Channel().channelDoc().getUnit().getAsBaseUnit(evcs.getVoltageL2().orElse(0));
        int voltageL3 = evcs.getVoltageL3Channel().channelDoc().getUnit().getAsBaseUnit(evcs.getVoltageL3().orElse(0));

        int phases = 0;
        if (Math.abs(Evcs.DEFAULT_VOLTAGE - voltageL1) < MIN_VOLTAGE_DIFF) phases++;
        if (Math.abs(Evcs.DEFAULT_VOLTAGE - voltageL2) < MIN_VOLTAGE_DIFF) phases++;
        if (Math.abs(Evcs.DEFAULT_VOLTAGE - voltageL3) < MIN_VOLTAGE_DIFF) phases++;

        if (log.isDebugEnabled()) {
            log.debug("calculateUsedPhasesFromVoltage phases in use: " + phases);
        }

        evcs._setPhases(phases);
    }

    public static CalculateUsedPhasesFromVoltage add(Evcs evcs) {
        CalculateUsedPhasesFromVoltage calculateUsedPhasesFromVoltage = new CalculateUsedPhasesFromVoltage(evcs);

        evcs.getVoltageL1Channel().onSetNextValue(calculateUsedPhasesFromVoltage);
        evcs.getVoltageL2Channel().onSetNextValue(calculateUsedPhasesFromVoltage);
        evcs.getVoltageL3Channel().onSetNextValue(calculateUsedPhasesFromVoltage);

        return calculateUsedPhasesFromVoltage;
    }
}
