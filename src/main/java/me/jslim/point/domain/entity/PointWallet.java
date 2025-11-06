package me.jslim.point.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jslim.point.global.exception.BusinessException;
import me.jslim.point.global.vo.ResultCode;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point_wallet")
@Comment("포인트 지갑")
public class PointWallet extends BaseEntity {
    @Id
    @Column(name = "wallet_key", unique = true)
    @Comment("포인트 지갑 아이디 (정렬가능한 pk)")
    private String walletKey;

    @Column(name = "user_id", unique = true)
    @Comment("사용자 아이디")
    private String userId;

    @Column(name = "balance_amount")
    @Comment("현재 잔액")
    private long balanceAmount;

    @Column(name = "maximum_amount")
    @Comment("최대 적립 금액")
    private long maximumAmount;

    public static PointWallet create(String walletKey, String userId, long maximumAmount) {
        PointWallet wallet = new PointWallet();
        wallet.walletKey = walletKey;
        wallet.userId = userId;
        wallet.maximumAmount = maximumAmount;
        wallet.balanceAmount = 0;
        return wallet;
    }

    /** 포인트 적립 **/
    public void earnBalance(long point){
        // 적립 최소금액
        if(point < 1){
            throw new BusinessException(ResultCode.WALLET_AMOUNT_ERR);
        }

        // 적립 최대금액 초과
        if(this.balanceAmount + point > this.maximumAmount){
            throw new BusinessException(ResultCode.WALLET_AMOUNT_ERR);
        }

        this.balanceAmount += point;
    }

    /** 포인트 사용 **/
    public void useBalance(long point){
        if(point < 1 || this.balanceAmount - point < 0){
            throw new BusinessException(ResultCode.WALLET_AMOUNT_ERR);
        }

        this.balanceAmount -= point;
    }

    /** 포인트 적립 취소 **/
    public void cancelEarnBalance(long point){
        if(point < 1 || this.balanceAmount - point < 0){
            throw new BusinessException(ResultCode.WALLET_AMOUNT_ERR);
        }

        this.balanceAmount -= point;
    }

    /** 포인트 사용 취소 **/
    public void cancelUseBalance(long point){
        if(point < 1){
            throw new BusinessException(ResultCode.WALLET_AMOUNT_ERR);
        }

        // 적립 최대금액 확인
        if(this.balanceAmount + point > this.maximumAmount){
            throw new BusinessException(ResultCode.WALLET_AMOUNT_ERR);
        }

        this.balanceAmount += point;
    }

}
