package pl.cleankod.exchange.entrypoint;

import pl.cleankod.exchange.core.domain.Account;
import pl.cleankod.exchange.core.usecase.FindAccountAndConvertCurrencyUseCase;
import pl.cleankod.exchange.core.usecase.FindAccountUseCase;

import java.util.Currency;
import java.util.Optional;

class AccountFacade {

    private final FindAccountAndConvertCurrencyUseCase findAccountAndConvertCurrencyUseCase;
    private final FindAccountUseCase findAccountUseCase;

    AccountFacade(FindAccountAndConvertCurrencyUseCase findAccountAndConvertCurrencyUseCase,
                  FindAccountUseCase findAccountUseCase) {
        this.findAccountAndConvertCurrencyUseCase = findAccountAndConvertCurrencyUseCase;
        this.findAccountUseCase = findAccountUseCase;
    }

    Optional<Account> findById(Account.Id id, String currencyOrNull) {
        return Optional.ofNullable(currencyOrNull)
                .map(code -> findAccountAndConvertCurrencyUseCase.execute(id, Currency.getInstance(code)))
                .orElseGet(() -> findAccountUseCase.execute(id));
    }

    Optional<Account> findByNumber(Account.Number number, String currencyOrNull) {
        return Optional.ofNullable(currencyOrNull)
                .map(code -> findAccountAndConvertCurrencyUseCase.execute(number, Currency.getInstance(code)))
                .orElseGet(() -> findAccountUseCase.execute(number));
    }
}


