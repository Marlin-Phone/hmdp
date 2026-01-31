package com.hmdp.utils;

public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec 锁的超时时间，过期后自动释放，防止死锁
     * @return true表示获取锁成功，false表示获取锁失败
     */
    boolean tryLock(long timeoutSec);
    /**
     * 释放锁
     */
    void unlock();
}
