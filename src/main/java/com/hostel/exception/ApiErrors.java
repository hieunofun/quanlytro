package com.hostel.exception;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) ResponseEntity<?> tooLarge(Exception e) {
        return ResponseEntity.status(413).body(Map.of("message","Ảnh phải nhỏ hơn hoặc bằng 6 MB"));
    }
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "Request failed" : e.getReason()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("message", "Duplicate record, invalid reference, or conflicting contract"));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, org.springframework.http.converter.HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<?> invalid(Exception e) {
        String message = e instanceof MethodArgumentNotValidException v ? v.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField()+": "+f.getDefaultMessage()).reduce((a,b) -> a+"; "+b).orElse("Invalid request") : "Invalid request values";
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
