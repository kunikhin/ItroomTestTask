package ru.kunikhin.ItroomTestTask.service;

import jakarta.transaction.Transactional;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import ru.kunikhin.ItroomTestTask.exception.InsufficientFundsException;
import ru.kunikhin.ItroomTestTask.exception.WalletNotFoundException;
import ru.kunikhin.ItroomTestTask.model.entity.Wallet;
import ru.kunikhin.ItroomTestTask.repository.WalletRepository;
import ru.kunikhin.ItroomTestTask.util.WalletOperationType;

import java.math.BigDecimal;
import java.util.UUID;


@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final RetryTemplate retryTemplate;

    public WalletService(WalletRepository walletRepository, RetryTemplate retryTemplate) {
        this.walletRepository = walletRepository;
        this.retryTemplate = retryTemplate;
    }

    public Wallet getWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));
    }

    @Transactional
    public Wallet createWallet() {
        return walletRepository.save(new Wallet());
    }

    @Transactional
    public Wallet executeOperation(UUID walletId, WalletOperationType operationType, BigDecimal amount) {
        return retryTemplate.execute(context ->
                doExecuteOperation(walletId, operationType, amount)
        );
    }

    private Wallet doExecuteOperation(UUID walletId, WalletOperationType operationType, BigDecimal amount) {
        Wallet wallet = getWallet(walletId);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        switch (operationType) {
            case DEPOSIT:
                wallet.setAmount(wallet.getAmount().add(amount));
                break;
            case WITHDRAW:
                if (wallet.getAmount().compareTo(amount) < 0) {
                    throw new InsufficientFundsException("Insufficient funds");
                }
                wallet.setAmount(wallet.getAmount().subtract(amount));
                break;
            default:
                throw new IllegalArgumentException("Unknown operation type: " + operationType);
        }

        return walletRepository.save(wallet);
    }

}
