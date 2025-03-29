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

import io.openems.common.types.OptionsEnum;

public enum ConsumptionManagment implements OptionsEnum {

    FULL   (3, "no limit"),  // E2=0, E1=0
    REDUCED(2, "reduced"),   // E2=0, E1=1
    UNUSED (1, "unused"),    // E2=1, E1=0
    OFF    (0, "off")        // E2=1, E1=1
    ;

    private final int value;
    private final String name;

    ConsumptionManagment(int value, String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public OptionsEnum getUndefined() {
        return OFF;
    }

    public static ConsumptionManagment getForInput(Boolean e1) {
        if (e1 == null) return ConsumptionManagment.OFF;

        return e1 ? ConsumptionManagment.FULL : ConsumptionManagment.OFF;
    }

    public static ConsumptionManagment getForInputs(Boolean e1, Boolean e2) {
        if (e1 == null || e2 == null) return ConsumptionManagment.OFF;

        if (!e1 && !e2) return ConsumptionManagment.FULL;
        if (!e1 && e2) return ConsumptionManagment.REDUCED;
        if (e1 && !e2) return ConsumptionManagment.UNUSED;
        if (e1 && e2) return ConsumptionManagment.OFF;

        throw new IllegalArgumentException("This should not happen.");
    }
}
