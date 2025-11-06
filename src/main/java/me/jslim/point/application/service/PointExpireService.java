package me.jslim.point.application.service;

import lombok.RequiredArgsConstructor;
import me.jslim.point.domain.entity.PointEarn;
import me.jslim.point.domain.repository.PointEarnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointExpireService {
    private final PointEarnRepository earnRepo;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void findActiveExpiring(LocalDate date) {
        List<PointEarn> expirePointEarn = earnRepo.findExpiredEarns(date);

        // 포인트 적립금 만료 처리
        for (PointEarn earn : expirePointEarn) {
            earn.expire();
        }
        
        // 저장
        earnRepo.saveAll(expirePointEarn);
    }
}
