package com.easyshop.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendResultDto {

    private long cooldownSeconds;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant cooldownUntil;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String otpStatus;
}

