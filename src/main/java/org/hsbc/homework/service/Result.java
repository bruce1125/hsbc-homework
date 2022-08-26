package org.hsbc.homework.service;

/**
 * service call result
 *
 * @author BruceSu
 */
public class Result<T> {

    /**
     * return code,default is 0
     */
    private int status = 0;
    /**
     * return object
     */
    private T retObj = null;

    private Result() {

    }

    private Result(int status) {
        this.status = status;
    }

    public boolean isOk() {
        return status == 0;
    }

    public boolean isFail() {
        return status != 0;
    }

    public int getStatus() {
        return status;
    }

    public T getRetObj() {
        return retObj;
    }

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        return result;
    }

    public static <T> Result<T> success(T retObj) {
        Result<T> result = new Result<>();
        result.retObj = retObj;
        return result;
    }

    public static <T> Result<T> fail(int status) {
        Result<T> result = new Result<>();
        result.status = status;
        return result;
    }
}
