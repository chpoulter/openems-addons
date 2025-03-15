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

import io.openems.edge.common.test.AbstractComponentTest.TestCase;

import org.junit.jupiter.api.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterBgeTechDS100Test {

    @Test
    public void test() throws Exception {
    	new ComponentTest(new MeterBgeTechDS100Impl())
            .addReference("cm", new DummyConfigurationAdmin())
            .addReference("setModbus", new DummyModbusBridge("modbus0"))
            .activate(MeterBgeTechDS100TestConfig.create()
                .setId("component0")
                .setModbusId("modbus0")
                .setType(MeterType.GRID)
                .setInvert(false)
                .build())
            .next(new TestCase())
            .deactivate();
    }

}
