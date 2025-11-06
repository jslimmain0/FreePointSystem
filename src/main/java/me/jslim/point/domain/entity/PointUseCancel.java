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
@Table(name = "point_use_cancel")
@Comment("포인트 사용취소 상세")
public class PointUseCancel extends BaseEntity {
    @Id
    @Column(name = "use_cancel_key", unique = true)
    @Comment("사용 취소 아이디")
    private String usaCancelKey;

    @Column(name = "use_key")
    @Comment("사용 아이디")
    private String useKey;

    @Column(name = "point_key")
    @Comment("포인트 키")
    private String pointKey;

    @Column(name = "amount")
    @Comment("사용 금액")
    private long amount;

    @Column(name = "order_number")
    @Comment("주문번호")
    private String orderNumber;

    public static PointUseCancel create(String usaCancelKey, PointUse pointUse, long cancelAmount, String pointKey) {
        PointUseCancel pointUseCancel = new PointUseCancel();
        pointUseCancel.usaCancelKey = usaCancelKey;
        pointUseCancel.useKey = pointUse.getUseKey();
        pointUseCancel.pointKey = pointKey;
        pointUseCancel.amount = cancelAmount;
        pointUseCancel.orderNumber = pointUse.getOrderNumber();
        return pointUseCancel;
    }

}

