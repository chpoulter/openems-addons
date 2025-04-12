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

import java.util.stream.DoubleStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

public class WeightedMean {

    private DescriptiveStatistics ds;
    private Mean mean;
    private double[] weights;
    private int size;

    public WeightedMean(double... weights) {
        this.weights = weights;
        this.size = weights.length;
        this.mean = new Mean();
    }

    public void clear() {
        ds = null;
    }

    public void addValue(double value) {
        if (ds == null) {
            double[] initial = DoubleStream.generate(() -> value).limit(size).toArray();
            ds = new DescriptiveStatistics(initial);
            ds.setWindowSize(size);
            ds.setMeanImpl(mean);
        }

        ds.addValue(value);
    }

    public double getMean() {
        if (ds == null) return 0d;

        return mean.evaluate(ds.getValues(), weights);
    }

    public double nextValue(double value) {
        addValue(value);
        return getMean();
    }

}
