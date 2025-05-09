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

import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.poulter.openems.lib.mean.WeightedMean;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
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
    EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS,
    EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS
})
public class ControllerParagraph14aImpl extends AbstractOpenemsComponent 
    implements ControllerParagraph14a, Controller, OpenemsComponent, EventHandler
{

    private static final Logger log = LoggerFactory.getLogger(ControllerParagraph14aImpl.class);

    @Reference
    private ConfigurationAdmin cm;

    @Reference
    private ComponentManager componentManager;

    // Relais
    private RelaisMode relaisMode;
    private ChannelAddress inputRelais1;
    private ChannelAddress inputRelais2;
    private ChannelAddress inputRelais3;
    private ChannelAddress inputRelais4;

    // grid meter
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private ElectricityMeter gridMeter;

    // pv inverter
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private ManagedSymmetricPvInverter pvInverter;
    private Optional<Integer> pvInverterActivePowerLimit = Optional.empty();
    private WeightedMean pvInverterActivePowerLimitMean = new WeightedMean(15d, 15d, 15d, 15d, 20d, 30d, 40d, 50d, 60d, 75d);

    // evcs
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private ManagedEvcsCluster evcsCluster;
    private Optional<Integer> evcsClusterMaximumAllowedPowerToDistribute = Optional.empty();
    private WeightedMean evcsClusterMaximumAllowedPowerToDistributeMean = new WeightedMean(15d ,15d ,15d ,15d ,20d ,30d ,40d ,50d ,60d ,75d);

    // misc
    private ProductionManagment production = ProductionManagment.OFF;
    private ConsumptionManagment consumption = ConsumptionManagment.OFF;
    private boolean debugMode;

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

        if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "gridMeter", config.gridMeter_id())) {
            return;
        }

        if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "pvInverter", config.pvInverter_id())) {
            return;
        }

        if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "evcsCluster", config.evcsCluster_id())) {
            return;
        }

        inputRelais1 = ChannelAddress.fromString(config.inputRelais1());
        inputRelais2 = ChannelAddress.fromString(config.inputRelais2());
        inputRelais3 = ChannelAddress.fromString(config.inputRelais3());
        inputRelais4 = ChannelAddress.fromString(config.inputRelais4());

        debugMode = config.debugMode();
        relaisMode = config.relaisMode();

        production = ProductionManagment.OFF;
        consumption = ConsumptionManagment.OFF;

        setRunStatus(Level.INFO);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();

        production = ProductionManagment.OFF;
        consumption = ConsumptionManagment.OFF;

        setRunStatus(Level.OK);
    }

    @Override
    public void handleEvent(Event event) {
        if (!isEnabled()) return;

        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
                setRunStatus(Level.OK);
                mapRelaisInputsToManagementModes();
                break;

            case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
                checkEvcsMaximumAllowedPowerToDistribute();
                break;
        }
    }

    @Override
    public void run() throws OpenemsNamedException {
        logDebug("production managment:  " + production);
        logDebug("consumption managment: " + consumption);

        calculatePvInverterActivePowerLimit();
        checkPvInverterLimit();

        calculateEvcsClusterMaximumAllowedPowerToDistribute();
    }


    private void logDebug(String message) {
        if (debugMode) {
            logInfo(log, message);
        }
    }

    private void setRunStatus(Level level) {
        channel(Controller.ChannelId.RUN_FAILED).setNextValue(level);
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
            setRunStatus(Level.WARNING);
        }

        return null;
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
                setRunStatus(Level.FAULT);

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

    private void checkPvInverterLimit() {

        if (pvInverter == null) {
            logDebug("No pvInverter defined.");
            setRunStatus(Level.WARNING);
            return;
        }

        IntegerWriteChannel activePowerLimitChannel = pvInverter.getActivePowerLimitChannel();
        logDebug("checkPvInverterLimit activePowerLimitChannel "+ activePowerLimitChannel.getNextWriteValue());

        Optional<Integer> activePowerLimit = activePowerLimitChannel
            .getNextWriteValue()
            .map( value -> (value < 0) ? null : value )
            .map( value -> Optional.of(Math.min(value, pvInverterActivePowerLimit.orElse(Integer.MAX_VALUE))))
            .orElse(pvInverterActivePowerLimit);

        logDebug("checkPvInverterLimit activePowerLimit "+ activePowerLimit);

        try {
            if (activePowerLimit.isPresent()) {
                activePowerLimitChannel.setNextWriteValue(activePowerLimit.get());
            }
        } catch (OpenemsNamedException ex) {
            log.error("Could not set new limit on pv inverter.", ex);
            setRunStatus(Level.FAULT);
        }

        logDebug("checkPvInverterLimit activePowerLimitChannel "+ activePowerLimitChannel.getNextWriteValue());
    }

    private void calculatePvInverterActivePowerLimit() throws OpenemsNamedException {
        logDebug("-PvInverter--------");

        if (pvInverter == null) {
            setRunStatus(Level.WARNING);
            pvInverterActivePowerLimitMean.clear();

            logWarn(log, "No Pv inverter.");
            logDebug("-------------------");

            return;
        }

        Value<Integer> pvInverterActivePowerValue = pvInverter.getActivePower();
        if (!pvInverterActivePowerValue.isDefined()) {
            setRunStatus(Level.WARNING);
            pvInverterActivePowerLimitMean.clear();

            logWarn(log, "Pv inverter has no active power defined.");
            logDebug("-------------------");

            return;
        }
        int pvInverterActivePower = pvInverterActivePowerValue.get();
        _setPvInverterActivePower(pvInverterActivePower);
        logDebug("pvInverterActivePower " + pvInverterActivePower);

        Value<Integer> pvInverterMaxActivePowerValue = pvInverter.getMaxActivePower();
        if (!pvInverterMaxActivePowerValue.isDefined()) {
            setRunStatus(Level.WARNING);
            pvInverterActivePowerLimitMean.clear();

            logWarn(log, "Pv inverter has no max active power defined.");
            logDebug("-------------------");

            return;
        }
        int pvInverterMaxActivePower = pvInverterMaxActivePowerValue.get();
        _setPvInverterMaxActivePower(pvInverterMaxActivePower);
        logDebug("pvInverterMaxActivePower " + pvInverterMaxActivePower);

        // no limit, just set hardware max on every pv inverter
        if (ProductionManagment.FULL.equals(production)) {
            pvInverterActivePowerLimitMean.clear();
            pvInverterActivePowerLimit = Optional.empty();
            _setPvInverterActivePowerLimit(null);

            logDebug("No pv inverter prodction limit required.");
            logDebug("-------------------");

            return;
        }

        int gridActivePower = gridMeter.getActivePower().orElse(0);
        int consumptionPower = (gridActivePower > 0) ? gridActivePower : 0;
        int productionPower = (gridActivePower < 0) ? gridActivePower * -1: 0;
        logDebug("consumption " + consumptionPower + ", production " + productionPower + ", gridActivePower " + gridActivePower);

        // max allowed active production on grid meter
        int activePowerLimit = Math.floorDiv(pvInverterMaxActivePower * production.getFactor(), 100);
        logDebug("activePowerLimit " + activePowerLimit);

        activePowerLimit += gridActivePower + pvInverterActivePower;
        logDebug("activePowerLimit " + activePowerLimit);

        activePowerLimit = TypeUtils.fitWithin(0, pvInverterMaxActivePower, activePowerLimit);
        logDebug("activePowerLimit " + activePowerLimit);

        activePowerLimit = (int) pvInverterActivePowerLimitMean.nextValue(activePowerLimit);
        logDebug("activePowerLimit " + activePowerLimit);

        pvInverterActivePowerLimit = Optional.of(activePowerLimit);
        _setPvInverterActivePowerLimit(activePowerLimit);

        logDebug("-------------------");
    }


    ////////////////////////////////////////////////////////////////////
    //
    // Evcs cluster
    //
    ////////////////////////////////////////////////////////////////////

    private void checkEvcsMaximumAllowedPowerToDistribute() {

        if (evcsCluster == null) {
            logDebug("No evcs cluster defined.");
            setRunStatus(Level.WARNING);
            return;
        }

        logDebug("checkEvcsMaximumAllowedPowerToDistribute evcsClusterMaximumAllowedPowerToDistribute "+ evcsClusterMaximumAllowedPowerToDistribute);

        IntegerWriteChannel maximumAllowedPowerToDistributeChannel = evcsCluster.getMaximumAllowedPowerToDistributeChannel();

        int maximumAllowedPowerToDistribute = maximumAllowedPowerToDistributeChannel
            .getNextWriteValue()
            .map( value -> value < 0 ? null : value )
            .map( value -> TypeUtils.fitWithin(0, evcsClusterMaximumAllowedPowerToDistribute.orElse(Integer.MAX_VALUE), value) )
            .orElse(evcsClusterMaximumAllowedPowerToDistribute.orElse(-1));

        logDebug("checkEvcsMaximumAllowedPowerToDistribute maximumAllowedPowerToDistribute "+ maximumAllowedPowerToDistribute);

        try {
            maximumAllowedPowerToDistributeChannel.setNextWriteValue(maximumAllowedPowerToDistribute);

        } catch (OpenemsNamedException ex) {
            log.error("Could not set new maximum allowed power to distribute on evcs cluster.", ex);
            setRunStatus(Level.FAULT);
        }
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

    private void calculateEvcsClusterMaximumAllowedPowerToDistribute() {

        logDebug("-EVCS cluster------");

        if (evcsCluster == null) {
            logDebug("No evcs cluster defined.");
            setRunStatus(Level.WARNING);
            return;
        }

        int evcsCount = evcsCluster.getEvcsCount().orElse(0);
        _setEvcsCount(evcsCount);

        if (ConsumptionManagment.FULL.equals(consumption)) {
            logDebug("No evcs cluster maximum allowed power to distribute required.");
            setRunStatus(Level.WARNING);

            evcsClusterMaximumAllowedPowerToDistribute = Optional.empty();
            _setEvcsClusterMaximumAllowedPowerToDistribute(null);

            logDebug("-------------------");
            return;
        }

        int pvInverterActivePower = pvInverter.getActivePower().orElse(0);
        logDebug("pvInverterActivePower " + pvInverterActivePower);

        int gridActivePower = gridMeter.getActivePower().orElse(0);
        int consumptionPower = (gridActivePower > 0) ? gridActivePower : 0;
        int productionPower = (gridActivePower < 0) ? gridActivePower * -1: 0;
        logDebug("consumption " + consumptionPower + ", production " + productionPower + ", gridActivePower " + gridActivePower);

        // Berechnung der erlaubten Mindestleistung gemäß 
        // Anlage 1 BK-622-300 Bundesnetzagentur Abschnitt 4.5 Satz 4
        // Stand 27.11.2023
        int pMin = 4200 + (evcsCount - 1) * determineGleichzeitigkeitsfaktor(evcsCount) * 42;
        logDebug("pMin " + pMin);

        int maximumAllowedPowerToDistribute = switch(consumption) {
            case REDUCED -> pMin + pvInverterActivePower;
            case OFF -> pvInverterActivePower;
            default -> 0;
        };
        logDebug("maximumAllowedPowerToDistribute " + maximumAllowedPowerToDistribute);

        maximumAllowedPowerToDistribute = (int) evcsClusterMaximumAllowedPowerToDistributeMean.nextValue(maximumAllowedPowerToDistribute);
        logDebug("maximumAllowedPowerToDistribute " + maximumAllowedPowerToDistribute);

        evcsClusterMaximumAllowedPowerToDistribute = Optional.of(maximumAllowedPowerToDistribute);
        _setEvcsClusterMaximumAllowedPowerToDistribute(maximumAllowedPowerToDistribute);

        logDebug("-------------------");
    }

}
