package co.id.finease.service;

import co.id.finease.dto.AccountResult;
import co.id.finease.dto.TransactionResponse;
import co.id.finease.dto.TransactionResult;
import co.id.finease.entity.Account;
import co.id.finease.entity.OwedTransaction;
import co.id.finease.entity.Transaction;
import co.id.finease.repository.AccountRepository;
import co.id.finease.repository.OwedTransactionRepository;
import co.id.finease.repository.TransactionRepository;
import co.id.finease.utils.Constants;
import co.id.finease.utils.TransactionType;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TransactionService {


    private final SessionService sessionService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private final OwedTransactionRepository owedTransactionRepository;

    private final OwedTransactionService owedTransactionService;

    public TransactionService(SessionService sessionService, AccountRepository accountRepository, TransactionRepository transactionRepository, OwedTransactionService owedTransactionService, OwedTransactionRepository owedTransactionRepository) {
        this.sessionService = sessionService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.owedTransactionRepository = owedTransactionRepository;
        this.owedTransactionService = owedTransactionService;
    }

    @Transactional
    public TransactionResponse processTransaction(String referenceId, String recipientId, BigDecimal amount, TransactionType transactionType) {
        if (referenceId == null || amount == null || transactionType == null) {
            throw new IllegalArgumentException("Reference ID, amount, and transaction type cannot be null");
        }
        TransactionResponse response;
        Account account = sessionService.getAccountIdFromSecurityContext();
        List<Transaction> transactions = switch (transactionType) {
            case WITHDRAW -> handleWithdraw(referenceId, account, amount);
            case DEPOSIT -> handleDeposit(referenceId, account, amount);
            case TRANSFER -> handleTransfer(referenceId, account, recipientId, amount);
        };

        response = convertToDTO(transactions);
        Optional<Account> optional = accountRepository.findByAccountRefAndSessionId(account.getAccountRef(), account.getSessionId());
        response.setAccountResult(new AccountResult(account.getAccountRef(), optional.map(Account::getBalance).orElse(null), account.getAccountName()));
        response.setOwedBy(owedTransactionService.getAmountsOwedByMe(account));
        response.setOwedTo(owedTransactionService.getOwedTransactionSummary(account));
        return response;
    }

    private List<Transaction> handleWithdraw(String referenceId, Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds for withdraw transaction");
        }
        Transaction transaction = createNewTransaction(referenceId,
                generateRefTransactionId(),
                account,
                null,
                amount,
                TransactionType.WITHDRAW, account.getBalance().subtract(amount));
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        return List.of(transaction);
    }

    public List<Transaction> handleDeposit(String referenceId, Account account, BigDecimal depositAmount) {
        Sort sort = Sort.by(Sort.Order.asc("createdAt")); // Process oldest owed transactions first
        List<String> owedStatuses = Arrays.asList(Constants.STATUS_UNPAID, Constants.STATUS_PARTIALLY_PAID);
        List<OwedTransaction> owedTransactions = owedTransactionRepository.findByPayFromAndStatusIn(account, owedStatuses, sort);

        String transactionId = generateRefTransactionId();
        List<Transaction> transactions = new ArrayList<>();

        // Calculate total available balance (either deposit or existing balance)
        BigDecimal availableAmount = (depositAmount != null) ? account.getBalance().add(depositAmount) : account.getBalance();

        if (!owedTransactions.isEmpty()) {
            for (OwedTransaction owedTransaction : owedTransactions) {
                if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) break; // Stop if no funds left

                BigDecimal owedAmount = owedTransaction.getAmount();
                BigDecimal remainingAfterPayment = availableAmount.subtract(owedAmount);
                Account recipient = owedTransaction.getRecipient();

                if (remainingAfterPayment.compareTo(BigDecimal.ZERO) >= 0) {
                    // Fully pay off the owed transaction
                    processFullOwedPayment(referenceId, transactionId, account, recipient, owedTransaction, owedAmount, transactions);
                    availableAmount = remainingAfterPayment;
                } else {
                    // Partially pay and create a new owed transaction for the remaining amount
                    processPartialOwedPayment(referenceId, transactionId, account, recipient, owedTransaction, availableAmount, transactions);
                    availableAmount = BigDecimal.ZERO;
                    break; // Stop further processing since funds are exhausted
                }
            }
        }

        // If login or deposit leaves extra balance, update it
        account.setBalance(availableAmount);

        // If it's a deposit (not login), create a deposit transaction
        if (depositAmount != null) {
            processDepositTransaction(referenceId, transactionId, account, depositAmount, transactions);
        }
        return transactions;
    }

    private void processDepositTransaction(
            String referenceId, String transactionId, Account account, BigDecimal depositAmount, List<Transaction> transactions) {

        // Only create a deposit transaction if there's actually a deposit amount
        if (depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            Transaction transaction = createNewTransaction(
                    referenceId, transactionId, account, null, depositAmount, TransactionType.DEPOSIT, account.getBalance()
            );
            transactionRepository.save(transaction);
            transactions.add(transaction);
            accountRepository.save(account); // Save updated account balance
        }
    }


    private void processFullOwedPayment(
            String referenceId, String transactionId, Account account, Account recipient,
            OwedTransaction owedTransaction, BigDecimal owedAmount, List<Transaction> transactions) {

        Transaction transaction = createNewTransaction(referenceId, transactionId, account, recipient, owedAmount, TransactionType.TRANSFER, account.getBalance());
        transactionRepository.save(transaction);
        transactions.add(transaction);

        // Mark owed transaction as PAID
        owedTransaction.setStatus(Constants.STATUS_PAID);
        owedTransaction.setUpdateAt(LocalDateTime.now());
        owedTransactionRepository.save(owedTransaction);

        // Update balances
        recipient.setBalance(recipient.getBalance().add(owedAmount));
        account.setBalance(account.getBalance().subtract(owedAmount));

        accountRepository.save(account);
        accountRepository.save(recipient);
    }

    private void processPartialOwedPayment(
            String referenceId, String transactionId, Account account, Account recipient,
            OwedTransaction owedTransaction, BigDecimal partialAmount, List<Transaction> transactions) {

        Transaction transaction = createNewTransaction(referenceId, transactionId, account, recipient, partialAmount, TransactionType.TRANSFER, account.getBalance());
        transactionRepository.save(transaction);
        transactions.add(transaction);

        // Update OwedTransaction with partial payment
        owedTransaction.setAmount(owedTransaction.getAmount().subtract(partialAmount));
        owedTransaction.setStatus(Constants.STATUS_PARTIALLY_PAID);
        owedTransactionRepository.save(owedTransaction);

        // Update recipient balance
        recipient.setBalance(recipient.getBalance().add(partialAmount));
        account.setBalance(BigDecimal.ZERO); // All funds used

        accountRepository.save(account);
        accountRepository.save(recipient);
    }



    private Transaction createNewTransaction(String referenceId, String transactionId,
                                             Account account, Account recipient,
                                             BigDecimal amount, TransactionType transactionType, BigDecimal balanceAfter) {
        Transaction autoTransaction = new Transaction();
        autoTransaction.setRefTransactionId(referenceId);
        autoTransaction.setTransactionId(transactionId);
        autoTransaction.setAccount(account);
        autoTransaction.setRecipientId(recipient);
        autoTransaction.setTransactionType(transactionType.getCode());
        autoTransaction.setBalanceBefore(account.getBalance());
        autoTransaction.setAmount(amount);
        autoTransaction.setBalanceAfter(balanceAfter);
        return autoTransaction;
    }

    private OwedTransaction createOwedTransaction(
            Transaction transaction, Account payFrom, Account recipient,
            BigDecimal remainingAmount, String status) {
        OwedTransaction partialTransaction = new OwedTransaction();
        partialTransaction.setTransaction(transaction);
        partialTransaction.setOwedTransactionID(owedTransactionService.generateRefTransactionId());
        partialTransaction.setStatus(status); // Set status to PARTIALLY_PAID
        partialTransaction.setAmount(remainingAmount.abs()); // Set the remaining positive amount
        partialTransaction.setPayFrom(payFrom); // Use the same payer
        partialTransaction.setRecipient(recipient); // Use the same recipient
        return partialTransaction;
    }


    public TransactionResponse convertToDTO(List<Transaction> transactions) {
        List<TransactionResult> results = new ArrayList<>();
        for (Transaction t : transactions) {
            TransactionResult result = new TransactionResult(t.getRefTransactionId(),
                    t.getTransactionId(),
                    TransactionType.fromCode(t.getTransactionType()).toString(),
                    t.getAmount(),
                    t.getBalanceBefore(),
                    t.getBalanceAfter(),
                    null,
                    null);
            if (TransactionType.TRANSFER.getCode() == t.getTransactionType()) {
                result.setRecipientId(t.getRecipientId().getAccountRef());
                result.setRecipientName(t.getRecipientId().getAccountName());
            }
            results.add(result);
        }
        TransactionResponse transactionResponse = new TransactionResponse();
        transactionResponse.setResults(results);
        transactionResponse.setStatus("Success");
        transactionResponse.setMessage("Transaction Successfully");
        return transactionResponse;
    }

    private String generateRefTransactionId() {
        Long sequenceValue = transactionRepository.getNextSequenceValue();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        String formattedSeq = String.format("%04d", sequenceValue);
        return "TRX" + date + formattedSeq;
    }

    public String generateRefId() {
        long sequenceValue = 1_000_000L + new Random().nextLong(9_000_000L);
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        String formattedSeq = String.format("%04d", sequenceValue);
        return "RF" + date + formattedSeq;
    }


    private List<Transaction> handleTransfer(String referenceId, Account account,
                                             String recipientName,
                                             BigDecimal amount) {
        if (account.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Insufficient funds for transfer transaction");
        }
        Account recipient = accountRepository.findActiveAccountNameWithLockAndSessionId(recipientName, account.getSessionId())
                .orElseThrow(() -> new RuntimeException("Recipient account is inactive or not found"));
        List<Transaction> transactions = new ArrayList<>();
        Sort sort = Sort.by("createdAt");
        List<String> hardcodedStatuses = Arrays.asList(Constants.STATUS_UNPAID, Constants.STATUS_PARTIALLY_PAID);
        List<OwedTransaction> transactionList = owedTransactionRepository.findByPayFromAndRecipientAndStatusIn(recipient, account, hardcodedStatuses, sort);
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0 && transactionList.size() == 0) {
            BigDecimal balance = account.getBalance().subtract(amount);
            Transaction transaction = createNewTransaction(referenceId,
                    generateRefTransactionId(),
                    account, recipient,
                    amount, TransactionType.TRANSFER,
                    account.getBalance().subtract(amount));
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                OwedTransaction owedTransaction = createOwedTransaction(transaction, account, recipient, balance, Constants.STATUS_UNPAID);
                owedTransactionRepository.save(owedTransaction);
                balance = BigDecimal.ZERO;
                amount = account.getBalance();
                transaction.setAmount(account.getBalance());
            }
            account.setBalance(balance);
            accountRepository.save(account);

            recipient.setBalance(recipient.getBalance().add(amount));
            accountRepository.save(recipient);
            transactionRepository.save(transaction);
            transactions.add(transaction);
        } else {
            for (OwedTransaction owedTransaction : transactionList) {
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    break; // If no more amount needs to be paid, exit the loop
                }
                BigDecimal remainingOwedAmount = amount.subtract(owedTransaction.getAmount());
                if (remainingOwedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    if (remainingOwedAmount.compareTo(BigDecimal.ZERO) < 0
                            && account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                        remainingOwedAmount = amount.add(account.getBalance()).subtract(owedTransaction.getAmount());
                    }
                    owedTransaction.setStatus(Constants.STATUS_PAID);  // Mark as PAID
                    owedTransaction.setAmount(amount);
                    owedTransaction.setUpdateAt(LocalDateTime.now());
                    owedTransactionRepository.save(owedTransaction);
                    if (remainingOwedAmount.compareTo(BigDecimal.ZERO) < 0) {
                        OwedTransaction remainingOwedTransaction = createOwedTransaction(null, account, recipient, remainingOwedAmount, Constants.STATUS_UNPAID);
                        owedTransactionRepository.save(remainingOwedTransaction);
                    }
                } else {
                    owedTransaction.setStatus(Constants.STATUS_PAID);  // Mark as PAID
                    owedTransaction.setAmount(owedTransaction.getAmount());
                    owedTransaction.setUpdateAt(LocalDateTime.now());
                    owedTransactionRepository.save(owedTransaction);
                }
                amount = remainingOwedAmount;
            }
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal lastAmount = account.getBalance().subtract(amount);
                Transaction transaction = transactionRepository.save(createNewTransaction(referenceId,
                        generateRefTransactionId(),
                        account, recipient,
                        amount, TransactionType.TRANSFER, lastAmount));

                if (lastAmount.compareTo(BigDecimal.ZERO) < 0) {
                    OwedTransaction remainingOwedTransaction = createOwedTransaction(null, account, recipient, lastAmount, Constants.STATUS_UNPAID);
                    owedTransactionRepository.save(remainingOwedTransaction);
                    lastAmount = BigDecimal.ZERO;
                    amount = account.getBalance();
                }
                account.setBalance(lastAmount);
                accountRepository.save(account);
                recipient.setBalance(recipient.getBalance().add(amount));
                accountRepository.save(account);
                transaction.setAmount(amount);
                transaction.setBalanceAfter(account.getBalance());
                transactionRepository.save(transaction);
                transactions.add(transaction);
            }
        }
        return transactions;
    }
}
