package me.jslim.point.application.dto;

import me.jslim.point.global.vo.ResultCode;

import java.time.LocalDate;

public record EarnResult(
        String pointKey,
        long balanceAmount,
        LocalDate expireDate,
        ResultCode resultCode
) {
    public static EarnResult success(String pointKey, long balanceAmount, LocalDate expireDate) {
        return new EarnResult(pointKey, balanceAmount, expireDate, ResultCode.SUCCESS);
    }
}
