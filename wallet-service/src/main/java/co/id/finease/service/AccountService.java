package co.id.finease.service;

import co.id.finease.dto.*;
import co.id.finease.entity.Account;
import co.id.finease.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private OwedTransactionService owedTransactionService;

    @Autowired
    private TransactionService transactionService;

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

    @Transactional
    public TransactionResponse loginTransaction(AccountRequest accountRequest) {
        var sessionId = sessionService.getSessionIdFromSecurityContext();
        // Retrieve account or create a new one if not found
        Account account = accountRepository.findBySessionIdAndAccountName(sessionId, accountRequest.getAccountName())
                .orElseGet(() -> {
                    Account newAccount = new Account();
                    newAccount.setAccountRef(generateAccountRefId());
                    newAccount.setAccountName(accountRequest.getAccountName());
                    newAccount.setSessionId(sessionId);
                    newAccount.setStatus('A');
                    return accountRepository.save(newAccount);
                });
        // Initialize response
        AccountDTO accountDTO = toAccountDTO(account);
        TransactionResponse response = new TransactionResponse();
        response.setAccountResult(new AccountResult(accountDTO.getAccountRef(), accountDTO.getBalance(), accountDTO.getAccountName()));
        // Fetch owed transactions
        List<OwedTransactionItem> owedList = owedTransactionService.getOwedTransactionSummary(account);
        // Process transactions if balance is positive and there are owed transactions
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0 && !owedList.isEmpty()) {
            var transactionList = transactionService.handleDeposit(
                    transactionService.generateRefId(), account, account.getBalance());
            response = transactionService.convertToDTO(transactionList);
        }
        // Set additional response details
        Optional<Account> optional = accountRepository.findByAccountRefAndSessionId(account.getAccountRef(), account.getSessionId());
        response.setAccountResult(new AccountResult(account.getAccountRef(), optional.map(Account::getBalance).orElse(null), account.getAccountName()));
        response.setOwedTo(owedTransactionService.getOwedTransactionSummary(account));
        response.setOwedBy(owedTransactionService.getAmountsOwedByMe(account));
        response.setMessage("Login Successfully");
        response.setStatus("Success");
        return response;
    }

    public AccountDTO toAccountDTO(Account account) {
        return new AccountDTO(account.getAccountRef(), account.getAccountName(), account.getBalance());
    }

}