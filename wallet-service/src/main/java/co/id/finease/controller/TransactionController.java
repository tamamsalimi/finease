package co.id.finease.controller;

import co.id.finease.dto.TransactionRequest;
import co.id.finease.dto.TransactionResponse;
import co.id.finease.service.TransactionService;
import co.id.finease.utils.TransactionType;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/v2/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    @PostMapping()
    public ResponseEntity<TransactionResponse> process(
            @Valid @RequestHeader(value = "account-id")
            @RequestBody TransactionRequest request) {
        TransactionResponse response = new TransactionResponse();
        if (request.getTransactionType() == TransactionType.TRANSFER && (null == request.getRecipient() || request.getRecipient().isEmpty())) {
            response.setStatus("Bad request");
            response.setMessage("Transaction Cancel");
            return ResponseEntity.badRequest().body(response);  // Return a Bad Request with the response body
        }
        try {
            response = transactionService.processTransaction(request.getReferenceId(),
                    request.getRecipient(),
                    request.getAmount(),
                    request.getTransactionType());
        } catch (IllegalArgumentException e) {
            log.error("Transaction failed due to invalid input: {}", e.getMessage());
            ResponseEntity.internalServerError().body(buildErrorResponse("Invalid transaction type", e.getMessage()));

        } catch (RuntimeException e) {
            log.error("Transaction processing failed: {}", e.getMessage(), e);
            ResponseEntity.internalServerError().body(buildErrorResponse("Transaction processing error", e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage(), e);
            ResponseEntity.internalServerError().body(buildErrorResponse("Unexpected system error", "Please contact support."));
        }
        return ResponseEntity.ok(response);
    }

    private TransactionResponse buildErrorResponse(String status, String message) {
        TransactionResponse response = new TransactionResponse();
        response.setStatus(status);
        response.setMessage(message);
        return response;
    }

}
