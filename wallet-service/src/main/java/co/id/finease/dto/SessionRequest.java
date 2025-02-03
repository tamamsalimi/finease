package co.id.finease.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionRequest {


    @JsonProperty("application_id")
    private String applicationID;
    @JsonProperty("api_key")
    private String apiKey;
}
