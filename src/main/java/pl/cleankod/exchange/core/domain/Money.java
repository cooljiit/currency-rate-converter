package pl.cleankod.exchange.core.domain;

import pl.cleankod.exchange.core.gateway.CurrencyConversionService;
import pl.cleankod.util.Preconditions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public static Money of(BigDecimal amount, Currency currency) {
        Preconditions.requireNonNull(amount);
        Preconditions.requireNonNull(currency);
        int fractionDigits = currency.getDefaultFractionDigits();
        BigDecimal normalized = fractionDigits >= 0
                ? amount.setScale(fractionDigits, RoundingMode.HALF_UP)
                : amount;
        return new Money(normalized, currency);
    }

    public static Money of(String amount, String currency) {
        Preconditions.requireNonNull(amount);
        Preconditions.requireNonNull(currency);
        Currency cur = Currency.getInstance(currency);
        return of(new BigDecimal(amount), cur);
    }

    public Money convert(CurrencyConversionService currencyConverter, Currency targetCurrency) {
        return currencyConverter.convert(this, targetCurrency);
    }
}
