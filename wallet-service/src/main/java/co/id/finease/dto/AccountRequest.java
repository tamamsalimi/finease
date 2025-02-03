package co.id.finease.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRequest {
    @JsonProperty("account_name")
    @NotNull(message = "Account name is required")
    @NotBlank(message = "Account name is required")
    private String accountName;

}
