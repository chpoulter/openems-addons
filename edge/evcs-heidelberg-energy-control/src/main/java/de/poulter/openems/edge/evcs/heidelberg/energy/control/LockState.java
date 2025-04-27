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

import io.openems.common.types.OptionsEnum;

public enum LockState implements OptionsEnum {

    LOCKED(0, "Locked"),
    UNLOCKED(1, "Unlocked"),
    UNKNOWN(-1, "Unknown"),

    ;

    private int value;
    private String name;

    private LockState(int value, String name) {
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
        return UNKNOWN;
    }
}
