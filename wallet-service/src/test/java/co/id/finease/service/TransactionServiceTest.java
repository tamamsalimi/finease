package co.id.finease.service;

import co.id.finease.dto.TransactionResponse;
import co.id.finease.entity.Account;
import co.id.finease.entity.OwedTransaction;
import co.id.finease.repository.AccountRepository;
import co.id.finease.repository.OwedTransactionRepository;
import co.id.finease.repository.TransactionRepository;
import co.id.finease.utils.Constants;
import co.id.finease.utils.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    private TransactionService transactionService;
    private SessionService sessionService;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private OwedTransactionRepository owedTransactionRepository;
    private OwedTransactionService owedTransactionService;

    @BeforeEach
    void setUp() {
        sessionService = mock(SessionService.class);
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        owedTransactionRepository = mock(OwedTransactionRepository.class);
        owedTransactionService = mock(OwedTransactionService.class);

        transactionService = new TransactionService(
                sessionService,
                accountRepository,
                transactionRepository,
                owedTransactionService,
                owedTransactionRepository
        );
    }

    // Deposit Tests
    @Test
    void testDepositWithoutOwedTransactions() {
        Account account = new Account();
        account.setBalance(new BigDecimal("500"));

        when(sessionService.getAccountIdFromSecurityContext()).thenReturn(account);
        when(owedTransactionRepository.findByPayFromAndStatusIn(eq(account), anyList(), any())).thenReturn(List.of());
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findByAccountRefAndSessionId(account.getAccountRef(), account.getSessionId())).thenReturn(Optional.of(account));
        TransactionResponse response = transactionService.processTransaction(
                "REF123", null, new BigDecimal("300"), TransactionType.DEPOSIT);

        assertNotNull(response);
        assertEquals(new BigDecimal("800"), account.getBalance());
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void testDepositFullyPaysOwedTransactions() {
        // Create sender account with zero balance
        Account account = new Account();
        account.setBalance(BigDecimal.ZERO);

        // Create recipient account
        Account recipient = new Account();

        // Create an owed transaction where the sender owes 300
        OwedTransaction owedTransaction = new OwedTransaction();
        owedTransaction.setAmount(new BigDecimal("300"));
        owedTransaction.setStatus(Constants.STATUS_UNPAID);
        owedTransaction.setRecipient(recipient);

        // Simulate updated account after deposit
        Account updatedAccount = new Account();
        updatedAccount.setBalance(BigDecimal.ZERO); // Expected balance after fully paying debt

        // Mock session and repository behavior
        when(sessionService.getAccountIdFromSecurityContext()).thenReturn(account);
        when(owedTransactionRepository.findByPayFromAndStatusIn(eq(account), anyList(), any()))
                .thenReturn(List.of(owedTransaction));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findByAccountRefAndSessionId(account.getAccountRef(), account.getSessionId()))
                .thenReturn(Optional.of(updatedAccount));

        // Process the deposit
        TransactionResponse response = transactionService.processTransaction(
                "REF123", null, new BigDecimal("300"), TransactionType.DEPOSIT);

        // Assertions
        assertNotNull(response);
        assertEquals(Constants.STATUS_PAID, owedTransaction.getStatus());
        assertEquals(new BigDecimal("300"), owedTransaction.getAmount()); // Corrected assertion
        assertEquals(BigDecimal.ZERO, updatedAccount.getBalance()); // Ensure account balance is 0

    }

    // Transfer Tests
    @Test
    void testTransferWithoutOwedTransactions() {
        Account sender = new Account();
        sender.setBalance(new BigDecimal("500"));

        Account recipient = new Account();

        recipient.setBalance(new BigDecimal("100"));

        Account updatedAccount = new Account();
        updatedAccount.setBalance(sender.getBalance().subtract(recipient.getBalance()));

        when(sessionService.getAccountIdFromSecurityContext()).thenReturn(sender);
        when(accountRepository.findActiveAccountNameWithLockAndSessionId(eq("Recipient"), any()))
                .thenReturn(Optional.of(recipient));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findByAccountRefAndSessionId(sender.getAccountRef(), sender.getSessionId())).thenReturn(Optional.of(updatedAccount));
        TransactionResponse response = transactionService.processTransaction(
                "REF123", "Recipient", new BigDecimal("300"), TransactionType.TRANSFER);

        assertNotNull(response);
        assertEquals(new BigDecimal("200"), sender.getBalance());
        assertEquals(new BigDecimal("400"), recipient.getBalance());
        verify(accountRepository, times(1)).save(sender);
        verify(accountRepository, times(1)).save(recipient);
    }

    @Test
    void testTransferToNonexistentRecipient() {
        Account sender = new Account();
        sender.setBalance(new BigDecimal("500"));

        when(sessionService.getAccountIdFromSecurityContext()).thenReturn(sender);
        when(accountRepository.findActiveAccountNameWithLockAndSessionId(eq("NonexistentRecipient"), any()))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                transactionService.processTransaction("REF123", "NonexistentRecipient", new BigDecimal("500"), TransactionType.TRANSFER));

        assertEquals("Recipient account is inactive or not found", exception.getMessage());
    }

    // Withdraw Tests
    @Test
    void testWithdrawWithSufficientBalance() {
        Account account = new Account();
        account.setBalance(new BigDecimal("1000"));

        when(sessionService.getAccountIdFromSecurityContext()).thenReturn(account);
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findByAccountRefAndSessionId(account.getAccountRef(), account.getSessionId())).thenReturn(Optional.of(account));
        TransactionResponse response = transactionService.processTransaction(
                "REF123", null, new BigDecimal("500"), TransactionType.WITHDRAW);

        assertNotNull(response);
        assertEquals(new BigDecimal("500"), account.getBalance());
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void testWithdrawWithInsufficientBalance() {
        Account account = new Account();
        account.setBalance(new BigDecimal("300"));

        when(sessionService.getAccountIdFromSecurityContext()).thenReturn(account);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                transactionService.processTransaction("REF123", null, new BigDecimal("500"), TransactionType.WITHDRAW));

        assertEquals("Insufficient funds for withdraw transaction", exception.getMessage());
    }

    // Incorrect Transaction Type Test
    @Test
    void testIncorrectTransactionType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                transactionService.processTransaction("REF123", null, new BigDecimal("500"), null));

        assertEquals("Reference ID, amount, and transaction type cannot be null", exception.getMessage());
    }
}
