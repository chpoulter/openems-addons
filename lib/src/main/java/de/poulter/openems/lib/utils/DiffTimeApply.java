/*
 *   OpenEMS Addons Lib
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

package de.poulter.openems.lib.utils;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public abstract class DiffTimeApply<T extends Number> {

    private static final Logger log = LoggerFactory.getLogger(DiffTimeApply.class);

    private Instant nextTimeoutAt = Instant.MIN;
    private Optional<T> currentValue = Optional.empty();
    private double compareDiff;

    public DiffTimeApply(double compareDiff) {
        this.compareDiff = compareDiff;
    }

    public void nextValue(Optional<Long> timeoutValue, T nextValue) throws OpenemsNamedException {
        long timeout = timeoutValue.orElse(30l);
        boolean timeApply = Instant.now().isAfter(nextTimeoutAt);
        boolean diffApply = currentValue.isEmpty() 
                            || (Math.abs(nextValue.doubleValue() - currentValue.get().doubleValue()) > compareDiff);

        log.info("currentValue " + currentValue.toString() + ", nextValue " + nextValue);
        log.info("timeout " + timeout + ", nextTimeoutAt " + nextTimeoutAt);
        log.info("diffApply " + diffApply + ", timeApply " + timeApply + " " + (diffApply || timeApply ? " [ APPLY ]" : "[ NOPE  ]"));

        if (diffApply || timeApply) {
            log.info("Setting to " + nextValue);

            accept(nextValue);

            nextTimeoutAt = Instant.now().plusSeconds(Math.ceilDiv(timeout, 2));
            currentValue = Optional.of(nextValue);
        }
    }

    public abstract void accept(T value) throws OpenemsNamedException;

}
