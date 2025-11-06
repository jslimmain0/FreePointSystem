package me.jslim.point.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import me.jslim.point.application.service.PointExpireService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class LocalExpireScheduler {
    private final PointExpireService pointExpireService;

    // 매일 00:00
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void run() {
        LocalDate date = LocalDate.now();
        pointExpireService.findActiveExpiring(date);
    }
}
