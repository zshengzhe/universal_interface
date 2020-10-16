package org.zsz.uniitf.dispatcher.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import org.zsz.uniitf.dispatcher.enumerate.ErrorCode;

@Data
@Accessors(chain = true)
public class Result<T> {

    public static final String SUCCESS = "000000";
    public static final String COMMON_FAILED = "999999";
    public static final String SUCCESS_MESSAGE = "成功";

    private String code;

    private String message;

    private T data;

    public static <T> Result<T> ok() {
        Result<T> r = new Result<>();
        return r.setCode(SUCCESS).setMessage(SUCCESS_MESSAGE);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        return r.setCode(SUCCESS).setMessage(SUCCESS_MESSAGE).setData(data);
    }

    public static <T> Result<T> failed(String message) {
        Result<T> r = new Result<>();
        return r.setCode(COMMON_FAILED).setMessage(message);
    }

    public static <T> Result<T> failed(ErrorCode errorCode) {
        Result<T> r = new Result<>();
        return r.setCode(errorCode.getCode()).setMessage(errorCode.getMessage());
    }

    public static <T> Result<T> failed(String code, String message) {
        Result<T> r = new Result<>();
        return r.setCode(code).setMessage(message);
    }

}
