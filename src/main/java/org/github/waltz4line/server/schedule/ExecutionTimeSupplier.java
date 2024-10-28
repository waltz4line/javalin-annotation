package org.github.waltz4line.server.schedule;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ExecutionTimeSupplier {

    public static final long ALREADY_FINISHED_TIME = -1;

    public static final long ALREADY_FINISHED_DELAY = 0x0FFFFFFFFFFFFFFFL;

    private static final Clock CLOCK = Clock.systemDefaultZone();

    private final ExecutionTime executionTime;

    private final AtomicLong waitingStateExecutionTime = new AtomicLong(0);

    private ExecutionTimeSupplier(ExecutionTime executionTime) {
        this.executionTime = executionTime;
        nextExecutionMillis();
    }

    private Optional<ZonedDateTime> nextExecution() {
        return executionTime.nextExecution(ZonedDateTime.now());
    }

    public long nextExecutionMillis() {
        if (ALREADY_FINISHED_TIME == waitingStateExecutionTime.get()) {
            return ALREADY_FINISHED_TIME;
        } else if (waitingStateExecutionTime.get() > CLOCK.millis()) {
            return waitingStateExecutionTime.get();
        } else {
            Optional<ZonedDateTime> nextExecution = nextExecution();
            if (nextExecution.isPresent()) {
                long nextExecutionMillis = nextExecution.get().toInstant().toEpochMilli();
                waitingStateExecutionTime.set(nextExecutionMillis);
                return nextExecutionMillis;
            } else {
                waitingStateExecutionTime.set(ALREADY_FINISHED_TIME);
            }
        }
        return ALREADY_FINISHED_TIME;
    }

    public long getDelay() {
        if (ALREADY_FINISHED_TIME == waitingStateExecutionTime.get()) {
            return ALREADY_FINISHED_DELAY;
        }
        return waitingStateExecutionTime.get() - CLOCK.millis();
    }

    public static ExecutionTimeSupplier newCalculator(String cronExpression) {
        Objects.requireNonNull(cronExpression, "cronExpression must not be null");
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        Cron parsedCron = parser.parse(cronExpression);
        ExecutionTime executionTime = ExecutionTime.forCron(parsedCron);
        return new ExecutionTimeSupplier(executionTime);
    }

}
