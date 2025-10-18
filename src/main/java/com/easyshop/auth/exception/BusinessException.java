package com.easyshop.auth.exception;

import lombok.Getter;

import java.text.MessageFormat;


@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] messageArgs;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.messageArgs = null;
    }

    public BusinessException(ErrorCode errorCode, Object... messageArgs) {
        super(formatMessage(errorCode.getMessage(), messageArgs));
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.messageArgs = null;
    }

    private static String formatMessage(String pattern, Object[] args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }
}
