package com.flowboard.auth.exception;

import org.springframework.http.HttpStatus;


public class CustomException extends RuntimeException{
    private final HttpStatus status;

    public CustomException(String msg, HttpStatus status){
        super(msg);
        this.status = status;
    }

    /**
     * Getter for HTTP status
     *
     * @return HttpStatus associated with this exception
     */
    public HttpStatus getStatus(){
        return status;
    }
}