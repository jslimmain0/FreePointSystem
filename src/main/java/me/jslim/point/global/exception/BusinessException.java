package me.jslim.point.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.jslim.point.global.vo.ResultCode;

@AllArgsConstructor
@Getter
public class BusinessException extends RuntimeException {
    private final ResultCode resultCode;
}
