package co.id.finease.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClientResponse {

    @JsonProperty("status_code")
    private int statusCode;

    @JsonProperty("status_message")
    private String statusMessage;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("application_id")
    private String applicationId;

    @JsonProperty("api_key")
    private String apiKey;
}

