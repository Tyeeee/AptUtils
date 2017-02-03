package com.yjt.apt.router.thread;

import java.util.concurrent.CountDownLatch;

public class CancelableCountDownLatch extends CountDownLatch {
    
    public CancelableCountDownLatch(int count) {
        super(count);
    }

    public void cancel() {
        while (getCount() > 0) {
            countDown();
        }
    }
}
