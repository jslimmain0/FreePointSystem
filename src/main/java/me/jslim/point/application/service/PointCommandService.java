package me.jslim.point.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jslim.point.application.dto.*;
import me.jslim.point.application.support.PointKeyGenerator;
import me.jslim.point.application.support.PointPolicy;
import me.jslim.point.application.support.UserLockRunner;
import me.jslim.point.domain.entity.*;
import me.jslim.point.domain.repository.*;
import me.jslim.point.domain.type.EarnType;
import me.jslim.point.global.exception.BusinessException;
import me.jslim.point.global.vo.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointCommandService {
    private final UserLockRunner userLockRunner;
    private final PointKeyGenerator keyGenerator;
    private final PointWalletRepository walletRepo;
    private final PointRepository pointRepo;
    private final PointEarnRepository earnRepo;
    private final PointEarnCancelRepository earnCancelRepo;
    private final PointUseRepository useRepo;
    private final PointUseCancelRepository useCancelRepo;
    private final PointPolicy policy;

    @Transactional
    public EarnResult earn(EarnCmd earnCmd, LocalDate earnDate){
        // USER LOCK을 통해 트렌젝션중에 다른 요청을 받아 꼬이는 일이 없게 처리
        return userLockRunner.run(earnCmd.userId(), () -> {
            // 1. 포인트 지갑 확인, 적립
            PointWallet wallet = walletRepo.findByUserId(earnCmd.userId())
                    .orElseGet(() -> {
                        String walletId = keyGenerator.newWalletKey();
                        PointWallet newWallet = PointWallet.create(walletId, earnCmd.userId(), policy.defWalletMaximumPoint());
                        return walletRepo.save(newWallet);
                    });

            wallet.earnBalance(earnCmd.pointAmount());
            walletRepo.save(wallet);


            // 2. 포인트 적립 등록
            Point point = pointRepo.save(
                    Point.createEarn(keyGenerator.newPointKey(), wallet.getWalletKey(), earnCmd.pointAmount(), policy.maximumPoint())
            );

            // 3. 적립내역 상세 등록
            EarnType earnType = EarnType.fromString(earnCmd.earnType());

            LocalDate expireDate = earnCmd.expireDate() == null
                            ? null
                            : LocalDate.parse(earnCmd.expireDate(), DateTimeFormatter.BASIC_ISO_DATE);
            PointEarn pointEarn = earnRepo.save(
                    PointEarn.create(
                            keyGenerator.newEarnKey(),
                            point,
                            earnType,
                            earnDate,
                            expireDate,
                            policy.maxExpireDays(),
                            policy.defExpireDays()
                    )
            );

            return EarnResult.success(pointEarn.getPointKey(), wallet.getBalanceAmount(), pointEarn.getExpireDate());
        });
    }

     /** 포인트 적립 취소 **/
    @Transactional
    public EarnCancelResult cancelEarn(EarnCancelCmd earnCancelCmd) {
        return userLockRunner.run(earnCancelCmd.userId(), () -> {
            // 1. 포인트 지갑 확인
            PointWallet wallet = walletRepo.findByUserId(earnCancelCmd.userId())
                    .orElseThrow(() -> new BusinessException(ResultCode.WALLET_NOT_FOUND));

            // 2. 적립건 찾기
            PointEarn pointEarn = earnRepo.findByPointKey(earnCancelCmd.pointKey())
                    .orElseThrow(() -> new BusinessException(ResultCode.UNKNOW_POINT_KEY));

            // 3. 적립취소
            pointEarn.cancelEarn();
            earnRepo.save(pointEarn);

            // 4. 적립취소금액 반영
            wallet.cancelEarnBalance(pointEarn.getEranAmount());
            walletRepo.save(wallet);

            // 5. 적립취소 포인트 생성
            Point point = pointRepo.save(
                    Point.createEarnCancel(keyGenerator.newPointKey(), wallet.getWalletKey(), pointEarn.getEranAmount())
            );

            // 6. 적립취소 상세 등록
            earnCancelRepo.save(
                    PointEarnCancel.create(keyGenerator.newEarnCancelKey(), pointEarn, point.getPointKey())
            );

            return EarnCancelResult.success(point.getPointKey(), wallet.getBalanceAmount());
        });
    }

    /** 포인트 사용 **/
    @Transactional
    public UseResult use(UseCmd useCmd, LocalDate useDate){
        // USER LOCK을 통해 트렌젝션중에 다른 요청을 받아 꼬이는 일이 없게 처리
        return userLockRunner.run(useCmd.userId(), () -> {
            // 1. 포인트 지갑 확인
            PointWallet wallet = walletRepo.findByUserId(useCmd.userId())
                    .orElseThrow(() -> new BusinessException(ResultCode.WALLET_NOT_FOUND));

            // 2. 사용 금액 반영
            wallet.useBalance(useCmd.useAmount());
            walletRepo.save(wallet);

            // 3. 사용
            Point point = pointRepo.save(
                    Point.createUse(keyGenerator.newPointKey(), wallet.getWalletKey(), useCmd.useAmount())
            );

            // 4. 사용 상세 등록
            long remaining = useCmd.useAmount();
            int pageSize = 50;
            Boolean lastManual = null;
            LocalDate lastExpire = null;
            String lastEarnKey = null;

            List<PointUse> useBatch = new ArrayList<>();
            List<PointEarn> updatedEarns = new ArrayList<>();

            // 금액만큼 적립 금액 차감
            while (remaining > 0) {
                List<PointEarn> earns = earnRepo.findAvailableEarns(
                        wallet.getWalletKey(), useDate,
                        lastManual, lastExpire, lastEarnKey, pageSize);

                if (earns.isEmpty()) break;

                for (PointEarn earn : earns) {
                    long usable = Math.min(earn.getEranBalanceAmount(), remaining);
                    if (usable <= 0) continue;

                    // 금액만큼 사용
                    earn.addBalanceAndStatus(-usable);
                    updatedEarns.add(earn);

                    // 사용상세 추가
                    useBatch.add(
                            PointUse.create(
                                    keyGenerator.newUseKey(),
                                    earn.getEarnKey(),
                                    point.getPointKey(),
                                    usable,
                                    useCmd.orderNumber()
                            )
                    );

                    remaining -= usable;
                    if (remaining == 0) break;
                }

                if (earns.size() < pageSize) {
                    // 마지막 조회에서도 금액 차감이 안된다면 시스템 에러
                    if (remaining > 0) {
                        throw new BusinessException(ResultCode.SYSTEM_ERROR);
                    }
                    break;
                }

                // 다음 커서에 사용될, 마지막 row 데이터 set
                PointEarn last = earns.getLast();
                lastManual = last.isManual();
                lastExpire = last.getExpireDate();
                lastEarnKey = last.getEarnKey();
            }

            // 5. 저장
            useRepo.saveAll(useBatch);
            earnRepo.saveAll(updatedEarns);

            return UseResult.success(point.getPointKey(), wallet.getBalanceAmount());
        });
    }


    /** 포인트 사용 취소 **/

    @Transactional
    public UseCancelResult cancelUse(UseCancelCmd useCancelCmd) {
        // USER LOCK을 통해 트렌젝션중에 다른 요청을 받아 꼬이는 일이 없게 처리
        return userLockRunner.run(useCancelCmd.userId(), () -> {
            // 1. 포인트 지갑 확인
            PointWallet wallet = walletRepo.findByUserId(useCancelCmd.userId())
                    .orElseThrow(() -> new BusinessException(ResultCode.WALLET_NOT_FOUND));

            // 2. 사용 금액 반영
            wallet.cancelUseBalance(useCancelCmd.useCancelAmount());
            walletRepo.save(wallet);

            // 3. 벌크 저장을 위한 List
            List<Point> points = new ArrayList<>();
            List<PointEarn> earns = new ArrayList<>();
            List<PointUseCancel> useCancels = new ArrayList<>();

            // 4. 사용 취소
            Point point = Point.createUseCancel(keyGenerator.newPointKey(), wallet.getWalletKey(), useCancelCmd.useCancelAmount());
            points.add(point);

            // 5. 사용 취소 상세 등록
            Point usePointKey = pointRepo.findByPointKey(useCancelCmd.pointKey())
                    .orElseThrow(() -> new BusinessException(ResultCode.UNKNOW_POINT_KEY));

            long alreadyCancelAmt = useCancelRepo.findAllByUsePointKey(usePointKey.getPointKey())
                    .stream()
                    .mapToLong(PointUseCancel::getAmount)
                    .sum();

            if(alreadyCancelAmt + useCancelCmd.useCancelAmount() + usePointKey.getPointAmount() > 0){
                throw new BusinessException(ResultCode.USE_CANCEL_FAIL);
            }

            long remaining = useCancelCmd.useCancelAmount();
            for (PointUse pointUse : useRepo.findAllByPointKey(usePointKey.getPointKey())) {
                long cancelable = Math.min(pointUse.getAmount(), remaining);
                PointEarn earn = earnRepo.findById(pointUse.getEarnKey())
                        .orElseThrow(() -> new BusinessException(ResultCode.EARN_NOT_FOUND));
                // 취소상세 생성
                PointUseCancel useCancel = PointUseCancel.create(keyGenerator.newUseCancelKey(), pointUse, cancelable, point.getPointKey());
                useCancels.add(useCancel);

                if (!earn.isExpired()) {
                    // 만료전이라면 사용 취소
                    earn.addBalanceAndStatus(cancelable);
                    earns.add(earn);
                } else {
                    // 만료되었다면, 새로운 적립으로 생성
                    Point newPoint = Point.createEarn(keyGenerator.newPointKey(), wallet.getWalletKey(), cancelable, policy.maximumPoint());
                    PointEarn newEarn = PointEarn.createAsUseCancel(
                            keyGenerator.newEarnKey(),
                            newPoint,
                            useCancel.getUsaCancelKey(),
                            earn.isManual(),
                            earn.getEarnDate(),
                            policy.maxExpireDays(),
                            policy.defExpireDays()
                    );
                    points.add(newPoint);
                    earns.add(newEarn);
                }

                remaining-=cancelable;
                if(remaining == 0) break;
            }

            // 취소된 금액이 요청금액과 다르다면 시스템 에러
            if(remaining != 0){
                throw new BusinessException(ResultCode.SYSTEM_ERROR);
            }

            // 한번에 저장
            pointRepo.saveAll(points);
            useCancelRepo.saveAll(useCancels);
            earnRepo.saveAll(earns);

            return UseCancelResult.success(point.getPointKey(), wallet.getBalanceAmount());
        });
    }
}
