package com.azhi.controller;

import com.azhi.pojo.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        return Result.error(exception.getMessage());
    }
}
