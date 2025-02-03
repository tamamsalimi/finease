package co.id.finease.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TransactionResult {

    @JsonProperty("reference_id")
    private String referenceId;
    @JsonProperty("transaction_id")
    private String transactionID;

    @JsonProperty("transaction_type")
    private String transactionType; // Will be converted from ENUM to String

    private BigDecimal amount;

    @JsonProperty("balance_before")
    private BigDecimal balanceBefore;

    @JsonProperty("balance_after")
    private BigDecimal balanceAfter;

    @JsonProperty("recipient_id")
    private String recipientId;

    @JsonProperty("recipient_name")
    private String recipientName;
}
