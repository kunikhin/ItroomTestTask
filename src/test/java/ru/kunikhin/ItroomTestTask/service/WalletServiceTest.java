package ru.kunikhin.ItroomTestTask.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import ru.kunikhin.ItroomTestTask.exception.InsufficientFundsException;
import ru.kunikhin.ItroomTestTask.exception.WalletNotFoundException;
import ru.kunikhin.ItroomTestTask.model.entity.Wallet;
import ru.kunikhin.ItroomTestTask.repository.WalletRepository;
import ru.kunikhin.ItroomTestTask.util.WalletOperationType;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    private RetryTemplate retryTemplate;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new NeverRetryPolicy());

        walletService = new WalletService(walletRepository, retryTemplate);
    }

    @Test
    void createWallet_ShouldCreateNewWallet() {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setAmount(BigDecimal.ZERO);

        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        Wallet result = walletService.createWallet();

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getAmount());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void getWallet_ShouldReturnWallet() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setAmount(new BigDecimal("10050.50"));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWallet(walletId);

        assertNotNull(result);
        assertEquals(walletId, result.getId());
        assertEquals(new BigDecimal("10050.50"), result.getAmount());
    }

    @Test
    void getWallet_ShouldThrowException_WhenWalletNotFound() {
        UUID walletId = UUID.randomUUID();

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWallet(walletId));
    }

    @Test
    void executeOperation_ShouldDepositAmount() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setAmount(new BigDecimal("10000.00"));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.executeOperation(walletId, WalletOperationType.DEPOSIT, new BigDecimal("1000.00"));

        assertNotNull(result);
        assertEquals(new BigDecimal("11000.00"), result.getAmount());
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void executeOperation_ShouldWithdrawAmount() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setAmount(new BigDecimal("10000.00"));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.executeOperation(walletId, WalletOperationType.WITHDRAW, new BigDecimal("1000.00"));

        assertNotNull(result);
        assertEquals(new BigDecimal("9000.00"), result.getAmount());
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void executeOperation_ShouldThrowException_WhenInsufficientFunds() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setAmount(new BigDecimal("10000.00"));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientFundsException.class, () ->
                walletService.executeOperation(walletId, WalletOperationType.WITHDRAW, new BigDecimal("15000.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void executeOperation_ShouldThrowException_WhenNegativeAmount() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setAmount(new BigDecimal("100000.00"));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThrows(IllegalArgumentException.class, () ->
                walletService.executeOperation(walletId, WalletOperationType.DEPOSIT, new BigDecimal("-10.00")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void executeOperation_ShouldRetry_WhenOptimisticLockingFailure() {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .retryOn(OptimisticLockingFailureException.class)
                .build();

        WalletService walletServiceWithRetry = new WalletService(walletRepository, retryTemplate);

        UUID walletId = UUID.randomUUID();
        AtomicReference<BigDecimal> initialAmount = new AtomicReference<>(new BigDecimal("10000.00"));
        BigDecimal depositAmount = new BigDecimal("1000.00");
        BigDecimal expectedAmount = new BigDecimal("11000.00");

        when(walletRepository.findById(walletId)).thenAnswer(invocation -> {
            Wallet freshWallet = new Wallet();
            freshWallet.setId(walletId);
            freshWallet.setAmount(initialAmount.get());
            return Optional.of(freshWallet);
        });

        when(walletRepository.save(any(Wallet.class)))
                .thenThrow(new OptimisticLockingFailureException("First fail"))
                .thenThrow(new OptimisticLockingFailureException("Second fail"))
                .thenAnswer(invocation -> {
                    Wallet savedWallet = invocation.getArgument(0);
                    initialAmount.set(savedWallet.getAmount());
                    return savedWallet;
                });

        Wallet result = walletServiceWithRetry.executeOperation(
                walletId,
                WalletOperationType.DEPOSIT,
                depositAmount
        );

        assertNotNull(result);
        assertEquals(expectedAmount, result.getAmount());
        verify(walletRepository, times(3)).save(any(Wallet.class));
    }
}