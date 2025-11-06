package me.jslim.point.infrastructure.keygen;

import com.github.f4b6a3.uuid.UuidCreator;
import me.jslim.point.application.support.PointKeyGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


@Profile("local")
@Component
public class LocalPointKeyGenerator implements PointKeyGenerator {
    private String key() {
        return UuidCreator.getTimeOrderedEpoch().toString().replace("-", "");
    }

    public String newWalletKey() { return key(); }
    public String newEarnKey()     { return key(); }
    public String newEarnCancelKey()     { return key(); }
    public String newPointKey() { return key(); }
    public String newUseKey()  { return key(); }
    public String newUseCancelKey()  { return key(); }
}
