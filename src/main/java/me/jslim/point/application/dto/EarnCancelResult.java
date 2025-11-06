package me.jslim.point.application.dto;

import me.jslim.point.global.vo.ResultCode;

public record EarnCancelResult(
        String pointKey,
        long balanceAmount,
        ResultCode resultCode
) {
    public static EarnCancelResult success(String pointKey, long balanceAmount) {
        return new EarnCancelResult(pointKey, balanceAmount, ResultCode.SUCCESS);
    }
}
