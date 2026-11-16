package com.volunteergrid.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for all controllers.
 *
 * Why ControllerAdvice?
 * Instead of duplicating try-catch in every controller, we handle
 * all common exceptions here and return appropriate error views.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model,
                                  HttpServletRequest request) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error/error";
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessRule(BusinessRuleException ex, Model model,
                                      HttpServletRequest request) {
        model.addAttribute("status", 400);
        model.addAttribute("error", "Business Rule Violation");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error/error";
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorized(UnauthorizedActionException ex, Model model,
                                      HttpServletRequest request) {
        model.addAttribute("status", 403);
        model.addAttribute("error", "Forbidden");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error/error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidation(MethodArgumentNotValidException ex, Model model,
                                    HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        model.addAttribute("status", 400);
        model.addAttribute("error", "Validation Error");
        model.addAttribute("message", errors);
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model,
                                 HttpServletRequest request) {
        model.addAttribute("status", 500);
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", "An unexpected error occurred. Please try again.");
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error/error";
    }
}
