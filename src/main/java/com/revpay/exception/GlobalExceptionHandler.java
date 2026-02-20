package com.revpay.exception;

import com.revpay.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse("REG_ERR_01", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // --- UPDATED FOR LAKSHMAN'S WALLET MODULE ---

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleWalletRuntime(RuntimeException ex) {
        // Default code
        String code = "WALLET_ERR";

        // Specific checks for Wallet Logic
        if (ex.getMessage().contains("PIN")) {
            code = "AUTH_ERR_02";
        } else if (ex.getMessage().contains("balance")) {
            code = "WALLET_ERR_01";
        } else if (ex.getMessage().contains("limit")) {
            // New check for the Daily Limit feature
            code = "LIMIT_ERR_01";
        }

        ErrorResponse error = new ErrorResponse(code, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Catch-all for other unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse error = new ErrorResponse("SYS_ERR", "An unexpected error occurred");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}