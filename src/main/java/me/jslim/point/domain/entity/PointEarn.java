package me.jslim.point.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jslim.point.domain.type.EarnStatus;
import me.jslim.point.domain.type.EarnType;
import me.jslim.point.global.exception.BusinessException;
import me.jslim.point.global.vo.ResultCode;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point_earn")
@Comment("포인트 적립")
public class PointEarn extends BaseEntity {
    @Id
    @Column(name = "earn_key", unique = true)
    @Comment("적립 키")
    private String earnKey;

    @Column(name = "wallet_key")
    @Comment("지갑 키")
    private String walletKey; // 사용할 적림금 조회에 사용

    @Column(name = "point_key")
    @Comment("포인트 키")
    private String pointKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "earn_type")
    @Comment("적립 타입(사용자 적립/관리자 적립/사용취소 적립)")
    private EarnType earnType;

    @Enumerated(EnumType.STRING)
    @Column(name = "earn_status")
    @Comment("적립 상태(사용가능/사용불가능/적립취소됨)")
    private EarnStatus earnStatus;
    
    @Column(name = "earn_amount")
    @Comment("적립 금액")
    private long eranAmount;

    @Column(name = "earn_balance_amount")
    @Comment("적립 잔액")
    private long eranBalanceAmount;

    @Column(name = "is_manual")
    @Comment("관리자 수동 적립 여부")
    private boolean isManual;

    @Column(name = "ref_use_cancel_key")
    @Comment("사용 취소 키(사용취소로 인한 적립일 경우)")
    private String refUseCancelKey;

    @Column(name = "earn_date")
    @Comment("적립일")
    private LocalDate earnDate;

    @Column(name = "expire_date")
    @Comment("만료일")
    private LocalDate expireDate;

    /** 포인트 적립 생성 **/
    private PointEarn(String earnKey, EarnType earnType, Point point, boolean isManual, String refUseCancelKey,
                                   LocalDate earnDate, LocalDate expireDate, int maxExpireDays, int defExpireDateDays) {
        // 만료일 미입력 시 기본 만료일 설정
        LocalDate calcExpireDate = expireDate != null
                ? expireDate
                : earnDate.plusDays(defExpireDateDays);

        // 만료일이 현재 이전일 경우 오류
        if(calcExpireDate.isBefore(earnDate)){
            throw new BusinessException(ResultCode.EARN_EXPIRE_DATE_ERROR);
        }

        // 만료일이 최대 만료일을 넘을 경우 오류
        if(calcExpireDate.isAfter(earnDate.plusDays(maxExpireDays))){
            throw new BusinessException(ResultCode.EARN_MAX_EXPIRE_DATE_ERROR);
        }

        this.earnKey = earnKey;
        this.walletKey = point.getWalletKey();
        this.earnType = earnType;
        this.earnStatus = EarnStatus.AVAILABLE;
        this.pointKey = point.getPointKey();
        this.eranAmount = point.getPointAmount();
        this.eranBalanceAmount = point.getPointAmount();
        this.isManual = isManual;
        this.earnDate = earnDate;
        this.refUseCancelKey = refUseCancelKey;
        this.expireDate = calcExpireDate;
    }

    /** 포인트 생성 **/
    public static PointEarn create(String earnKey, Point point, EarnType earnType,
                                   LocalDate earnDate, LocalDate expireDate, int maxExpireDays, int defExpireDateDays){
        return new PointEarn(
                earnKey,
                earnType,
                point,
                earnType == EarnType.EARN_MANUAL,
                null,
                earnDate,
                expireDate,
                maxExpireDays,
                defExpireDateDays
        );
    }

    /** 사용취소로인한 포인트생성 **/
    public static PointEarn createAsUseCancel(String earnKey, Point point, String refUseCancelKey, boolean isManual,
                      LocalDate earnDate, int maxExpireDays, int defExpireDateDays){


        return new PointEarn(
                earnKey,
                EarnType.EARN_AS_USE_CANCEL,
                point,
                isManual, // 사용취소로 인한 포인트 적립이지만, 기존 포인트가 관리자 적립이라면 그대로 처리될 수 있게
                refUseCancelKey,
                earnDate,
                null, // 사용취소로 발생한 포인트이기 때문에, 만료일은 def로 계산하도록 처리
                maxExpireDays,
                defExpireDateDays
        );
    }

    /** 포인트 적립 취소 **/
    public void cancelEarn(){
        if(isUsed()){
            throw new BusinessException(ResultCode.EARN_USED_ERROR);
        }
        if(this.earnStatus == EarnStatus.CANCELED){
            throw new BusinessException(ResultCode.EARN_ALREADY_CANCELED);
        }

        if(this.earnStatus == EarnStatus.EXPIRED){
            throw new BusinessException(ResultCode.EARN_ALREADY_CANCELED);
        }

        this.earnStatus = EarnStatus.CANCELED;
        this.eranBalanceAmount = 0L;
    }

    /** 사용된 포인트인지 확인 **/
    private boolean isUsed(){
        return this.eranAmount != eranBalanceAmount;
    }

    /** 포인트 잔액 변경 및 잔액에 맞게 상태 변경 **/
    public void addBalanceAndStatus(long point){
        // 잔액이 0보다 작거나, 원래금액보다 커질 수 없음
        if(this.eranBalanceAmount < 0 || this.eranBalanceAmount + point > this.eranAmount){
            throw new BusinessException(ResultCode.EARN_BALANCE_ERROR);
        }

        this.eranBalanceAmount += point;
        this.earnStatus = this.eranBalanceAmount == 0 ? EarnStatus.UNAVAILABLE : EarnStatus.AVAILABLE;
    }

    /** 포인트 만료처리 */
    public void expire(){
        this.earnStatus = EarnStatus.EXPIRED;
    }

    /** 포인트 만료 여부 **/
    public boolean isExpired() {
        return this.earnStatus == EarnStatus.EXPIRED;
    }
}
