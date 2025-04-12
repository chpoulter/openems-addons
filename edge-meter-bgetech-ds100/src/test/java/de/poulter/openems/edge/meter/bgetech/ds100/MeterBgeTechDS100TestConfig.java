/*
 *   OpenEMS Meter B+G E-Tech DS100 bundle
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

package de.poulter.openems.edge.meter.bgetech.ds100;

import io.openems.common.utils.ConfigUtils;
import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;

public class MeterBgeTechDS100TestConfig extends AbstractComponentConfig implements Config {

    protected static class Builder {
    	private String id;
    	private String modbusId = null;
    	private int modbusUnitId;
        private MeterType type;
        private boolean invert;

    	private Builder() {}

    	public Builder setId(String id) {
            this.id = id;
            return this;
    	}

    	public Builder setModbusId(String modbusId) {
            this.modbusId = modbusId;
            return this;
    	}

    	public Builder setModbusUnitId(int modbusUnitId) {
            this.modbusUnitId = modbusUnitId;
            return this;
    	}

    	public Builder setType(MeterType type) {
            this.type = type;
            return this;
        }

        public Builder setInvert(boolean invert) {
            this.invert = invert;
            return this;
        }

    	public MeterBgeTechDS100TestConfig build() {
	        return new MeterBgeTechDS100TestConfig(this);
    	}
    }

    public static Builder create() {
        return new Builder();
    }

    private final Builder builder;

    private MeterBgeTechDS100TestConfig(Builder builder) {
        super(Config.class, builder.id);
        this.builder = builder;
    }

    @Override
    public String modbus_id() {
        return this.builder.modbusId;
    }

    @Override
    public String Modbus_target() {
        return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
    }

    @Override
    public int modbusUnitId() {
        return this.builder.modbusUnitId;
    }

    @Override
    public MeterType type() {
        return this.builder.type;
    }

    @Override
    public boolean invert() {
        return this.builder.invert;
    }

}
