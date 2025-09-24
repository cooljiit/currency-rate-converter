package pl.cleankod.exchange.core.usecase;

public class CircuitOpenException extends RuntimeException {
    public CircuitOpenException(String message) {
        super(message);
    }
}
