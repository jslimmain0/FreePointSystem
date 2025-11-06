package me.jslim.point.global.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResultCode {
    SUCCESS("성공"),

    /*포인트 지갑*/
    WALLET_NOT_FOUND("포인트 지갑을 찾을 수 없습니다."),
    WALLET_AMOUNT_ERR("포인트 지갑 잔액 최소값, 최대값 확인 필요"),

    /*포인트*/
    UNKNOW_POINT_KEY("포인트 키에 대한 내용을 찾을 수 없습니다"),
    POINT_AMOUNT_ERR("포인트 금액의 최소값, 최대값 확인 필요")


    /*적립*/,
    EARN_NOT_FOUND("포인트 적립내용을 찾을 수 없습니다."),
    EARN_EXPIRE_DATE_ERROR("포인트 만료일 에러"),
    EARN_MAX_EXPIRE_DATE_ERROR("최대 포인트 만료일 에러"),
    EARN_ALREADY_CANCELED("이미 취소된 적립 입니다."),
    EARN_ALREADY_EXPIRED("이미 만료된 적립 입니다."),
    EARN_USED_ERROR("이미 사용한 포인트는 적립취소할 수 없습니다."),
    EARN_BALANCE_ERROR("포인트 잔액이 0보다 작거나, 원래금액보다 커질 수 없습니다."),

    /*기타*/
    USE_CANCEL_FAIL("취소금액 확인 필요"),
    VALIDATION_ERROR("유효성 검증 에러"),
    SYSTEM_ERROR("시스템에러"),
    ;
    private final String resultMsg;
}
