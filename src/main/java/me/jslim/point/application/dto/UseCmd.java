package me.jslim.point.application.dto;

import jakarta.validation.constraints.NotNull;

public record UseCmd(
        @NotNull String userId,
        @NotNull Long useAmount,
        @NotNull String orderNumber
) {
}
