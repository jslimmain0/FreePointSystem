package me.jslim.point.application.support;

import java.util.function.Supplier;

public interface UserLockRunner {
    <T> T run(String userId, Supplier<T> action);
}
