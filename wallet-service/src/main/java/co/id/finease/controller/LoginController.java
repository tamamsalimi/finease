package co.id.finease.controller;

import co.id.finease.dto.AccountRequest;
import co.id.finease.dto.TransactionResponse;
import co.id.finease.entity.Account;
import co.id.finease.service.AccountService;
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
        TransactionResponse response = accountService.loginTransaction(accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}