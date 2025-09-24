package pl.cleankod.exchange.entrypoint;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pl.cleankod.exchange.core.usecase.CurrencyConversionException;
import pl.cleankod.exchange.entrypoint.model.ApiError;

@ControllerAdvice
public class ExceptionHandlerAdvice {

    @ExceptionHandler({
            CurrencyConversionException.class,
            IllegalArgumentException.class
    })
    protected ResponseEntity<ApiError> handleBadRequest(CurrencyConversionException ex) {
        return ResponseEntity.badRequest().body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler({
            pl.cleankod.exchange.core.usecase.CircuitOpenException.class
    })
    protected ResponseEntity<ApiError> handleServiceUnavailable(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(ex.getMessage()));
    }
}
