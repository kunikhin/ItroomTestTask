package ru.kunikhin.ItroomTestTask.model.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import ru.kunikhin.ItroomTestTask.util.WalletOperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletOperationDto {

    @JsonProperty("walletId")
    @NotNull(message = "Wallet ID is required")
    private UUID walletId;

    @JsonProperty("operationType")
    @NotNull(message = "Operation type must be DEPOSIT or WITHDRAW")
    private WalletOperationType operationType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
    private BigDecimal amount;

    public WalletOperationDto() {
    }

    public WalletOperationDto(UUID walletId, WalletOperationType operationType, BigDecimal amount) {
        this.walletId = walletId;
        this.operationType = operationType;
        this.amount = amount;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public WalletOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(WalletOperationType operationType) {
        this.operationType = operationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}