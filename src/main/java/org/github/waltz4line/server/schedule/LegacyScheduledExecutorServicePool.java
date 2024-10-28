/*
 * MIT License
 *
 * Copyright (c) 2017 Matthew Lohbihler
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.github.waltz4line.server.schedule;

import org.github.waltz4line.server.schedule.thread.NamedThreadFactory;
import org.slf4j.Logger;

import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;


/**
 *
 * -- 수정 사항 --
 * # 정해진 스케줄링의 Quantize 및 Cron 을 적용하기 위해 수정함.
 * # 캐시된 스레드 풀에 대해 타이머 스레드를 실행시킴.
 * # 정해진 시작 시간 및 종료 시간을 갖는 스케줄을 지원 함.
 * # 또한 예약된 실행이 지난 작업에 대해서는 ignore 이벤트를 발생시키도록 함.
 *
 * @author song-uiyoung
 */
public class LegacyScheduledExecutorServicePool implements ScheduledExecutorService, Runnable {

    private final Logger logger;

    /*
     * 스케줄러의 구동 상태
     */
    private enum State {
        running, stopping, stopped
    }

    // 스케줄 Timer 를 위한 Clock
    private final Clock clock;
    // 캐시된 스레드 풀을 이용하기 위한 ExecutorService
    private final ExecutorService executorService;
    // 스케줄링을 관리하기 위한 스케줄러 스레드
    private final Thread scheduler;
    // 스케줄러 상태
    private volatile State state;
    // 실행 태스크 리스트
    private final List<ScheduledTrigger<?>> tasks = new LinkedList<>();

    public static LegacyScheduledExecutorServicePool start(Logger logger, String pooName) {
        return LegacyScheduledExecutorServicePool.start(logger, Clock.systemUTC(), pooName);
    }

    public static LegacyScheduledExecutorServicePool start(Logger logger, Clock clock, String pooName) {
        return new LegacyScheduledExecutorServicePool(logger, clock, pooName);
    }

    /**
     * @param poolName 태스크를 동작시킬 스레드 생성 시 부여할 스레드 이름 접두사
     */
    private LegacyScheduledExecutorServicePool(Logger logger, Clock clock, String poolName) {
        this.logger = logger;
        this.clock = clock;
        this.scheduler = new Thread(this, "ScheduledExecutorServicePool");
        this.state = State.running;
        this.scheduler.start();
        this.executorService = Executors.newCachedThreadPool(new NamedThreadFactory(poolName));
    }

    //
    // -- implements for Runnable
    //     : 스케줄러 스레드를 통해 동작할 내용
    //

    @Override
    public void run() {

        try {

            while (state == State.running) {

                synchronized (tasks) {
                    long waitTime;
                    ScheduledTrigger<?> task;

                    // Poll for a task.
                    if (tasks.isEmpty())
                        // No tasks are scheduled. We could wait indefinitely here since we'll be notified
                        // of a change, but out of paranoia we'll only wait one second. When there are no
                        // tasks, this introduces nominal overhead.
                        waitTime = 1000;
                    else {
                        // 태스크 추가 시 실행 순으로 정렬하여 추가되어 있으므로
                        // 리스트 순서대로 태스크를 검사하도록 함
                        task = tasks.get(0);
                        // 첫 번째로 실행시켜야 할 태스크의 대기 시간을 구한다.
                        waitTime = task.getDelay(TimeUnit.MILLISECONDS);

                        // 태스크 시간이 임박한 경우...
                        if (waitTime <= 0) {
                            // Remove the task
                            tasks.remove(0);
                            if (!task.isCancelled()) {
                                // 취소된 태스크가 아니라면 바로 실행 시킴
                                // Execute the task
                                task.execute();
                            }
                        }
                    }

                    // 첫번쨰 태스크 시간이 도래하지 않았으므로 대기한다.
                    // 일정시간 동안 대기한다.
                    if (waitTime > 0) {
                        try {
                            tasks.wait(waitTime);
                        } catch (final InterruptedException e) {
                            logger.warn("Interrupted", e);
                        }
                    }
                }
            }

        } finally {
            state = State.stopped;
        }
    }

    //
    // -- implements for ScheduledExecutorService
    //      : 기본 스케줄링에 대한 동작
    //

    /**
     * (기본 트리거)
     * OneTimeTrigger 를 이용한 스케줄링 (반복 X, 한번만 실행)
     */
    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return addTask(new OneTimeTrigger(command, delay, unit));
    }

    /**
     * (기본 트리거)
     * FixedRateTrigger 를 이용한 스케줄링 (반복 O)
     * 일정한 시간 간격으로 계속 실행. (완료 여부와 관계 없이)
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
                                                  final TimeUnit unit) {
        return addTask(new FixedRateTrigger(command, initialDelay, period, unit));
    }

    /**
     * (기본 트리거)
     * FixedDelayTrigger 를 이용한 스케줄링 (반복O)
     * 작업이 끝난 이후 delay 시간만큼 기다린 후 시작할 수 있도록 함.
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
                                                     final TimeUnit unit) {
        return addTask(new FixedDelayTrigger(command, initialDelay, delay, unit));
    }

    /**
     * (기본 트리거)
     * OneTimeCallableTrigger : 리턴이 있는 일회성 스케줄링
     */
    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return addTask(new OneTimeCallable<>(callable, delay, unit));
    }

    /*
     * 태스크 추가
     */
    private <V> ScheduledTrigger<V> addTask(final ScheduledTrigger<V> task) {

        synchronized (tasks) {
            // 실행시킨 시간 순서대로 insert 할 위치를 찾는다
            int index = Collections.binarySearch(tasks, task);
            if (index < 0)
                index = -index - 1;
            tasks.add(index, task);
            tasks.notify();
        }
        return task;
    }

    //
    // -- implements for ExecutorService
    //

    @Override
    public void shutdown() {
        shutdownScheduler();
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdownScheduler();
        return executorService.shutdownNow();
    }

    private void shutdownScheduler() {
        synchronized (tasks) {
            state = State.stopping;
            tasks.notify();
        }
    }

    @Override
    public boolean isShutdown() {
        return state != State.running && executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return state == State.stopped && executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        final long start = Clock.systemUTC().millis();

        final long millis = unit.toMillis(timeout);
        scheduler.join(millis);
        if (state != State.stopped)
            return false;

        final long remaining = millis - (Clock.systemUTC().millis() - start);
        if (remaining <= 0)
            return false;

        return executorService.awaitTermination(remaining, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return executorService.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return executorService.submit(task, result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return executorService.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout,
                                         final TimeUnit unit) throws InterruptedException {
        return executorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny(tasks, timeout, unit);
    }

    //
    //  -- implements for Executor
    //

    @Override
    public void execute(final Runnable command) {
        executorService.execute(command);
    }


    //
    // -- 스케줄 트리거
    //     : 스케줄 유형 별 트리거를 준비한다.
    //

    /**
     * <pre>
     * # 기본 트리거
     *
     *  - OneTimeTrigger : 일회성 스케줄
     *
     *  - FixedRateTrigger : 반복성 스케줄 (일정 간격 실행)
     *
     *  - FixedDelayTrigger : 반복성 스케줄 (작업 간 일정 지연으로 실행)
     *
     * # 응용 트리거
     *
     *  - FixedRateWithLimitTrigger : 기한이 있는 반복성 스케줄 (일정간격, 스케줄 보정 기능)
     *
     *  - FixedDelayWithLimitTrigger : 기한이 있는 반복성 스케줄 (일정지연, 스케줄 보정 기능)
     *
     *  - CronWithLimitTrigger : 기한이 있는 Cron expression 을 응용한 반복 트리거
     *
     *  - PollingTrigger : 폴링 태스크를 위한 트리거 (스케줄 보정 기능, 스케줄 시작 및 종료에 대한 기능, 일/주/월 등에 대한 스케줄 계산 포함)
     *
     *  </pre>
     */
    abstract class ScheduledTrigger<V> implements ScheduledFuture<V> {

        protected volatile Future<V> future;
        private volatile boolean cancelled;

        abstract void execute();

        void setFuture(final Future<V> future) {
            synchronized (this) {
                this.future = future;
                notifyAll();
            }
        }

        void clearFuture() {
            future = null;
        }

        @Override
        public int compareTo(final Delayed that) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), that.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            synchronized (this) {
                if (future != null) {
                    return future.cancel(mayInterruptIfRunning);
                }
                cancelled = true;
                notifyAll();
                logger.info("schedule cancel");
                return true;
            }
        }

        @Override
        public boolean isCancelled() {
            synchronized (this) {
                if (future != null)
                    return future.isCancelled();

                return cancelled;
            }
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            try {
                return await(false, 0L);
            } catch (final TimeoutException e) {
                // Should not happen
                throw new RuntimeException(e);
            }
        }

        @Override
        public V get(final long timeout, final TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return await(true, unit.toMillis(timeout));
        }

        private V await(final boolean timed, final long millis)
                throws InterruptedException, ExecutionException, TimeoutException {
            final long expiry = clock.millis() + millis;

            while (true) {
                synchronized (this) {
                    final long remaining = expiry - clock.millis();
                    if (future != null) {
                        if (timed)
                            return future.get(remaining, TimeUnit.MILLISECONDS);
                        return future.get();
                    }
                    if (isCancelled())
                        throw new CancellationException();

                    if (timed) {
                        if (remaining <= 0)
                            throw new TimeoutException();
                        wait(remaining);
                    } else {
                        wait();
                    }
                }
            }
        }

    }

    class OneTimeTrigger extends ScheduledTrigger<Void> {

        private final Runnable command;
        private final long runtime;

        public OneTimeTrigger(final Runnable command, final long delay, final TimeUnit unit) {
            this.command = command;
            runtime = clock.millis() + unit.toMillis(delay);
        }

        @SuppressWarnings("unchecked")
        @Override
        void execute() {
            synchronized (this) {
                setFuture((Future<Void>) executorService.submit(command));
            }
        }

        @Override
        public boolean isDone() {
            synchronized (this) {
                if (future != null)
                    return future.isDone();
                return isCancelled();
            }
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            final long millis = runtime - clock.millis();
            return unit.convert(millis, TimeUnit.MILLISECONDS);
        }

    }

    class OneTimeCallable<V> extends ScheduledTrigger<V> {

        private final Callable<V> command;

        private final long runtime;

        public OneTimeCallable(final Callable<V> command, final long delay, final TimeUnit unit) {
            this.command = command;
            runtime = clock.millis() + unit.toMillis(delay);
        }

        @Override
        void execute() {
            setFuture(executorService.submit(command));
        }

        @Override
        public boolean isDone() {
            synchronized (this) {
                if (future != null)
                    return future.isDone();
                return isCancelled();
            }
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            final long millis = runtime - clock.millis();
            return unit.convert(millis, TimeUnit.MILLISECONDS);
        }

    }

    abstract class RepeatingTrigger extends ScheduledTrigger<Void> {

        private final Runnable command;
        protected final TimeUnit unit;
        protected long nextRuntime;

        public RepeatingTrigger(final Runnable command, final long initialDelay, final TimeUnit unit) {
            this.command = () -> {
                command.run();
                synchronized (this) {
                    if (!isCancelled()) {
                        // Reschedule to run at the period from the last run.
                        updateNextRuntime();
                        clearFuture();
                        addTask(this);
                    }
                }
            };
            nextRuntime = clock.millis() + unit.toMillis(initialDelay);
            this.unit = unit;
        }

        @SuppressWarnings("unchecked")
        @Override
        void execute() {
            synchronized (this) {
                setFuture((Future<Void>) executorService.submit(command));
            }
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            final long millis = nextRuntime - clock.millis();
            return unit.convert(millis, TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean isDone() {
            return isCancelled();
        }

        abstract void updateNextRuntime();

    }

    class FixedRateTrigger extends RepeatingTrigger {

        private final long period;

        public FixedRateTrigger(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
            super(command, initialDelay, unit);
            this.period = period;
        }

        @Override
        void updateNextRuntime() {
            nextRuntime += unit.toMillis(period);
        }

    }

    class FixedDelayTrigger extends RepeatingTrigger {

        private final long delay;

        public FixedDelayTrigger(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
            super(command, initialDelay, unit);
            this.delay = delay;
        }

        @Override
        void updateNextRuntime() {
            nextRuntime = clock.millis() + unit.toMillis(delay);
        }

    }
}