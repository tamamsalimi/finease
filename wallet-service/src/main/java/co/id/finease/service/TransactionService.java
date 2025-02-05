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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        List<Transaction> transactions;

        switch (transactionType) {
            case WITHDRAW:
                transactions = handleWithdraw(referenceId, account, amount);
                break;
            case DEPOSIT:
                transactions = handleDeposit(referenceId, account, amount, TransactionType.DEPOSIT);
                break;
            case TRANSFER:
                transactions = handleTransfer(referenceId, account, recipientId, amount);
                break;
            default:
                throw new IllegalArgumentException("Invalid transaction type: " + transactionType);
        }
        response = convertToDTO(transactions);
        Optional<Account> optional = accountRepository.findByAccountRefAndSessionId(account.getAccountRef(),account.getSessionId());
        response.setAccountResult(new AccountResult(account.getAccountRef(),optional.get().getBalance(),account.getAccountName()));
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
        return Arrays.asList(transaction);
    }

    private List<Transaction> handleDeposit(String referenceId, Account account, BigDecimal amount, TransactionType transactionType) {
        Sort sort = Sort.by("createdAt");
        List<String> hardcodedStatuses = Arrays.asList(Constants.STATUS_UNPAID, Constants.STATUS_PARTIALLY_PAID);
        List<OwedTransaction> transactionList = owedTransactionRepository.findByPayFromAndStatusIn(account, hardcodedStatuses, sort);
        String transactionId = generateRefTransactionId();
        List<Transaction> transactions = new ArrayList<>();
        if (transactionList.size() > 0) {
            for (OwedTransaction owedTransaction : transactionList) {
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal remainingAmount = amount.subtract(owedTransaction.getAmount());
                Account recipient = owedTransaction.getRecipient();
                if (remainingAmount.compareTo(BigDecimal.ZERO) >= 0) {
                    owedTransaction.setStatus(Constants.STATUS_PAID);
                    owedTransaction.setUpdateAt(LocalDateTime.now());
                    owedTransactionRepository.save(owedTransaction);
                    recipient.setBalance(recipient.getBalance().add(owedTransaction.getAmount()));
                    accountRepository.save(recipient);
                } else {
                    owedTransaction.setStatus(Constants.STATUS_PAID);
                    owedTransaction.setAmount(amount);
                    owedTransactionRepository.save(owedTransaction);
                    recipient.setBalance(recipient.getBalance().add(owedTransaction.getAmount()));
                    accountRepository.save(recipient);

                    if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
                        OwedTransaction partialOwedTransaction = createOwedTransaction(owedTransaction.getTransaction(), account, recipient, remainingAmount, Constants.STATUS_PARTIALLY_PAID);
                        owedTransactionRepository.save(partialOwedTransaction);
                        remainingAmount = BigDecimal.ZERO;
                    }
                }
                Transaction transaction = transactionRepository.save(createNewTransaction(referenceId, transactionId, account, recipient, owedTransaction.getAmount(), TransactionType.TRANSFER, remainingAmount));
                transactions.add(transaction);
                amount = remainingAmount;
            }
        }
        Transaction transaction = transactionRepository.save(createNewTransaction(
                referenceId, transactionId, account, null, amount, transactionType,
                account.getBalance().add(amount)));
        transactions.add(transaction);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        return transactions;
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
                    owedTransaction.setStatus(Constants.STATUS_PAID);  // Mark as PAID
                    owedTransaction.setAmount(amount);
                    owedTransaction.setUpdateAt(LocalDateTime.now());
                    owedTransactionRepository.save(owedTransaction);
                    if (remainingOwedAmount.compareTo(BigDecimal.ZERO) < 0) {
                        OwedTransaction remainingOwedTransaction = createOwedTransaction(null, account, recipient, remainingOwedAmount, Constants.STATUS_UNPAID);
                        owedTransactionRepository.save(remainingOwedTransaction);
                    }
                    amount = remainingOwedAmount;
                } else {
                    owedTransaction.setStatus(Constants.STATUS_PAID);  // Mark as PAID
                    owedTransaction.setAmount(owedTransaction.getAmount());
                    owedTransaction.setUpdateAt(LocalDateTime.now());
                    owedTransactionRepository.save(owedTransaction);
                    amount = remainingOwedAmount;
                }
            }
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                Transaction transaction = transactionRepository.save(createNewTransaction(referenceId,
                        generateRefTransactionId(),
                        account, recipient,
                        amount, TransactionType.TRANSFER,
                        account.getBalance().subtract(amount)));
                account.setBalance(account.getBalance().subtract(amount));
                accountRepository.save(account);

                recipient.setBalance(recipient.getBalance().add(amount));
                accountRepository.save(account);

                transaction.setAmount(amount);
                transaction.setBalanceAfter(amount);
                transactionRepository.save(transaction);
            }
        }
        return transactions;
    }


    private TransactionResponse convertToDTO(List<Transaction> transactions) {
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

}
