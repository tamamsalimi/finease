package co.id.finease.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AccountResult {

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("name")
    private String name;

}
