package me.jslim.point.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jslim.point.domain.type.PointType;
import me.jslim.point.global.exception.BusinessException;
import me.jslim.point.global.vo.ResultCode;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point")
@Comment("포인트 내역")
public class Point extends BaseEntity {
    @Id
    @Column(name = "point_key", unique = true)
    @Comment("포인트 키")
    private String pointKey;

    @Column(name = "wallet_key")
    @Comment("포인트 지갑 키")
    private String walletKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type")
    @Comment("포인트 타입(적립/사용/적립취소/사용취소)")
    private PointType pointType;

    @Column(name = "point_amount")
    @Comment("포인트 금액")
    private long pointAmount;

    public static Point createEarn(String pointKey, String walletKey, long amount, long maxPoint){
        // 포인트 금액 최소값, 최대값 확인
        if(amount > maxPoint || amount < 1){
            throw new BusinessException(ResultCode.POINT_AMOUNT_ERR);
        }

        Point point = new Point();
        point.pointKey = pointKey;
        point.walletKey = walletKey;
        point.pointType = PointType.EARN;
        point.pointAmount = amount;
        return point;
    }

    public static Point createEarnCancel(String pointKey, String walletKey, long amount){
        if(amount < 1){
            throw new BusinessException(ResultCode.POINT_AMOUNT_ERR);
        }

        Point point = new Point();
        point.pointKey = pointKey;
        point.walletKey = walletKey;
        point.pointType = PointType.EARN_CANCEL;
        point.pointAmount = -amount;
        return point;
    }

    public static Point createUse(String pointKey, String walletKey, long amount){
        // 포인트 금액 최소값, 최대값 확인
        if(amount < 1){
            throw new BusinessException(ResultCode.POINT_AMOUNT_ERR);
        }

        Point point = new Point();
        point.pointKey = pointKey;
        point.walletKey = walletKey;
        point.pointType = PointType.USE;
        point.pointAmount = -amount;
        return point;
    }

    public static Point createUseCancel(String pointKey, String walletKey, long amount){
        // 포인트 금액 최소값확인, 사용취소로인한 적립이기 때문에 최대값 확인 X
        if(amount < 1){
            throw new BusinessException(ResultCode.POINT_AMOUNT_ERR);
        }

        Point point = new Point();
        point.pointKey = pointKey;
        point.walletKey = walletKey;
        point.pointType = PointType.USE_CANCEL;
        point.pointAmount = amount;
        return point;
    }
}
