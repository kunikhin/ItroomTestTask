package ru.kunikhin.ItroomTestTask.controller;


import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.kunikhin.ItroomTestTask.model.entity.Wallet;
import ru.kunikhin.ItroomTestTask.model.dto.WalletOperationDto;
import ru.kunikhin.ItroomTestTask.service.WalletService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<BigDecimal> getWalletBalance(@PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getWallet(walletId).getAmount());
    }

    @PostMapping("/wallet/new")
    public ResponseEntity<Wallet> createWallet() {
        return ResponseEntity.ok(walletService.createWallet());
    }

    @PatchMapping("/wallet")
    public ResponseEntity<?> executeOperation(@Valid @RequestBody WalletOperationDto request) {
        return ResponseEntity.ok(
                walletService.executeOperation(
                    request.getWalletId(),
                    request.getOperationType(),
                    request.getAmount()
                )
        );
    }
}
