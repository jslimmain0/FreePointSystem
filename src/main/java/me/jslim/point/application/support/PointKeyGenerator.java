package me.jslim.point.application.support;

public interface PointKeyGenerator {
    String newWalletKey();
    String newEarnKey()   ;
    String newEarnCancelKey()   ;
    String newPointKey();
    String newUseKey();
    String newUseCancelKey();
}
