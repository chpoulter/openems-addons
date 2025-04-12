package de.poulter.openems.edge.solaredge.pvinverter;

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
