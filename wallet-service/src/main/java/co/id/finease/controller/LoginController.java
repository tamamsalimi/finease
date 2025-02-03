package co.id.finease.controller;

import co.id.finease.dto.*;
import co.id.finease.entity.Account;
import co.id.finease.service.AccountService;
import co.id.finease.service.OwedTransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class LoginController {
    @Autowired
    private AccountService accountService;

    @Autowired
    private OwedTransactionService owedTransactionService;

    @GetMapping("/acc/{accountRef}")
    public ResponseEntity<Account> getAccountByRef(
            @Valid @PathVariable String accountRef) {
        return accountService.getAccountRef(accountRef)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping("/login")
    public ResponseEntity<TransactionResponse> createAccount(
            @Valid @RequestBody AccountRequest accountRequest) {
        var account = accountService.createAccount(accountRequest);
        TransactionResponse response = new TransactionResponse();

        response.setAccountResult(new AccountResult(account.getAccountRef(), account.getBalance(), account.getAccountName()));
        response.setOwedBy(owedTransactionService.getAmountsOwedByMe(account));
        response.setOwedTo(owedTransactionService.getOwedTransactionSummary(account));
        response.setMessage("Login Successfully");
        response.setStatus("Success");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}