 package org.pygisserver.pytc.thread;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class PyLockProvider {
java.util.concurrent.locks.Lock[] locks;
	
	public PyLockProvider() {
		this(1024);
	}
	
	public PyLockProvider(int initLockum) {
		locks = new java.util.concurrent.locks.Lock[initLockum];
		for (int i = 0; i < initLockum; i++) {
			locks[i] = new ReentrantLock();
		}
	}
	
	public Lock getLock(String lockKey) {
		int idx = getIndex(lockKey);
		return locks[idx];
	}
	
	
	private int getIndex(String lockKey) {
        int idx = Math.abs(DigestUtils.sha1Hex(lockKey).hashCode() % locks.length);
        return idx;
    }
}
