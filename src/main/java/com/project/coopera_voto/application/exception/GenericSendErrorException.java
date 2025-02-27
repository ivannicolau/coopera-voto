package com.project.coopera_voto.application.exception;

public class GenericSendErrorException extends RuntimeException{
    public GenericSendErrorException(String exception){
        super(exception);
    }
}
