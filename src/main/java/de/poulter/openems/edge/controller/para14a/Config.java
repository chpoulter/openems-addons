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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Controller Paragraph14aEnWG",
    description = "Implements some support for §14a EnWG in OpenEMS with relais interface.")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique id of this component")
    String id() default "ctrlParagraph14a0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this component; defaults to component-id")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
    boolean debugMode() default false;

    @AttributeDefinition(name = "RelaisMode", description = "Relais assignment mode.")
    RelaisMode relaisMode() default RelaisMode.FNN2bit1StbV1StbE;

    @AttributeDefinition(name = "Digital Input Relais 1", description = "Input channel for relais E1.")
    String inputRelais1() default "io0/InputOutput0";

    @AttributeDefinition(name = "Digital Input Relais 2", description = "Input channel for relais E2.")
    String inputRelais2() default "io0/InputOutput1";

    @AttributeDefinition(name = "Digital Input Relais 3", description = "Input channel for relais E3.")
    String inputRelais3() default "io0/InputOutput2";

    @AttributeDefinition(name = "Digital Input Relais 4", description = "Input channel for relais E4.")
    String inputRelais4() default "io0/InputOutput3";

    @AttributeDefinition(name = "Grid meter", description = "The grid meter used for calculations.")
    String gridmeter_id() default "meter0";

    @AttributeDefinition(name = "PvInverter-IDs", description = "Ids of pv-inverter devices.")
    String[] pvInverter_ids() default { "pvInverter0" };

    @AttributeDefinition(name = "PvInverter max active power", description = "Maximum active power for each pv inverter in watt.")
    String[] pvInverter_maxActivePower() default { "10000" };

    @AttributeDefinition(name = "Evcs-IDs", description = "Ids of evcs devices.")
    String[] evcs_ids() default { "evcsCluster0" };

    String webconsole_configurationFactory_nameHint() default "Controller Paragraph 14a EnWG [{id}]";
}
