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

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.test.DummyManagedSymmetricPvInverter;

public class ControllerParagraph14aTest {

    private static final int RAMP_CYCLES = 30;

    @Test
    public void paragraph14a_pvInverterTest() throws Exception {

        ControllerParagraph14aImpl sut = new ControllerParagraph14aImpl();
        new ControllerTest(sut)
                .addReference("cm", new DummyConfigurationAdmin())
                .addReference("pvInverter", new DummyManagedSymmetricPvInverter("pvInverter0"))
                .addReference("gridMeter", new DummyElectricityMeter("meter0"))
                .addReference("componentManager", new DummyComponentManager())
                .addComponent(new DummyInputOutput("io0"))
                .activate(MyConfig.create()
                        .setId("ctrlParagraph14a0")
                        .setMode(RelaisMode.FNN2bit1StbV1StbE)
                        .setInputRelaisId1("io0/InputOutput0")
                        .setInputRelaisId2("io0/InputOutput1")
                        .setInputRelaisId3("io0/InputOutput2")
                        .setInputRelaisId4("io0/InputOutput3")
                        .setGridMeterId("meter0")
                        .setPvInverterId("pvInverter0")
                        .setEvcsClusterId("evcsCluster0")
                        .build()
                ).next(new TestCase() // c 1000, p 4000, g -3000
                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false)
                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false)
                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1000)
                        .input("pvInverter0", ManagedSymmetricPvInverter.ChannelId.MAX_ACTIVE_POWER, 20000)
                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTER_ACTIVE_POWER, 1000)
                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTER_MAX_ACTIVE_POWER, 20000)
                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTER_ACTIVE_POWER_LIMIT, null)
                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, null)

                // no limit
                ).next(new TestCase()  // c 1000, p 4000, g -3000
                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTER_MAX_ACTIVE_POWER, 20000)
                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTER_ACTIVE_POWER_LIMIT, null)
                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, null)

                // limit 60%
                ).next(new TestCase() // c 1000, p 4000, g -3000
                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false)
                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true)
                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTER_ACTIVE_POWER_LIMIT, 13000)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTER_ACTIVE_POWER_LIMIT, 14750)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13875)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13438)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13219)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13110)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13055)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13028)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13014)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13007)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13004)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13002)
//                ).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13001)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTERS_ACTIVE_POWER_LIMIT_SUM, 13001)
//
                ).next(new TestCase() // c 7500, p 4000, g 3500
                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 7500)
                        .input("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, null)
                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 14455)
                        .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTER_ACTIVE_POWER_LIMIT, 14455)

                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
                    .input("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, null)
                    .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19500)
                    .output("ctrlParagraph14a0", ControllerParagraph14a.ChannelId.PVINVERTER_ACTIVE_POWER_LIMIT, 19500)

//                ).next(new TestCase() // c 8500, p 4000, g 4500
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 8500)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19600)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 20000)
//                ).next(new TestCase() // c 9000, p 15000, g -6000
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 9000)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 20000)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 20000)
//                ).next(new TestCase() // c 9000, p 20000, g -11000
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 20000)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 20000)
//                ).next(new TestCase() // c 7000, p 20000, g -13000
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 7000)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19500)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19001)
//                ).next(new TestCase() // c 7000, p 20000, g -13000
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19001)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19001)
//                ).next(new TestCase() // c 7000, p 19000, g -12000
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19001)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19001)
//                ).next(new TestCase() // c 1000, p 19000, g -18000
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1000)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 16001)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13001)
//                ).next(new TestCase() // c 1000, p 13000, g -12000
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13001)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13001)
//                ).next(new TestCase() // c 950, p 13000, g -12000
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 950)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 12976)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 12951)
//                ).next(new TestCase() // c 1100, p 13000, g -12000
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1100)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 12980)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13100)
//
//// TODO: needs adaption to ramping
////                // limit 30%
////                ).next(new TestCase() // c 1000, p 4000, g -3000
////                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true)
////                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false)
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 1000)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 7000)
////                ).next(new TestCase() // c 7500, p 4000, g 3500
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 7500)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13500)
////                ).next(new TestCase() // c 8500, p 4000, g 4500
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 8500)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 14500)
////                ).next(new TestCase() // c 14500, p 15000, g -1000
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 14500)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 20000)
////                ).next(new TestCase() // c 14500, p 20000, g -5500
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 20000)
////                ).next(new TestCase() // c 13000, p 20000, g -7000
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 13000)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19000)
////                ).next(new TestCase() // c 13000, p 20000, g -7000
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19000)
////                ).next(new TestCase() // c 13000, p 19000, g -6000
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 19000)
////                ).next(new TestCase() // c 1000, p 19000, g -18000
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 1000)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 7000)
////                ).next(new TestCase() // c 1000, p 13000, g -12000
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 7000)
////                ).next(new TestCase() // c 950, p 13000, g -12050
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 950)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 6950)
////                ).next(new TestCase() // c 1100, p 13000, g -11900
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 1100)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 7100)
////
////                // OFF
////                ).next(new TestCase() // c 1000, p 4000, g -3000
////                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true)
////                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true)
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 1000)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 1000)
////                ).next(new TestCase() // c 7500, p 4000, g 3500
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 7500)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 7500)
////                ).next(new TestCase() // c 8500, p 4000, g 4500
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 8500)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 8500)
////                ).next(new TestCase() // c 14500, p 15000, g -1000
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 14500)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 14500)
////                ).next(new TestCase() // c 14500, p 20000, g -5500
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 14500)
////                ).next(new TestCase() // c 13000, p 20000, g -7000
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 13000)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13000)
////                ).next(new TestCase() // c 13000, p 20000, g -7000
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13000)
////                ).next(new TestCase() // c 13000, p 19000, g -6000
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13000)
////                ).next(new TestCase() // c 1000, p 19000, g -18000
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 1000)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 1000)
////                ).next(new TestCase() // c 1000, p 13000, g -12000
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 1000)
////                ).next(new TestCase() // c 950, p 13000, g -12050
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 950)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 950)
////                ).next(new TestCase() // c 1100, p 13000, g -11900
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 1100)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 1100)
////
////                // no limit
////                ).next(new TestCase() // c 1100, p 13000, g -11900
////                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false)
////                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false)
////                        .input("_sum", Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 1000)
////                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 20000)
                ).deactivate();
    }
//
//
//    @Test
//    public void paragraph14a_twoPvInverterTest() throws Exception {
//
//        ControllerParagraph14aImpl sut = new ControllerParagraph14aImpl();
//        new ControllerTest(sut)
//                .addComponent(new DummyInputOutput("io0"))
//                .addComponent(new DummyElectricityMeter("meter0"))
//                .addComponent(new DummyManagedSymmetricPvInverter("pvInverter0"))
//                .addComponent(new DummyManagedSymmetricPvInverter("pvInverter1"))
//                .addReference("componentManager", new DummyComponentManager())
//                .activate(MyConfig.create()
//                        .setId("ctrlParagraph14a0")
//                        .setMode(RelaisMode.FNN2bit1StbV1StbE)
//                        .setInputRelaisId1("io0/InputOutput0")
//                        .setInputRelaisId2("io0/InputOutput1")
//                        .setInputRelaisId3("io0/InputOutput2")
//                        .setInputRelaisId4("io0/InputOutput3")
//                        .setGridmeterId("meter0")
//                        .setPvInverterId("pvInverter0")
//                        .build()
//                ).next(new TestCase() // c 1000, p 4000, g -3000
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false)
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1000)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 7000)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13000)
//
//                // no limit
//                ).next(new TestCase() // c 1000, p 4000, g -3000
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 7000)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13000)
//
//                // limit 60%
//                ).next(new TestCase() // c 1000, p 4000, g -3000
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 5775)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 10725)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 4550)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 8450)
//                ).next(new TestCase() // c 9000, p 15000, g -6000
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 9000)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 7000)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13000)
//                ).next(new TestCase() // c 1000, p 13000, g -12000
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 7000)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13000)
//                ).next(new TestCase() // c 1100, p 13000, g -12000
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1100)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 4585)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 8515)
//
//                // limit 30%
//                ).next(new TestCase() // c 1000, p 4000, g -3000
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false)
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1000)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 3517)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 6533)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 2450)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 4550)
//                ).next(new TestCase() // c 950, p 13000, g -12050
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 950)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 2432)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 4518)
//                ).next(new TestCase() // c 1100, p 13000, g -11900
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1100)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 2485)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 4615)
//
//                // OFF
//                ).next(new TestCase() // c 1000, p 4000, g -3000
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true)
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1000)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 350)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 650)
//                ).next(new TestCase() // c 950, p 13000, g -12050
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 950)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 332)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 618)
//                ).next(new TestCase() // c 1100, p 13000, g -11900
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1100)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 385)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 715)
//
//                // no limit
//                ).next(new TestCase() // c 1100, p 13000, g -11900
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false)
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1000)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 7000)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 13000)
//                ).deactivate();
//    }
//
//    @Test
//    public void paragraph14a_pvInverterWithEvcsClusterTest() throws Exception {
//
//        ControllerParagraph14aImpl sut = new ControllerParagraph14aImpl();
//        new ControllerTest(sut)
//                .addComponent(new DummyInputOutput("io0"))
//                .addComponent(new DummyElectricityMeter("meter0"))
//                .addComponent(new DummyEvcsClusterPeakShaving("evcsCluster0"))
//                .addComponent(new DummyManagedSymmetricPvInverter("pvInverter0"))
//                .addComponent(new DummyManagedSymmetricPvInverter("pvInverter1"))
//                .addReference("componentManager", new DummyComponentManager())
//                .activate(MyConfig.create()
//                        .setId("ctrlParagraph14a0")
//                        .setMode(RelaisMode.FNN2bit1StbV1StbE)
//                        .setInputRelaisId1("io0/InputOutput0")
//                        .setInputRelaisId2("io0/InputOutput1")
//                        .setInputRelaisId3("io0/InputOutput2")
//                        .setInputRelaisId4("io0/InputOutput3")
//                        .setGridmeterId("meter0")
//                        .setPvInverterId("pvInverter0")
//                        .setEvcsIds(new String[]{"evcsCluster0"})
//                        .build()
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false)
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1000)
//                        .input("evcsCluster0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 10000)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT,  5000)
//
//                // limit 30%
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 3667)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 1833)
//                ).next(new TestCase()
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1223)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()                        
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 3815)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 1907)
//                ).next(new TestCase()
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1200)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 3800)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 1900)
//                ).next(new TestCase()
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1160)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 3774)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 1887)
//                ).next(new TestCase()
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 900)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 3600)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 1800)
//                ).next(new TestCase()
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 3000)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 1500)
//
//                // off
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true)
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1000)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 667)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 333)
//                ).next(new TestCase()
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 14900)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 9933)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 4966)
//
//                // no limit
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false)
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 14900)
//                ).next(new TestCase(), RAMP_CYCLES).next(new TestCase()
//                        .output("pvInverter0", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 10000)
//                        .output("pvInverter1", ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 5000)                        
//                ).deactivate();
//    }
//
//    @Test
//    public void paragraph14a_simpleEvcsClusterTest() throws Exception {
//
//        ControllerParagraph14aImpl sut = new ControllerParagraph14aImpl();
//        new ControllerTest(sut)
//                .addComponent(new DummyInputOutput("io0"))
//                .addComponent(new DummyElectricityMeter("meter0"))
//                .addComponent(new DummyManagedSymmetricPvInverter("pvInverter0"))
//                .addComponent(new DummyEvcsClusterPeakShaving("evcsCluster0"))
//                .addReference("componentManager", new DummyComponentManager())
//                .activate(MyConfig.create()
//                        .setId("ctrlParagraph14a0")
//                        .setMode(RelaisMode.FNN2bit1StbV1StbE)
//                        .setInputRelaisId1("io0/InputOutput0")
//                        .setInputRelaisId2("io0/InputOutput1")
//                        .setInputRelaisId3("io0/InputOutput2")
//                        .setInputRelaisId4("io0/InputOutput3")
//                        .setGridmeterId("meter0")
//                        .setPvInverterId("pvInverter0")
//                        .setEvcsIds(new String[]{"evcsCluster0"})
//                        .build()
//
//                // no limit
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false)
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                        .input("evcsCluster0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                        .input("evcsCluster0", ManagedEvcsCluster.ChannelId.EVCS_COUNT, 2)
//                        .output(ControllerParagraph14a.ChannelId.CONSUMPTION_MANAGEMENT, ConsumptionManagment.FULL)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_COUNT, 2)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, -1)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, -1)
//
//                // shut off
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, true)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, true)
//                        .output(ControllerParagraph14a.ChannelId.CONSUMPTION_MANAGEMENT, ConsumptionManagment.OFF)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, 0)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 0)
//                ).next(new TestCase()
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 5000)
//                        .output(ControllerParagraph14a.ChannelId.CONSUMPTION_MANAGEMENT, ConsumptionManagment.OFF)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, 5000)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 5000)
//
//                // unused state
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, true)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false)
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                        .output(ControllerParagraph14a.ChannelId.CONSUMPTION_MANAGEMENT, ConsumptionManagment.UNUSED)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, 0)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 0)
//
//                // scale down
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, true)
//                        .output(ControllerParagraph14a.ChannelId.CONSUMPTION_MANAGEMENT, ConsumptionManagment.REDUCED)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, 7560)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 7560)
//                ).next(new TestCase()
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 3000)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 10560)
//                );
//    }
//
//    @Test
//    public void paragraph14a_multipleEvcsClusterTest() throws Exception {
//
//        ControllerParagraph14aImpl sut = new ControllerParagraph14aImpl();
//        new ControllerTest(sut)
//                .addComponent(new DummyInputOutput("io0"))
//                .addComponent(new DummyElectricityMeter("meter0"))
//                .addComponent(new DummyEvcsClusterPeakShaving("evcsCluster0"))
//                .addComponent(new DummyEvcsClusterPeakShaving("evcsCluster1"))
//                .addComponent(new DummyManagedSymmetricPvInverter("pvInverter0"))
//                .addComponent(new DummyManagedSymmetricPvInverter("pvInverter1"))
//                .addReference("componentManager", new DummyComponentManager())
//                .activate(MyConfig.create()
//                        .setId("ctrlParagraph14a0")
//                        .setMode(RelaisMode.FNN2bit1StbV1StbE)
//                        .setInputRelaisId1("io0/InputOutput0")
//                        .setInputRelaisId2("io0/InputOutput1")
//                        .setInputRelaisId3("io0/InputOutput2")
//                        .setInputRelaisId4("io0/InputOutput3")
//                        .setGridmeterId("meter0")
//                        .setPvInverterId("pvInverter0")
//                        .setEvcsIds(new String[]{"evcsCluster0", "evcsCluster1"})
//                        .build()
//
//                // no limit
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false)
//                        .input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                        .input("pvInverter1", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                        .input("evcsCluster0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                        .input("evcsCluster0", ManagedEvcsCluster.ChannelId.EVCS_COUNT, 2)
//                        .input("evcsCluster1", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
//                        .input("evcsCluster1", ManagedEvcsCluster.ChannelId.EVCS_COUNT, 3)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, -1)
//                        .output("evcsCluster1", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, -1)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_COUNT, 5)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, -1)
//
//                // scale down
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, true)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 7560)
//                        .output("evcsCluster1", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 7560)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, 15120)
//                ).next(new TestCase()
//                        .input("pvInverter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 3000)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 9060)
//                        .output("evcsCluster1", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 9060)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, 18120)
//                ).next(new TestCase()
//                        .input("evcsCluster0", ElectricityMeter.ChannelId.ACTIVE_POWER, 12000)
//                        .input("evcsCluster1", ElectricityMeter.ChannelId.ACTIVE_POWER, 5000)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 12560)
//                        .output("evcsCluster1", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 5560)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, 18120)
//                ).next(new TestCase()
//                        .input("evcsCluster0", ElectricityMeter.ChannelId.ACTIVE_POWER, 12000)
//                        .input("evcsCluster1", ElectricityMeter.ChannelId.ACTIVE_POWER, 11000)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 12000)
//                        .output("evcsCluster1", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 6120)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, 18120)
//                ).next(new TestCase()
//                        .input("evcsCluster0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000)
//                        .input("evcsCluster1", ElectricityMeter.ChannelId.ACTIVE_POWER, 11000)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 18120)
//                        .output("evcsCluster1", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, 0)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, 18120)
//                ).next(new TestCase()
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false)
//                        .input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false)
//                        .output("evcsCluster0", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, -1)
//                        .output("evcsCluster1", ManagedEvcsCluster.ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE, -1)
//                        .output(ControllerParagraph14a.ChannelId.EVCS_CLUSTERS_ALLOWED_POWER, -1)
//                );
//    }
//
}

