package co.id.finease.dto;

import co.id.finease.entity.Account;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TransactionResponse {

    @JsonProperty
    private String message;

    @JsonProperty
    private String status;

    @JsonProperty("account")
    private AccountResult accountResult;

    @JsonProperty("results")
    private List<TransactionResult> results;

    @JsonProperty("owed_by")
    private List<OwedTransactionItem> owedBy;

    @JsonProperty("owed_to")
    private List<OwedTransactionItem> owedTo;

}
