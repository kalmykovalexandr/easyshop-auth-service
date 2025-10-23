package com.easyshop.auth.web;

import com.easyshop.auth.model.dto.AuthDto;
import com.easyshop.auth.model.dto.OtpSendDto;
import com.easyshop.auth.model.dto.PasswordResetDto;
import com.easyshop.auth.model.dto.VerifyCodeDto;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.Locale;

@ControllerAdvice
public class RequestNormalizationAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // Apply for all request bodies; filter in afterBodyRead by type
        return true;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        // TODO check performance for this block
        if (body instanceof AuthDto dto) {
            // TODO check that both passwords are equals
            dto.setEmail(normalize(dto.getEmail()));
            return dto;
        }
        if (body instanceof OtpSendDto dto) {
            dto.setEmail(normalize(dto.getEmail()));
            return dto;
        }
        if (body instanceof VerifyCodeDto dto) {
            dto.setEmail(normalize(dto.getEmail()));
            return dto;
        }
        if (body instanceof PasswordResetDto dto) {
            // TODO check that both passwords are equals
            dto.setEmail(normalize(dto.getEmail()));
            return dto;
        }
        return body;
    }

    private String normalize(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
