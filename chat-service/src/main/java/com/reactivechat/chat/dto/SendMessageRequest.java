package com.reactivechat.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank String roomId,
        @NotBlank String content
) {}
