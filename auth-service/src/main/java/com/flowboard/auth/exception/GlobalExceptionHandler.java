package com.flowboard.auth.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final AuthenticationManager authenticationManager;

    GlobalExceptionHandler(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
	
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<?> handleRuntimeException(RuntimeException ex){
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}
	 @ExceptionHandler(EmailAlreadyExistsException.class)
	    public ResponseEntity<?> handleEmail(EmailAlreadyExistsException ex) {
	        return ResponseEntity
	                .status(HttpStatus.CONFLICT)
	                .body(Map.of("error", ex.getMessage()));
	    }

	    @ExceptionHandler(UsernameAlreadyExistsException.class)
	    public ResponseEntity<?> handleUsername(UsernameAlreadyExistsException ex) {
	        return ResponseEntity
	                .status(HttpStatus.CONFLICT)
	                .body(Map.of("error", ex.getMessage()));
	    }
	    
	    @ExceptionHandler(UserNotFoundException.class)
	    public ResponseEntity<?> handleUser(UserNotFoundException ex){
	    	return ResponseEntity
	    			.status(HttpStatus.CONFLICT)
	    			.body(Map.of("error",ex.getMessage()));
	    }

}