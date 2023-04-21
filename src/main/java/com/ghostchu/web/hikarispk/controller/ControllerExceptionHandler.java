package com.ghostchu.web.hikarispk.controller;

import com.ghostchu.web.hikarispk.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ControllerExceptionHandler {
    // hook all exceptions
    @ExceptionHandler(Exception.class)
    public String exception(Exception e) {
        if (e instanceof ResourceNotFoundException) return "404 Not Found";
        if (e instanceof IllegalArgumentException ex) return ex.getMessage();
        return "500 Internal Server Error";
    }


}
