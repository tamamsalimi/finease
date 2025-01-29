package co.id.finease.service;

import co.id.finease.entity.Account;
import co.id.finease.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Optional<Account> getAccountByRef(String accountRef) {
        return accountRepository.findByAccountRef(accountRef);
    }

    public Account createAccount(Account account) {
        // Check if an account with the same client_id and account_name already exists
        Optional<Account> existingAccount = accountRepository.findByClientIdAndAccountName(account.getClientId(), account.getAccountName());
        if (existingAccount.isPresent()) {
            throw new RuntimeException("Account with the same client_id and account_name already exists");
        }
        return accountRepository.save(account);
    }

    public Account updateAccount(Long accountId, Account updatedAccount) {
        return accountRepository.findById(accountId)
                .map(account -> {
                    account.setAccountName(updatedAccount.getAccountName());
                    account.setStatus(updatedAccount.getStatus());
                    account.setBalance(updatedAccount.getBalance());
                    return accountRepository.save(account);
                })
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public void deleteAccount(Long accountId) {
        accountRepository.deleteById(accountId);
    }
}