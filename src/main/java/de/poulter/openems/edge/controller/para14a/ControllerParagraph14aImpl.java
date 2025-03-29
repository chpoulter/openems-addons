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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.api.ManagedEvcsCluster;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(
    name = "Controller.Paragraph14aEnWG",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@EventTopics({
    EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS
})
public class ControllerParagraph14aImpl extends AbstractOpenemsComponent 
    implements ControllerParagraph14a, Controller, OpenemsComponent, EventHandler
{

    private static final Logger log = LoggerFactory.getLogger(ControllerParagraph14aImpl.class);

    private boolean debugMode;

    @Reference
    private ComponentManager componentManager;

    private RelaisMode relaisMode;
    private ChannelAddress inputRelais1;
    private ChannelAddress inputRelais2;
    private ChannelAddress inputRelais3;
    private ChannelAddress inputRelais4;

    private ProductionManagment production = ProductionManagment.OFF;
    private ConsumptionManagment consumption = ConsumptionManagment.OFF;

    // grid meter
    private ElectricityMeter gridMeter;

    // pv inverters
    private Map<ManagedSymmetricPvInverter, Integer> pvInverterActivePowerLimits = Collections.emptyMap();
    private Map<ManagedSymmetricPvInverter, Integer> pvInvertersMaxHardware = Collections.emptyMap();
    private Integer pvInvertersLastActivePowerLimitSum;
    private int pvInverterLittleDiffCounts;

    // evcs clusters
    private List<ManagedEvcsCluster> evcsClusters;

    public ControllerParagraph14aImpl() {
        super(
            OpenemsComponent.ChannelId.values(),
            Controller.ChannelId.values(),
            ControllerParagraph14a.ChannelId.values()
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        debugMode = config.debugMode();

        inputRelais1 = ChannelAddress.fromString(config.inputRelais1());
        inputRelais2 = ChannelAddress.fromString(config.inputRelais2());
        inputRelais3 = ChannelAddress.fromString(config.inputRelais3());
        inputRelais4 = ChannelAddress.fromString(config.inputRelais4());

        relaisMode = config.relaisMode();
        production = ProductionManagment.OFF;
        consumption = ConsumptionManagment.OFF;

        gridMeter = componentManager.getComponent(config.gridmeter_id());

        pvInverterActivePowerLimits = new HashMap<>();
        pvInvertersMaxHardware = new HashMap<>();
        pvInvertersLastActivePowerLimitSum = null;

        for (int i = 0; i < config.pvInverter_ids().length; i++) {
            String pvInverter_id = config.pvInverter_ids()[i];
            String pvInverter_maxActivePower = config.pvInverter_maxActivePower()[i];

            ManagedSymmetricPvInverter pvInverter = componentManager.getComponent(pvInverter_id);

            Integer maxActivePower;
            try {
                maxActivePower = Integer.valueOf(pvInverter_maxActivePower);
            } catch(NumberFormatException ex) {
                throw new OpenemsException("Could not parse max active power for pvInverter", ex);
            }

            pvInvertersMaxHardware.put(pvInverter, maxActivePower);
        }

        evcsClusters = new ArrayList<>();
        for (int i = 0; i < config.evcs_ids().length; i++) {
            String evcsCluster_id = config.evcs_ids()[i];

            ManagedEvcsCluster evcsCluster = componentManager.getComponent(evcsCluster_id);
            evcsClusters.add(evcsCluster);
        }
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();

        production = ProductionManagment.OFF;
        consumption = ConsumptionManagment.OFF;

        gridMeter = null;

        pvInvertersMaxHardware = Collections.emptyMap();
        pvInvertersLastActivePowerLimitSum = null;

        evcsClusters = Collections.emptyList();
    }

    @Override
    public void handleEvent(Event event) {
        if (!isEnabled()) return;

        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
                mapRelaisInputsToManagementModes();
                break;
        }
    }

    @Override
    public void run() throws OpenemsNamedException {
        logDebug("new production managment: " + production);
        logDebug("new consumption managment: " + consumption);

        // determine sum of pvInverter production
        int pvInverterSumActivePower = determinePvInverterSumActivePower();

        // calculate pv inverter limits
        calculatePvInverterLimits(pvInverterSumActivePower);

        // set power limit in every cycle otherwise it gets lost
        logDebug("pvInverterActivePowerLimits " + pvInverterActivePowerLimits);
        for (ManagedSymmetricPvInverter pvInverter : pvInverterActivePowerLimits.keySet()) {
            pvInverter.setActivePowerLimit(pvInverterActivePowerLimits.get(pvInverter));
        }

        // calc evcs count and power distribution
        int evcsCount = calcEvcsCount();
        int evcsClustersAllowedPower = calcAllowedPowerToDistributeOnEvcsClusters(evcsCount, pvInverterSumActivePower);
        Map<ManagedEvcsCluster, Integer> allowedPowerPerClusters = distributePower(evcsClustersAllowedPower, evcsClusters);

        // set power limit on each evcs cluster
        for (Entry<ManagedEvcsCluster, Integer> entry : allowedPowerPerClusters.entrySet()) {
            ManagedEvcsCluster evcsCluster = entry.getKey();
            Integer allowedPower = entry.getValue();

            evcsCluster.setMaximumAllowedPowerToDistribute(allowedPower);
        }

        // set channels
        _setEvcsCount(evcsCount);
        _setEvcsClustersAllowedPower(evcsClustersAllowedPower);
    }

    public void logDebug(String message) {
        if (debugMode) {
            logInfo(log, message);
        }
    }

    ////////////////////////////////////////////////////////////////////
    //
    // Relais-Mapping
    //
    ////////////////////////////////////////////////////////////////////

    private Boolean readInputRelais(ChannelAddress channelAddress) {
        try {
            BooleanReadChannel booleanReadChannel = this.componentManager.getChannel(channelAddress);
            return booleanReadChannel.value().get();

        } catch (IllegalArgumentException | OpenemsNamedException ex) {
            logError(log, "Could not read " + channelAddress + ".");
            return null;
        }
    }

    private void mapRelaisInputsToManagementModes() {
        logDebug("relaisMode: " + relaisMode);
        logDebug("  inputRelais1: " + inputRelais1.getChannelId() + ", " + readInputRelais(inputRelais1));
        logDebug("  inputRelais2: " + inputRelais2.getChannelId() + ", " + readInputRelais(inputRelais2));
        logDebug("  inputRelais3: " + inputRelais3.getChannelId() + ", " + readInputRelais(inputRelais3));
        logDebug("  inputRelais4: " + inputRelais4.getChannelId() + ", " + readInputRelais(inputRelais4));

        switch(relaisMode) {
            case DreiRelais1StbE:
                production = ProductionManagment.getForInputs(
                    readInputRelais(inputRelais1),
                    readInputRelais(inputRelais2),
                    readInputRelais(inputRelais3)
                );
                break;

            case Einzelkontakt4StbV:
                consumption = ConsumptionManagment.getForInput(
                    readInputRelais(inputRelais1)
                );
                break;

            case VierRelais1StbE:
                production = ProductionManagment.getForInputs(
                    readInputRelais(inputRelais1),
                    readInputRelais(inputRelais2),
                    readInputRelais(inputRelais3),
                    readInputRelais(inputRelais4)
                );
                break;

            case FNN2bit1StbV:
                consumption = ConsumptionManagment.getForInputs(
                    readInputRelais(inputRelais1),
                    readInputRelais(inputRelais2)
                );
                production = ProductionManagment.OFF;
                break;

            case FNN2bit2StbV:
                consumption = ConsumptionManagment.getForInputs(
                    readInputRelais(inputRelais1),
                    readInputRelais(inputRelais2)
                );
                production = ProductionManagment.OFF;
                break;

            case FNN2bit1StbE:
                consumption = ConsumptionManagment.OFF;
                production = ProductionManagment.getForInputs(
                    readInputRelais(inputRelais1),
                    readInputRelais(inputRelais2)
                );
                break;

            case FNN2bit2StbE:
                consumption = ConsumptionManagment.OFF;
                production = ProductionManagment.getForInputs(
                    readInputRelais(inputRelais1),
                    readInputRelais(inputRelais2)
                );
                break;

            case FNN2bit1StbV1StbE:
                consumption = ConsumptionManagment.getForInputs(
                    readInputRelais(inputRelais1),
                    readInputRelais(inputRelais2)
                );
                production = ProductionManagment.getForInputs(
                    readInputRelais(inputRelais3),
                    readInputRelais(inputRelais4)
                );
                break;

            default:
                logWarn(log, "Mode " + relaisMode + " is not supported yet.");

            case None:
                production = ProductionManagment.OFF;
                consumption = ConsumptionManagment.OFF;

        }

        _setProductionManagment(production);
        _setConsumptionManagmentChannel(consumption);
    }

    ////////////////////////////////////////////////////////////////////
    //
    // PV Inverter
    //
    ////////////////////////////////////////////////////////////////////

    private int determinePvInverterSumActivePower() {

        // current production of pv inverters
        int pvInverterSumActivePower = pvInvertersMaxHardware.keySet().stream()
            .mapToInt( pvInverter -> pvInverter.getActivePower().orElse(0))
            .sum();

        _setPvInverterSumActivePower(pvInverterSumActivePower);

        logDebug("pvInverterSumActivePower " + pvInverterSumActivePower);

        return pvInverterSumActivePower;
    }

    private void calculatePvInverterLimits(int pvInverterSumActivePower) throws OpenemsNamedException {
        logDebug("-PvInverter--------");

        int hardwareMax = pvInvertersMaxHardware.values().stream().mapToInt(i -> i.intValue()).sum();
        _setPvInvertersHardwareMaxActivePower(hardwareMax);
        logDebug("hardwareMax " + hardwareMax);

        // no limit, just set hardware max on every pv inverter
        if (ProductionManagment.FULL.equals(production)) {
            logDebug("No prodction limit required.");

            pvInverterActivePowerLimits = new HashMap<>();
            for (Entry<ManagedSymmetricPvInverter, Integer> entry : pvInvertersMaxHardware.entrySet()) {
                ManagedSymmetricPvInverter pvInverter = entry.getKey();
                Integer pvInverterHarwareMax = entry.getValue();

                pvInverterActivePowerLimits.put(pvInverter, pvInverterHarwareMax);
            }
            pvInvertersLastActivePowerLimitSum = hardwareMax;
            _setPvInvertersActivePowerLimitSum(pvInvertersLastActivePowerLimitSum);

            return;
        }

        int gridActivePower = gridMeter.getActivePower().orElse(0);
        int consumptionPower = (gridActivePower > 0) ? gridActivePower : 0;
        int productionPower = (gridActivePower < 0) ? gridActivePower * -1: 0;
        logDebug("consumption " + consumptionPower + ", production " + productionPower + ", gridActivePower " + gridActivePower);

        // max allowed active production on grid meter
        int pvInvertersActivePowerLimitSum = Math.floorDiv(hardwareMax * production.getFactor(), 100);
        logDebug("pvInvertersActivePowerLimitSum " + pvInvertersActivePowerLimitSum);

        pvInvertersActivePowerLimitSum += gridActivePower + pvInverterSumActivePower;
        logDebug("pvInvertersActivePowerLimitSum " + pvInvertersActivePowerLimitSum);

        pvInvertersActivePowerLimitSum = TypeUtils.fitWithin(0, hardwareMax, pvInvertersActivePowerLimitSum);
        logDebug("pvInvertersActivePowerLimitSum " + pvInvertersActivePowerLimitSum + ", pvInvertersLastActivePowerLimitSum " + pvInvertersLastActivePowerLimitSum);

        if (pvInvertersLastActivePowerLimitSum != null && (pvInvertersActivePowerLimitSum > 100)) {
            int diff = Math.abs(pvInvertersLastActivePowerLimitSum - pvInvertersActivePowerLimitSum);
            logDebug("diff " + diff);

            // increase in slow steps
            if (pvInvertersActivePowerLimitSum > pvInvertersLastActivePowerLimitSum) {

                // if change is small -> do nothing
                if (diff < Math.floorDiv(hardwareMax, 200)) {
                    pvInverterLittleDiffCounts++;

                    if (pvInverterLittleDiffCounts < 5) {
                        logDebug("little diff " + diff);
                        return;
                    }

                    logDebug("little diff anyway " + diff);

                    pvInvertersActivePowerLimitSum = Math.min(pvInvertersLastActivePowerLimitSum + diff, pvInvertersActivePowerLimitSum);
                    logDebug("pvInvertersActivePowerLimitSum 1.1 " + pvInvertersActivePowerLimitSum);

                } else {
                    pvInvertersActivePowerLimitSum = Math.min(pvInvertersLastActivePowerLimitSum + Math.floorDiv(diff, 5), pvInvertersActivePowerLimitSum);
                    logDebug("pvInvertersActivePowerLimitSum 1.2 " + pvInvertersActivePowerLimitSum);
                }
                pvInverterLittleDiffCounts = 0;

            // decrease in steps
            } else {
                pvInvertersActivePowerLimitSum = Math.max(pvInvertersLastActivePowerLimitSum - Math.floorDiv(diff, 2), pvInvertersActivePowerLimitSum);
                logDebug("pvInvertersActivePowerLimitSum 2 " + pvInvertersActivePowerLimitSum);
            }
        }
        pvInvertersLastActivePowerLimitSum = pvInvertersActivePowerLimitSum;
        _setPvInvertersActivePowerLimitSum(pvInvertersActivePowerLimitSum);

        pvInverterActivePowerLimits = new HashMap<>();
        for (Entry<ManagedSymmetricPvInverter, Integer> entry : pvInvertersMaxHardware.entrySet()) {
            ManagedSymmetricPvInverter pvInverter = entry.getKey();
            int pvInverterHarwareMax = entry.getValue();

            double factor = (hardwareMax == 0) ? 0 : (double)pvInverterHarwareMax / (double)hardwareMax;
            int pvInvertermaxActivePower = (int) Math.floor((double)pvInvertersActivePowerLimitSum * factor);
            logDebug("pvInvertermaxActivePower " + pvInvertermaxActivePower);

            pvInvertermaxActivePower = TypeUtils.fitWithin(0, pvInverterHarwareMax, pvInvertermaxActivePower);
            logDebug("pvInvertermaxActivePower " + pvInvertermaxActivePower);

            pvInverterActivePowerLimits.put(pvInverter, pvInvertermaxActivePower);
        }

        logDebug("-------------------");
    }

    ////////////////////////////////////////////////////////////////////
    //
    // Evcs cluster
    //
    ////////////////////////////////////////////////////////////////////

    private int calcEvcsCount() {
        return evcsClusters.stream()
            .mapToInt( evcsCluster -> evcsCluster.getEvcsCount().orElse(0))
            .sum();
    }

    private int determineGleichzeitigkeitsfaktor(int evcs) {
        if (evcs < 2) return 0;
        if (evcs == 2) return 80;
        if (evcs == 3) return 75;
        if (evcs == 4) return 70;
        if (evcs == 5) return 65;
        if (evcs == 6) return 60;
        if (evcs == 7) return 55;
        if (evcs == 8) return 50;
        if (evcs > 8) return 45;

        return 0;
    }

    private int calcAllowedPowerToDistributeOnEvcsClusters(int evcsCount, int pvInverterSumActivePower) {

        int gridActivePower = gridMeter.getActivePower().orElse(0);
        int consumptionPower = (gridActivePower > 0) ? gridActivePower : 0;
        int productionPower = (gridActivePower < 0) ? gridActivePower * -1: 0;
        logDebug("consumption " + consumptionPower + ", production " + productionPower + ", gridActivePower " + gridActivePower);

        // Berechnung der erlaubten Mindestleistung gemäß 
        // Anlage 1 BK-622-300 Bundesnetzagentur Abschnitt 4.5 Satz 4
        // Stand 27.11.2023
        int pMin = 4200 + (evcsCount - 1) * determineGleichzeitigkeitsfaktor(evcsCount) * 42;

        switch(consumption) {
            case FULL: return -1;
            case REDUCED: return pMin + pvInverterSumActivePower;
            case OFF: return pvInverterSumActivePower;
            case UNUSED:
            default: return 0;
        }
    }

    private Map<ManagedEvcsCluster, Integer> distributePower(int allowedPowerToDistribute, Collection<ManagedEvcsCluster> evcsClusters) {
        logDebug("-Evcs--------------");
        logDebug("allowedPowerToDistribute " + allowedPowerToDistribute);

        Map<ManagedEvcsCluster, Integer> allowedPowerPerCluster = new HashMap<>();

        // skip distribution in case of OFF or NOLIMIT
        if (allowedPowerToDistribute < 1) {
            for (ManagedEvcsCluster cluster : evcsClusters) {
                allowedPowerPerCluster.put(cluster, allowedPowerToDistribute);
            }

        // try to distribute allowed max power in a useful way
        } else {

            int allowedPowerLeft = allowedPowerToDistribute;

            for (ManagedEvcsCluster cluster : evcsClusters) {
                logDebug("cluster " + cluster.id());

                int clusterActivePower = cluster.getActivePower().orElse(0);
                int allowedPower = TypeUtils.fitWithin(0, allowedPowerLeft, clusterActivePower);
                allowedPowerLeft -= allowedPower;

                logDebug("allowedPower " + allowedPower);
                allowedPowerPerCluster.put(cluster, allowedPower);
            }

            logDebug("allowedPowerLeft " + allowedPowerLeft);
            logDebug("allowedPowerPerCluster " + allowedPowerPerCluster);

            if (allowedPowerLeft > 0) {
                int clusterCount = evcsClusters.size();
                int additionalPowerPerCluster = (clusterCount == 0) ? 0 : Math.divideExact(allowedPowerLeft, clusterCount);

                for (ManagedEvcsCluster cluster : evcsClusters) {
                    int allowedPower = allowedPowerPerCluster.get(cluster) + additionalPowerPerCluster;

                    logDebug("allowedPower " + allowedPower);

                    allowedPowerPerCluster.put(cluster, allowedPower);
                }
            }

            logDebug("allowedPowerPerCluster " + allowedPowerPerCluster);
        }

        logDebug("-------------------");

        return allowedPowerPerCluster;
    }

}
