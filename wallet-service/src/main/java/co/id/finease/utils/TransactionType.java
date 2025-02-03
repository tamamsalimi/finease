package co.id.finease.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    DEPOSIT('D'), WITHDRAW('W'), TRANSFER('T');

    private final char code;

    TransactionType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    @JsonValue // Used when converting Enum -> JSON (Response)
    public String toJson() {
        return this.name(); // Returns "CREDIT" or "DEBIT"
    }

    @JsonCreator // Used when converting JSON -> Enum (Request)
    public static TransactionType fromJson(String value) {
        return switch (value.toUpperCase()) {
            case "DEPOSIT" -> DEPOSIT;
            case "WITHDRAW" -> WITHDRAW;
            case "TRANSFER" -> TRANSFER;
            default -> throw new IllegalArgumentException("Invalid transaction type: " + value);
        };
    }

    public static TransactionType fromCode(char code) {
        return switch (code) {
            case 'D' -> DEPOSIT;
            case 'W' -> WITHDRAW;
            case 'T'-> TRANSFER;
            default -> throw new IllegalArgumentException("Invalid transaction type code: " + code);
        };
    }
}
