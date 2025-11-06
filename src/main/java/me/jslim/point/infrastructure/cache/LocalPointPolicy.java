package me.jslim.point.infrastructure.cache;

import me.jslim.point.application.support.PointPolicy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Profile("local")
@Component
public class LocalPointPolicy implements PointPolicy {
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    
    public void reload() {
        // LOCAL용으로 메모리에 저장하기때문에 reload로직 없음
    }

    public void update(String key, String value) {
        cache.put(key, value);
    }


    public int maxExpireDays() {
        return getInt("MAX_EXPIRE_DAYS", 365*5);
    }

    public int defExpireDays() {
        return getInt("DEF_EXPIRE_DAYS", 365);
    }

    public long maximumPoint() {
        return getLong("MAXIMUM_POINT", 100_000);
    }

    public long defWalletMaximumPoint() {
        return getLong("DEF_WALLET_MAXIMUM_POINT", 1_000_000);
    }

    private int getInt(String k, int def) {
        try {
            return Integer.parseInt(cache.getOrDefault(k, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }

    private long getLong(String k, long def) {
        try {
            return Long.parseLong(cache.getOrDefault(k, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }
}
