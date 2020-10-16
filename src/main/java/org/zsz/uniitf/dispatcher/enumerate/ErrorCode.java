package org.zsz.uniitf.dispatcher.enumerate;

import lombok.Getter;

@Getter
public enum ErrorCode {
    /**
     * 成功
     */
    SUCCESS("000000", "成功"),
    /**
     * 异常
     */
    COMMON_FAILED("999999", "未知异常")
    ;

    private String code;
    private String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}