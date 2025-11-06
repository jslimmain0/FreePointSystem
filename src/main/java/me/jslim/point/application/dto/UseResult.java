package me.jslim.point.application.dto;

import me.jslim.point.global.vo.ResultCode;

public record UseResult(
        String pointKey,
        long balanceAmount,
        ResultCode resultCode
) {
    public static UseResult success(String pointKey, long balanceAmount){
        return new UseResult(pointKey, balanceAmount, ResultCode.SUCCESS);
    }
}
