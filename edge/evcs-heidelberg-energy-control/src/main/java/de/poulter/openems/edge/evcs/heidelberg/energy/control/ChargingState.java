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
import io.openems.edge.evcs.api.Status;

public enum ChargingState implements OptionsEnum {

    A1( 2, Plug.UNPLUGGED, Status.NOT_READY_FOR_CHARGING, "No vehicle plugged, Charging disabled"),
    B1( 4, Plug.PLUGGED,   Status.NOT_READY_FOR_CHARGING, "Vehicle plugged without charging request, Charging disabled"),
    C1( 6, Plug.PLUGGED,   Status.NOT_READY_FOR_CHARGING, "Vehicle plugged with charging request, Charging disabled"),
    A2( 3, Plug.UNPLUGGED, Status.READY_FOR_CHARGING,     "No vehicle plugged, Charging enabled"),
    B2( 5, Plug.PLUGGED,   Status.READY_FOR_CHARGING,     "Vehicle plugged without charging request, Charging enabled"),
    C2( 7, Plug.PLUGGED,   Status.CHARGING,               "Vehicle plugged with charging request, Charging enabled"),
    DE( 8, Plug.UNDEFINED, Status.ERROR,                  "derating"),
    E ( 9, Plug.UNDEFINED, Status.ERROR,                  "E"),
    F (10, Plug.UNDEFINED, Status.ERROR,                  "F"),
    ER(11, Plug.UNDEFINED, Status.ERROR,                  "ERR"),

    ;

    private int value;
    private Plug plug;
    private Status status;
    private String name;

    private ChargingState(int value, Plug plug, Status status, String name) {
        this.value = value;
        this.plug = plug;
        this.status = status;
        this.name = name;
    }

    @Override
    public int getValue() {
        return value;
    }

    public Plug getPlug() {
        return plug;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public OptionsEnum getUndefined() {
        return ER;
    }

}
