/*
 *   OpenEMS IO bestep bundle
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

package de.poulter.openems.edge.io.bestep;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Debounce;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;

public interface IoBestep extends OpenemsComponent, ModbusSlave {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        INPUT_1(new BooleanDoc().debounce(2, Debounce.SAME_VALUES_IN_A_ROW_TO_CHANGE).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),
        INPUT_2(new BooleanDoc().debounce(2, Debounce.SAME_VALUES_IN_A_ROW_TO_CHANGE).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),
        INPUT_3(new BooleanDoc().debounce(2, Debounce.SAME_VALUES_IN_A_ROW_TO_CHANGE).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),
        INPUT_4(new BooleanDoc().debounce(2, Debounce.SAME_VALUES_IN_A_ROW_TO_CHANGE).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)),

        RELAY_1(new BooleanDoc().accessMode(AccessMode.READ_WRITE).persistencePriority(PersistencePriority.MEDIUM)),
        RELAY_2(new BooleanDoc().accessMode(AccessMode.READ_WRITE).persistencePriority(PersistencePriority.MEDIUM)),
        RELAY_3(new BooleanDoc().accessMode(AccessMode.READ_WRITE).persistencePriority(PersistencePriority.MEDIUM)),
        RELAY_4(new BooleanDoc().accessMode(AccessMode.READ_WRITE).persistencePriority(PersistencePriority.MEDIUM)),

        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }
}
