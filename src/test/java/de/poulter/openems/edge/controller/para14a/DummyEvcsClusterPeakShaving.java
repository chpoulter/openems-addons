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

import io.openems.common.types.MeterType;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcsCluster;
import io.openems.edge.evcs.api.MetaEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.meter.api.ElectricityMeter;

public class DummyEvcsClusterPeakShaving extends AbstractDummyOpenemsComponent<DummyEvcsClusterPeakShaving>
    implements MetaEvcs, OpenemsComponent, Evcs, ElectricityMeter, ManagedEvcsCluster
{

    public DummyEvcsClusterPeakShaving(String id) {
        super(id,
            OpenemsComponent.ChannelId.values(),
            ElectricityMeter.ChannelId.values(),
            Evcs.ChannelId.values(),
            ManagedEvcsCluster.ChannelId.values()
        );
    }

    @Override
    public MeterType getMeterType() {
        return MeterType.MANAGED_CONSUMPTION_METERED;
    }

    @Override
    public PhaseRotation getPhaseRotation() {
        return PhaseRotation.L1_L2_L3;
    }

    @Override
    protected DummyEvcsClusterPeakShaving self() {
        return this;
    }

}
