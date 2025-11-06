package me.jslim.point.application.dto;

import me.jslim.point.global.vo.ResultCode;

public record UseCancelResult(
        String pointKey,
        long balanceAmount,
        ResultCode resultCode
) {
    public static UseCancelResult success(String pointKey, long balanceAmount){
        return new UseCancelResult(pointKey, balanceAmount, ResultCode.SUCCESS);
    }
}
