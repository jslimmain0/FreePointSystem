package me.jslim.point.domain.type;

public enum EarnType {
    EARN_GENERAL, // 일반 적립
    EARN_MANUAL, // 관리자 수동 적립
    EARN_AS_USE_CANCEL, // 사용 취소로 인한 적립
    ;
    public static EarnType fromString(String type) {
        if(type == null || type.isBlank()){
            return EarnType.EARN_GENERAL;
        }

        try {
            return EarnType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EarnType.EARN_GENERAL;
        }
    }
}
