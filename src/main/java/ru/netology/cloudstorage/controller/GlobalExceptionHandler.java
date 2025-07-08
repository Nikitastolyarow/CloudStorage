package ru.netology.cloudstorage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import ru.netology.cloudstorage.dto.ErrorResponse;

import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse("Конечная точка не найдена: " + ex.getRequestURL(), 404));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), 400));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex) {
        return ResponseEntity.status(500).body(new ErrorResponse("Ошибка ввода-вывода: " + ex.getMessage(), 500));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(500).body(new ErrorResponse("Внутренняя ошибка сервера:" + ex.getMessage(), 500));
    }
}