package com.easyshop.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OtpSendDto {

    @Email
    @NotBlank
    private String email;

    @JsonCreator
    public OtpSendDto(@JsonProperty("email") String email) {
        setEmail(email);
    }

    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
    }
}
