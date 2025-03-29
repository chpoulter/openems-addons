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

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evcs.api.ChargeMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

    private final Builder builder;

    protected static class Builder {
        private String id;
        private boolean enabled = true;
        private boolean debugEnabled = true;
        private RelaisMode relaisMode;
        private String inputRelaisId1;
        private String inputRelaisId2;
        private String inputRelaisId3;
        private String inputRelaisId4;
        private String gridmeterId;
        private String[] pvInverterIds = {};
        private String[] pvInverterMaxActivePower = {};
        private String[] evcsIds = {};

        private Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setMode(RelaisMode relaisMode) {
            this.relaisMode = relaisMode;
            return this;
        }

        public Builder setInputRelaisId1(String inputRelaisId1) {
            this.inputRelaisId1 = inputRelaisId1;
            return this;
        }

        public Builder setInputRelaisId2(String inputRelaisId2) {
            this.inputRelaisId2 = inputRelaisId2;
            return this;
        }

        public Builder setInputRelaisId3(String inputRelaisId3) {
            this.inputRelaisId3 = inputRelaisId3;
            return this;
        }

        public Builder setInputRelaisId4(String inputRelaisId4) {
            this.inputRelaisId4 = inputRelaisId4;
            return this;
        }

        public Builder setGridmeterId(String gridmeterId) {
            this.gridmeterId = gridmeterId;
            return this;
        }

        public Builder setPvInverterIds(String[] pvInverterIds) {
            this.pvInverterIds = pvInverterIds;
            return this;
        }

        public Builder setPvInverterMaxActivePower(String[] pvInverterMaxActivePower) {
            this.pvInverterMaxActivePower = pvInverterMaxActivePower;
            return this;
        }

        public Builder setEvcsIds(String[] evcsIds) {
            this.evcsIds = evcsIds;
            return this;
        }

        public MyConfig build() {
            return new MyConfig(this);
        }
    }

    public static Builder create() {
        return new Builder();
    }

    private MyConfig(Builder builder) {
        super(Config.class, builder.id);

        this.builder = builder;
    }

    @Override
    public String id() {
        return builder.id;
    }

    @Override
    public String inputRelais1() {
        return builder.inputRelaisId1;
    }

    @Override
    public String inputRelais2() {
        return builder.inputRelaisId2;
    }

    @Override
    public String inputRelais3() {
        return builder.inputRelaisId3;
    }

    @Override
    public String inputRelais4() {
        return builder.inputRelaisId4;
    }

    @Override
    public String[] pvInverter_ids() {
        return builder.pvInverterIds;
    }

    @Override
    public String[] pvInverter_maxActivePower() {
        return builder.pvInverterMaxActivePower;
    }

    @Override
    public RelaisMode relaisMode() {
        return builder.relaisMode;
    }

    @Override
    public String[] evcs_ids() {
        return builder.evcsIds;
    }

    @Override
    public String gridmeter_id() {
        return builder.gridmeterId;
    }

    @Override
    public boolean debugMode() {
        return builder.debugEnabled;
    }

}

