package pl.cleankod.exchange.provider;

import pl.cleankod.exchange.core.domain.Money;
import pl.cleankod.exchange.core.gateway.CurrencyConversionService;
import pl.cleankod.exchange.provider.nbp.ExchangeRatesNbpClient;
import pl.cleankod.exchange.provider.nbp.model.RateWrapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public class CurrencyConversionNbpService implements CurrencyConversionService {
    private final ExchangeRatesNbpClient exchangeRatesNbpClient;
    private final Map<String, CacheEntry> rateCache = new ConcurrentHashMap<>();
    private final long ttlMillis = 5 * 60 * 1000L; // 5 minutes
    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionNbpService.class);

    // Simple circuit breaker
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openUntilMillis = new AtomicLong(0);
    private final int failureThreshold = 3;
    private final long openStateMillis = 30_000L; // 30s

    public CurrencyConversionNbpService(ExchangeRatesNbpClient exchangeRatesNbpClient) {
        this.exchangeRatesNbpClient = exchangeRatesNbpClient;
    }

    @Override
    public Money convert(Money money, Currency targetCurrency) {
        BigDecimal midRate = getMidRate(targetCurrency);
        BigDecimal calculatedRate = money.amount().divide(midRate, 10, RoundingMode.HALF_UP);
        return Money.of(calculatedRate, targetCurrency);
    }

    private BigDecimal getMidRate(Currency targetCurrency) {
        String code = targetCurrency.getCurrencyCode();
        long now = System.currentTimeMillis();
        long openUntil = openUntilMillis.get();
        if (openUntil > now) {
            throw new IllegalStateException("NBP client circuit open until " + openUntil);
        }
        CacheEntry cached = rateCache.get(code);
        if (cached != null && cached.expiresAtMillis > now) {
            return cached.midRate;
        }

        try {
            RateWrapper rateWrapper = exchangeRatesNbpClient.fetch("A", code);
            List<pl.cleankod.exchange.provider.nbp.model.Rate> rates = rateWrapper.rates();
            if (rates == null || rates.isEmpty()) {
                throw new IllegalStateException("NBP returned no rates for " + code);
            }
            BigDecimal mid = rates.get(0).mid();
            if (mid == null || mid.signum() <= 0) {
                throw new IllegalStateException("NBP returned invalid rate for " + code);
            }

            rateCache.put(code, new CacheEntry(mid, now + ttlMillis));
            consecutiveFailures.set(0);
            return mid;
        } catch (RuntimeException ex) {
            int failures = consecutiveFailures.incrementAndGet();
            log.warn("NBP fetch failed ({}): {}", failures, ex.toString());
            if (failures >= failureThreshold) {
                openUntilMillis.set(now + openStateMillis);
                log.error("Opening circuit for NBP client for {} ms", openStateMillis);
            }
            throw ex;
        }
    }

    private static final class CacheEntry {
        final BigDecimal midRate;
        final long expiresAtMillis;

        CacheEntry(BigDecimal midRate, long expiresAtMillis) {
            this.midRate = midRate;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
