package com.project.coopera_voto.domain.records;

public record ApiResponse<T>(
    String status,
    String message,
    T data
) {}
