package co.id.finease.service;

import co.id.finease.dto.AccountRequest;
import co.id.finease.entity.Account;
import co.id.finease.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.Optional;

@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SessionService sessionService;

    private static final String PREFIX = "RFID";
    private static final DecimalFormat FORMATTER = new DecimalFormat("0000000000"); // Ensures 10-digit format


    public Optional<Account> getAccountRef(String accountRef) {
        var sessionId = sessionService.getSessionIdFromSecurityContext();
        return accountRepository.findByAccountRefAndSessionId(accountRef, sessionId);
    }

    public Account createAccount(AccountRequest accountRequest) {
        var sessionId = sessionService.getSessionIdFromSecurityContext();
        Optional<Account> existingAccount = accountRepository.findBySessionIdAndAccountName(sessionId, accountRequest.getAccountName());
        if (existingAccount.isPresent()) {
            return existingAccount.get();
        }
        // Mapping AccountRequest to Account entity
        Account account = new Account();
        account.setAccountRef(generateAccountRefId());
        account.setAccountName(accountRequest.getAccountName());
        account.setSessionId(sessionId);
        account.setStatus('A');
        // Save the account and return it
        return accountRepository.save(account);
    }

    public String generateAccountRefId() {
        String formattedSequence = FORMATTER.format(accountRepository.getNextSequenceValue());
        return PREFIX + formattedSequence;
    }

}