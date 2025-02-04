package co.id.finease.service;

import co.id.finease.dto.OwedTransactionItem;
import co.id.finease.entity.Account;
import co.id.finease.repository.AccountRepository;
import co.id.finease.repository.OwedTransactionRepository;
import co.id.finease.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OwedTransactionService {

    private final SessionService sessionService;

    private final OwedTransactionRepository owedTransactionRepository;

    public OwedTransactionService(SessionService sessionService, OwedTransactionRepository owedTransactionRepository) {
        this.sessionService = sessionService;
        this.owedTransactionRepository = owedTransactionRepository;
    }
    public String generateRefTransactionId() {
        Long sequenceValue = owedTransactionRepository.getNextSequenceValue();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        String formattedSeq = String.format("%04d", sequenceValue);
        return "OWED" + date + formattedSeq;
    }


    public List<OwedTransactionItem> getOwedTransactionSummary(Account account) {
        if (null == account)
            account = sessionService.getAccountIdFromSecurityContext();
        // Fetching the grouped data from the repository
        List<Object[]> result = owedTransactionRepository.findTotalOwedGroupedByRecipient(account);
        // Mapping the query result to OwedItem objects
        return result.stream()
                .map(row -> new OwedTransactionItem(
                        (String) row[0],   // recipientId
                        (String) row[1],  // recipientName
                        (BigDecimal) row[2]  // totalAmount (column 3 now contains the sum)
                ))
                .collect(Collectors.toList()); // Collect the result into a List of OwedItem
    }

    public List<OwedTransactionItem> getAmountsOwedByMe(Account account) {
        if (null == account)
            account = sessionService.getAccountIdFromSecurityContext();
        // Fetching the grouped data from the repository for amounts owed by the user
        List<Object[]> result = owedTransactionRepository.findTotalOwedGroupedByPayer(account);

        // Mapping the query result to OwedItem objects
        return result.stream()
                .map(row -> new OwedTransactionItem(
                        (String) row[0],   // recipientId
                        (String) row[1],  // recipientName
                        (BigDecimal) row[2]  // totalAmount (column 3 now contains the sum)
                ))
                .collect(Collectors.toList()); // Collect the result into a List of OwedItem
    }
}