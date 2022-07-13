package com.atguigu.gmall.product.service;

public interface TestService {
    /**
     * 测试锁
     */
    void testLock();

    /**
     * 读锁
     */
    String readLock();

    /**
     * 写锁
     */
    String writeLock();
}
