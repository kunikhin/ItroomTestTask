package ru.kunikhin.ItroomTestTask.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.kunikhin.ItroomTestTask.exception.InsufficientFundsException;
import ru.kunikhin.ItroomTestTask.exception.WalletNotFoundException;
import ru.kunikhin.ItroomTestTask.model.dto.WalletOperationDto;
import ru.kunikhin.ItroomTestTask.model.entity.Wallet;
import ru.kunikhin.ItroomTestTask.service.WalletService;
import ru.kunikhin.ItroomTestTask.util.WalletOperationType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

    @Test
    void createWallet_ShouldReturnCreatedWallet() throws Exception {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setAmount(BigDecimal.ZERO);

        when(walletService.createWallet()).thenReturn(wallet);

        mockMvc.perform(post("/api/v1/wallet/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.amount").value(0));
    }

    @Test
    void getWalletBalance_ShouldReturnBalance() throws Exception {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setAmount(new BigDecimal("100.50"));

        when(walletService.getWallet(walletId)).thenReturn(wallet);

        mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId))
                .andExpect(status().isOk())
                .andExpect(content().string("100.50"));
    }

    @Test
    void getWalletBalance_ShouldReturnNotFound_WhenWalletNotExists() throws Exception {
        UUID walletId = UUID.randomUUID();

        when(walletService.getWallet(walletId))
                .thenThrow(new WalletNotFoundException("Wallet not found with id: " + walletId));

        mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Wallet not found with id: " + walletId));
    }

    @Test
    void executeOperation_ShouldProcessDeposit() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationDto request = new WalletOperationDto(
                walletId, WalletOperationType.DEPOSIT, new BigDecimal("100")
        );

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setAmount(new BigDecimal("1000"));

        when(walletService.executeOperation(any(), any(), any())).thenReturn(wallet);

        mockMvc.perform(patch("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.amount").value(1000));
    }

    @Test
    void executeOperation_ShouldProcessWithdraw() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationDto request = new WalletOperationDto(
                walletId, WalletOperationType.WITHDRAW, new BigDecimal("1000")
        );

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setAmount(new BigDecimal("1000"));

        when(walletService.executeOperation(any(), any(), any())).thenReturn(wallet);

        mockMvc.perform(patch("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.amount").value(1000));
    }

    @Test
    void executeOperation_ShouldReturnBadRequest_WhenInsufficientFunds() throws Exception {
        WalletOperationDto request = new WalletOperationDto(
                UUID.randomUUID(), WalletOperationType.WITHDRAW, new BigDecimal("1000.00")
        );

        when(walletService.executeOperation(any(), any(), any()))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        mockMvc.perform(patch("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    void executeOperation_ShouldReturnBadRequest_WhenNegativeAmount() throws Exception {
        WalletOperationDto request = new WalletOperationDto(
                UUID.randomUUID(), WalletOperationType.DEPOSIT, new BigDecimal("-1000.00")
        );

        mockMvc.perform(patch("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("amount"))
                .andExpect(jsonPath("$.validationErrors[0].message").value("Amount must be positive"))
                .andExpect(jsonPath("$.validationErrors[0].rejectedValue").value(-1000.00));
    }

    @Test
    void executeOperation_ShouldReturnBadRequest_WhenInvalidJson() throws Exception {
        String invalidRequest = """
        {
            "walletId": "%s",
            "operationType": "DAITE_DENEG_MOLU",
            "amount": 1000.00
        }
        """.formatted(UUID.randomUUID());

        mockMvc.perform(patch("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid JSON format"));
    }

    @Test
    void executeOperation_ShouldReturnNotFound_WhenWalletNotExists() throws Exception {
        UUID nonExistentWalletId = UUID.randomUUID();
        WalletOperationDto request = new WalletOperationDto(
                nonExistentWalletId, WalletOperationType.DEPOSIT, new BigDecimal("1000.00")
        );

        when(walletService.executeOperation(any(), any(), any()))
                .thenThrow(new WalletNotFoundException("Wallet not found with id: " + nonExistentWalletId));

        mockMvc.perform(patch("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Wallet not found with id: " + nonExistentWalletId));
    }
}