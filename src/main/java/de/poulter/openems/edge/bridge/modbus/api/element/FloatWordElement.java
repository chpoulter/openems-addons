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

package de.poulter.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.element.AbstractSingleWordElement;
import io.openems.edge.common.type.TypeUtils;

/**
 * A FloatWordElement represents a Float value in an {@link AbstractSingleWordElement}.
 */
public class FloatWordElement extends AbstractSingleWordElement<FloatWordElement, Float> {

    public FloatWordElement(int address) {
        super(OpenemsType.FLOAT, address);
    }
    
    @Override
    protected FloatWordElement self() {
        return this;
    }
    
    @Override
    protected Float byteBufferToValue(ByteBuffer buff) {
        Short s = buff.getShort(0);
        return s.floatValue();
    }
    
    @Override
    protected void valueToByteBuffer(ByteBuffer buff, Float value) {
        Short s = TypeUtils.getAsType(OpenemsType.SHORT, value);
        buff.putShort(s.shortValue());
    }

}