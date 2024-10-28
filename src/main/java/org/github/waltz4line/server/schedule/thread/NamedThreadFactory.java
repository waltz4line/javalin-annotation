package org.github.waltz4line.server.schedule.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {java.util.concurrent.Executors.DefaultThreadFactory}를 기본으로 스레드 풀에서
 * 스레드 생성 시 스레드 풀에 작업을 추적할 수 있도록 해당하는 이름을 지정할 수 있도록 한다.
 *
 * @author song-uiyoung
 */
public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String threadPoolName;
    private final ThreadGroup group;

    public NamedThreadFactory(String threadPoolNamePrefix) {
        this.group = Thread.currentThread().getThreadGroup();
        this.threadPoolName = threadPoolNamePrefix + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {

        Thread t = new Thread(group, r, this.threadPoolName + threadNumber.getAndIncrement(), 0);

        if (t.isDaemon()) {
            t.setDaemon(false);
        }

        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }

        return t;
    }
}