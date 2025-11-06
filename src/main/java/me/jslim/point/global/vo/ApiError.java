package me.jslim.point.global.vo;


public record ApiError (String errorCode, String errorMsg) {
    public static ApiError of(ResultCode resultCode) {
        return new ApiError(resultCode.name(), resultCode.getResultMsg());
    }
}
