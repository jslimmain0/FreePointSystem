package me.jslim.point.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jslim.point.domain.type.EarnCancelType;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point_earn_cancel")
@Comment("포인트 적립 취소")
public class PointEarnCancel extends BaseEntity {
    @Id
    @Column(name = "earn_cancel_key", unique = true)
    @Comment("적립 취소 키")
    private String earnCancelKey;

    @Column(name = "earn_key", unique = true)
    @Comment("적립 키")
    private String earnKey;

    @Column(name = "point_key")
    @Comment("포인트 키")
    private String pointKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "earn_cancel_type")
    @Comment("적립 취소 타입(사용자 적립취소/유효기간 만료 적립취소)")
    private EarnCancelType earnCancelType;

    @Column(name = "earn_cancel_amount")
    @Comment("적립 취소 금액")
    private long earnCancelAmount;

    public PointEarnCancel(String earnCancelKey, String earnKey, String pointKey, EarnCancelType earnCancelType, long earnCancelAmount) {
        this.earnCancelKey = earnCancelKey;
        this.earnKey = earnKey;
        this.pointKey = pointKey;
        this.earnCancelType = earnCancelType;
        this.earnCancelAmount = earnCancelAmount;
    }

    public static PointEarnCancel create(String earnCancelKey, PointEarn pointEarn, String pointKey) {
        return new PointEarnCancel(
                earnCancelKey,
                pointEarn.getEarnKey(),
                pointKey,
                EarnCancelType.EARN_CANCEL_GENERAL,
                pointEarn.getEranAmount()
        );
    }
}
