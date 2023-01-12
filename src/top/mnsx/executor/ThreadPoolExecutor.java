package top.mnsx.executor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author Mnsx_x xx1527030652@gmail.com
 */
public class ThreadPoolExecutor {
    // 核心线程数
    private int corePoolSize;
    // 最大线程数
    private int maxPoolSize;
    // 保持连接的时间
    private long keepAliveTime;
    // 当前线程池状态
    private AtomicInteger state = new AtomicInteger();
    // 工作线程数量
    private AtomicInteger workerCount = new AtomicInteger();
    // 已经完成的任务数量
    private int finishedTaskCount = 0;
    // 当前线程池的中状态
    private final static int RUNNING = 0;
    private final static int STOPPED = 1;
    // 任务队列
    private BlockingQueue<Runnable> taskQueue;
    // 工作线程集
    private HashSet<Worker> workers = new HashSet<>();
    // 全局锁
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 构造器方法
     * @param corePoolSize 核心线程数
     * @param maxPoolSize 最大线程数
     * @param keepAliveTime 连接保持时间
     * @param taskQueue 任务队列
     */
    public ThreadPoolExecutor(int corePoolSize, int maxPoolSize, int keepAliveTime, BlockingQueue<Runnable> taskQueue) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.taskQueue = taskQueue;
    }

    /**
     *
     * @return
     */
    public int getFinishedTaskCount() {
        lock.lock();
        try {
            int alreadyFinishCount =  finishedTaskCount;
            for (Worker worker : workers) {
                alreadyFinishCount += worker.finishedTask;
            }
            return alreadyFinishCount;
        } finally {
            lock.unlock();
        }
    }

    public void execute(Runnable task) throws RejectTaskException {
        // 判断任务不为空
        if (task == null) {
            throw new NullPointerException("The task can not be null");
        }

        // 检查线程池状态是否为Running
        if (state.get() == RUNNING) {
            // 接收任务
            // 直接创建一个Worker，当前的task直接作为Worker的firstWork
            if (workerCount.get() < corePoolSize) {
                if (addWorker(task, true)) {
                    return;
                }
            }
            // Worker数量已经达到基本线程数，不再创建线程，将task放入任务队列中
            lock.lock();
            try {
                if (state.get() == RUNNING && taskQueue.offer(task)) {
                    if (state.get() != RUNNING && taskQueue.remove(task)) {
                        throw new RejectTaskException("Thread pool has stopped, Refuse to add task");
                    }
                    return;
                }
            } finally {
                lock.unlock();
            }
            // Worker数量未达到最大线程数，那么直接创建一个线程来执行
            if (!addWorker(task, false)) {
                // Worker数量到达最大线程数，抛出异常
                throw new RejectTaskException("Thread pool has stopped, Refuse to add task");
            }
        } else {
            // 当前线程池已经处于停止状态，拒绝任务
            throw new RejectTaskException("Thread pool has stopped, Refuse to add task");
        }
    }

    private boolean addWorker(Runnable firstTask, boolean ifCore) {
        if (state.get() == STOPPED) {
            return false;
        }

        // RUNNING
        out:
        while (true) {
            if (state.get() == STOPPED) {
                return false;
            }
            while (true) {
                if (workerCount.get() > (ifCore ? corePoolSize : maxPoolSize)) {
                    // 线程池不再允许创建新的Worker
                    return false;
                }
                if (!casIncreaseWorkerCount()) {
                    continue out;
                }
                break out;
            }
        }
        // 实际添加Worker
        Worker worker;
        lock.lock();
        try {
            if (state.get() == STOPPED) {
                return false;
            }
            worker = new Worker(firstTask);
            // 拿到worker的工作线程
            Thread wt = worker.thread;
            if (wt != null) {
                if (wt.isAlive()) {
                    throw new IllegalStateException();
                }
                wt.start();
                workers.add(worker);
            }
        } finally {
            lock.unlock();
        }
        return true;
    }

    private boolean casIncreaseWorkerCount() {
        return workerCount.compareAndSet(workerCount.get(), workerCount.get() + 1);
    }

    public Runnable getTask() {
        if (state.get() == STOPPED) {
            return null;
        }

        Runnable task = null;
        while (true) {
            try {
                if (state.get() == STOPPED) {
                    return null;
                }

                if (workerCount.get() <= corePoolSize) {
                    // 核心线程
                    task = taskQueue.take();
                } else {
                    // 非核心线程
                    task = taskQueue.poll(keepAliveTime, TimeUnit.MILLISECONDS);
                }
                if (task != null) {
                    return task;
                }
            } catch (InterruptedException e) {
                return null;
            }
        }
    }

    public void stop() {
        lock.lock();
        try {
            setState(STOPPED);
            interruptAllWorkers();
        } finally {
            lock.unlock();
        }
    }

    private void setState(int targetState) {
        while (true) {
            if (state.get() == targetState) {
                break;
            }
            if (state.compareAndSet(state.get(), targetState)) {
                break;
            }
        }
    }

    private void interruptAllWorkers() {
        lock.lock();
        try {
            for (Worker worker : workers) {
                if (!worker.thread.isInterrupted()) {
                    worker.thread.interrupt();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public List<Runnable> stopNow() {
        List<Runnable> remains = new ArrayList<>();
        lock.lock();
        try {
            if (!taskQueue.isEmpty()) {
                taskQueue.drainTo(remains);
            }
            while (!taskQueue.isEmpty()) {
                remains.add(taskQueue.poll());
            }
        } finally {
            lock.unlock();
        }
        return remains;
    }

    private void runWorker(Worker worker) {
        if (worker == null) {
            throw new NullPointerException();
        }

        Thread wt = worker.thread;
        Runnable task = worker.firstTask;
        worker.firstTask = null;
        try {
            while (task != null || (task = getTask()) != null) {
               if (wt.isInterrupted()) {
                   System.out.println("This task is interrupted");
                   return;
               }
               if (state.get() == STOPPED) {
                   System.out.println("The thread pool has already stopped");
                   return;
               }
               task.run();
               task = null;
               worker.finishedTask++;
            }
        } finally {
            // worker正常退出逻辑
            lock.lock();
            try {
                workers.remove(worker);
                if (casDecreaseWorkerCount()) {
                   finishedTaskCount += worker.finishedTask;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private boolean casDecreaseWorkerCount() {
        return workerCount.compareAndSet(workerCount.get(), workerCount.get() - 1);
    }

    private final class Worker implements Runnable {

        Runnable firstTask;

        Thread thread;

        int finishedTask = 0;

        Worker(Runnable firstTask) {
            this.firstTask = firstTask;
            this.thread = new DefaultThreadFactory().newThread(this);
        }

        @Override
        public void run() {
            runWorker(this);
        }
    }
}