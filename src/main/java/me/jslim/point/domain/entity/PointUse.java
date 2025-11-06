package me.jslim.point.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point_use")
@Comment("포인트 사용 상세")
public class PointUse extends BaseEntity {
    @Id
    @Column(name = "use_key", unique = true)
    @Comment("사용 아이디")
    private String useKey;

    @Column(name = "point_key")
    @Comment("포인트 키")
    private String pointKey;

    @Column(name = "earn_key")
    @Comment("적립 키")
    private String earnKey;

    @Column(name = "amount")
    @Comment("사용 금액")
    private long amount;

    @Column(name = "order_number")
    @Comment("주문번호")
    private String orderNumber;

    public static PointUse create(String useKey, String earnKey, String pointKey, long amount, String orderNumber) {
        PointUse pointUse = new PointUse();
        pointUse.useKey = useKey;
        pointUse.earnKey = earnKey;
        pointUse.pointKey = pointKey;
        pointUse.amount = amount;
        pointUse.orderNumber = orderNumber;
        return pointUse;
    }
}

