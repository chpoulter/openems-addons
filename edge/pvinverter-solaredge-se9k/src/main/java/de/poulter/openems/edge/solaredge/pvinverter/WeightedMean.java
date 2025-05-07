/*
 *   OpenEMS PvInverter SolarEdge Se9k bundle
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

package de.poulter.openems.edge.solaredge.pvinverter;

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

    public void addValue(double value, int countLower, int countHigher) {
        double currentMean = getMean();

        if ((currentMean * 1.1) < value) {
            addValue(value, countHigher);

        } else if ((currentMean * 0.9) > value) {
            addValue(value, countLower);

        } else {
            addValue(value);
        }
    }

    public void addValue(double value, int count) {
        for (int i = 0; i < count ; i++) {
            addValue(value);
        }
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
}
