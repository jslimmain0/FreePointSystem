package me.jslim.point.application.dto;

import jakarta.validation.constraints.NotNull;

public record EarnCancelCmd(
        @NotNull String userId,
        @NotNull String pointKey
) {
}
