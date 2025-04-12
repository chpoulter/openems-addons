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

import io.openems.common.types.OptionsEnum;

public enum ProductionManagment implements OptionsEnum {

    FULL     (3, "No limit",    100, false),    // E2=0, E1=0
    REDUCED60(2, "Reduced 60%",  60, true),     // E2=0, E1=1
    REDUCED30(1, "Reduced 30%",  30, true),     // E2=1, E1=0
    OFF      (0, "off",           0, true)      // E2=1, E1=1
    ;

    private final int value;
    private final String name;
    private int factor;
    private boolean applyThreshold;

    ProductionManagment(int value, String name, int factor, boolean applyThreshold) {
        this.value = value;
        this.name = name;
        this.factor = factor;
        this.applyThreshold = applyThreshold;
    }

    public int getFactor() {
        return factor;
    }

    public boolean isApplyThreshold() {
        return applyThreshold;
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

    public static ProductionManagment getForInputs(Boolean e1, Boolean e2) {
        if (e1 == null || e2 == null) return ProductionManagment.OFF;

        if (e1) {
            return e2 ? ProductionManagment.OFF : ProductionManagment.REDUCED30;
        } else {
            return e2 ? ProductionManagment.REDUCED60 : ProductionManagment.FULL;
        }
    }

    public static  ProductionManagment getForInputs(Boolean e1, Boolean e2, Boolean e3) {
        if (e1 == null || e2 == null || e3 == null) return ProductionManagment.OFF;

        if (e1) return ProductionManagment.OFF;
        if (e2) return ProductionManagment.REDUCED30;
        if (e3) return ProductionManagment.REDUCED60;

        return ProductionManagment.FULL;
    }

    public static  ProductionManagment getForInputs(Boolean e1, Boolean e2, Boolean e3, Boolean e4) {
        if (e1 == null || e2 == null || e3 == null || e4 == null) return ProductionManagment.OFF;

        if (e1) return ProductionManagment.OFF;
        if (e2) return ProductionManagment.REDUCED30;
        if (e3) return ProductionManagment.REDUCED60;
        if (e4) return ProductionManagment.FULL;

        return ProductionManagment.OFF;
    }

}
