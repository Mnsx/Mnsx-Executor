package excutor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Mnsx_x xx1527030652@gmail.com
 */
public class DefaultThreadFactory implements ThreadFactory {
    // 线程池数量
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    // 线程组
    private final ThreadGroup group;
    // 线程数量
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    // 线程名前缀
    private final String namePrefix;

    DefaultThreadFactory() {
        this.group = Thread.currentThread().getThreadGroup();
        this.namePrefix = "mnsx-" +
                poolNumber.getAndIncrement() +
                "-thread-";
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(group, runnable,
                namePrefix + threadNumber.getAndIncrement(),
                0);

        // 如果线程为守护线程，那么设置为正常线程
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }

        // 如果线程的优先级不为默认优先级，那么将其优先级设置为默认优先级
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }

        return thread;
    }
}
