package edu.eci.arsw.blueprints.controllers;

public record ApiResponseWrapper<T>(
        int code,
        String message,
        T data
) {}
