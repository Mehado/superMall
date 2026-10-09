package com.supermall.takeout.concurrency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * 并发测试小工具：让 N 个线程在同一瞬间起跑，收集每个线程的返回值与异常。
 * <p>
 * 用法：{@code ConcurrentRunner.run(100, 30, () -> doSomething())}
 */
public final class ConcurrentRunner {

    private ConcurrentRunner() {
    }

    @FunctionalInterface
    public interface Task<T> {
        T call() throws Exception;
    }

    public record Result<T>(List<T> values, List<Throwable> errors, boolean finished) {

        /** 统计满足条件的返回值个数 */
        public long count(Predicate<T> predicate) {
            return values.stream().filter(predicate).count();
        }
    }

    /**
     * 并发执行 threads 个任务后统一返回。
     *
     * @param threads        线程数
     * @param timeoutSeconds 等待全部完成的超时时间
     * @param task           每个线程执行的任务
     */
    public static <T> Result<T> run(int threads, long timeoutSeconds, Task<T> task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<T> values = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    values.add(task.call());
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(timeoutSeconds, TimeUnit.SECONDS);
        pool.shutdownNow();
        return new Result<>(values, errors, finished);
    }
}
