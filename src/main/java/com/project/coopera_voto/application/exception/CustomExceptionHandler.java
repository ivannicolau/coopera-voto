package com.project.coopera_voto.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.project.coopera_voto.domain.records.ApiResponse;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(GenericNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(GenericNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(GenericAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<?>> handleAlreadyExists(GenericAlreadyExistsException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(GenericOperationException.class)
    public ResponseEntity<ApiResponse<?>> handleOperationError(GenericOperationException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(GenericSendErrorException.class)
    public ResponseEntity<ApiResponse<?>> handleSendError(GenericSendErrorException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
        return buildErrorResponse("Erro de validação", HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUntracked(Exception ex) {
        return buildErrorResponse("Erro interno no servidor", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<?>> buildErrorResponse(String message, HttpStatus status, Object details) {
        ApiResponse<?> response = new ApiResponse<>("error", message, details);
        return new ResponseEntity<>(response, status);
    }

    private ResponseEntity<ApiResponse<?>> buildErrorResponse(String message, HttpStatus status) {
        return buildErrorResponse(message, status, null);
    }
}
