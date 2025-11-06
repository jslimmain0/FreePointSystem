package me.jslim.point.application.support;

public interface PointPolicy {
   int maxExpireDays();
   int defExpireDays();
   long maximumPoint();
   long defWalletMaximumPoint();
   void reload();
   void update(String key, String value);
}
