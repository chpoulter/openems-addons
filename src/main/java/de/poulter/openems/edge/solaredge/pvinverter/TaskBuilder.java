/*
 *   OpenEMS PvInverter SolarEdge Se9k bundle
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

package de.poulter.openems.edge.solaredge.pvinverter;

import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.taskmanager.Priority;

public class TaskBuilder {

    private TaskBuilder() {}

    public static Task buildFC3ReadRegistersTask(ModbusElement... elements) {
        return buildFC3ReadRegistersTask(Priority.LOW, elements);
    }

    public static Task buildFC3ReadRegistersTask(Priority priority, ModbusElement... elements) {
        if (elements.length < 1) {
            throw new IllegalArgumentException("No modbus elements defined.");
        }

        int startAddress = elements[0].startAddress;

        return new FC3ReadRegistersTask(startAddress, priority, elements);
    }

    public static Task[] buildFC3ReadRegistersTaskFC16WriteRegistersTask(ModbusElement... elements) {
        return buildFC3ReadRegistersTaskAndFC16WriteRegistersTask(Priority.LOW, elements);
    }

    public static Task[] buildFC3ReadRegistersTaskAndFC16WriteRegistersTask(Priority priority, ModbusElement... elements) {
        if (elements.length < 1) {
            throw new IllegalArgumentException("No modbus elements defined.");
        }

        int startAddress = elements[0].startAddress;

        return new Task[] {
            new FC3ReadRegistersTask(startAddress, priority, elements),
            new FC16WriteRegistersTask(startAddress, elements)
        };
    }

}