package me.jslim.point.infrastructure.lock;

import me.jslim.point.application.support.UserLockRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Profile("local")
@Component
public class LocalUserLockRunner implements UserLockRunner {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T run(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try { return action.get(); }
        finally { lock.unlock(); }
    }
}
