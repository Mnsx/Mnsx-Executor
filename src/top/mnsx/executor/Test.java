package top.mnsx.executor;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mnsx_x xx1527030652@gmail.com
 */
public class Test {
    public static void main(String[] args) throws RejectTaskException {
       ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 0, new LinkedBlockingDeque<>());
        for (int i = 0; i < 20; i++) {
            int finalI = i;
            executor.execute(new Thread(() -> {
                System.out.println(Thread.currentThread() + " 线程任务 " + finalI + " 执行");
            }, String.valueOf(i)));
        }
        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("已完成任务");
    }
}
