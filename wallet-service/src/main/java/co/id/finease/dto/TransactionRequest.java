package co.id.finease.dto;

import co.id.finease.utils.TransactionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    @JsonProperty("recipient")
    private String recipient; // Using accountRef instead of accountId
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    @JsonProperty("transaction_type")
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;


    @JsonProperty("reference_id")
    @NotNull(message = "Reference id is required")
    private String referenceId;
}
