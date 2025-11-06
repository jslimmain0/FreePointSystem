package me.jslim.point.application.dto;

import jakarta.validation.constraints.NotNull;

public record EarnCmd(
        @NotNull String userId,
        @NotNull Long pointAmount,
        String earnType,
        String expireDate
) {
}
