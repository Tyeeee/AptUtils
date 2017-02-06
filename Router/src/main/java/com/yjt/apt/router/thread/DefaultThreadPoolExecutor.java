package com.yjt.apt.router.thread;


import com.yjt.apt.router.constant.Constant;
import com.yjt.apt.router.utils.DebugUtil;
import com.yjt.apt.router.utils.StringUtil;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DefaultThreadPoolExecutor extends ThreadPoolExecutor {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int INIT_THREAD_COUNT = CPU_COUNT + 1;
    private static final int MAX_THREAD_COUNT = 20;
    private static final long SURPLUS_THREAD_LIFE = 30L;

    private static DefaultThreadPoolExecutor instance;

    public static DefaultThreadPoolExecutor getInstance() {
        if (instance == null) {
            synchronized (DefaultThreadPoolExecutor.class){
                if (instance == null) {
                    instance = new DefaultThreadPoolExecutor(
                            INIT_THREAD_COUNT,
                            MAX_THREAD_COUNT,
                            SURPLUS_THREAD_LIFE,
                            TimeUnit.SECONDS,
                            new SynchronousQueue<Runnable>(),
                            new DefaultThreadFactory());
                }
            }
        }
        return instance;
    }

    private DefaultThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        if (throwable == null && runnable instanceof Future<?>) {
            try {
                ((Future<?>) runnable).get();
            } catch (CancellationException ce) {
                throwable = ce;
            } catch (ExecutionException ee) {
                throwable = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        if (throwable != null) {
            DebugUtil.getInstance().warning(Constant.TAG, "Running task appeared exception! Thread [" + Thread.currentThread().getName() + "], because [" + throwable.getMessage() + "]\n" + StringUtil.getInstance().formatStackTrace(throwable.getStackTrace()));
        }
    }
}
