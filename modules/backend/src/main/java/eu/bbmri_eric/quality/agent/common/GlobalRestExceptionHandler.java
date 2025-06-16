package eu.bbmri_eric.quality.agent.common;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalRestExceptionHandler {
    @ExceptionHandler(org.hibernate.StaleStateException.class)
    public ResponseEntity<String> handleStaleState(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Entity not found");
    }
    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<String> handleFailedConversion(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
