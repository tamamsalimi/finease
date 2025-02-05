package co.id.finease.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data

public class AccountDTO {
    private String accountRef;
    private String accountName;
    private BigDecimal balance;
}
