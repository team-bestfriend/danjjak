package com.bestfriend.danjjak.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.DirectRecipientRequest;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.TransferRequest;
import com.bestfriend.danjjak.transfer.mapper.TransferMapper;
import com.bestfriend.danjjak.transfer.model.RecipientRecord;
import com.bestfriend.danjjak.transfer.model.TransactionCommand;
import com.bestfriend.danjjak.transfer.model.TransferAccountRecord;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TransferServiceTest {

    private TransferMapper transferMapper;
    private PinVerifier pinVerifier;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferMapper = mock(TransferMapper.class);
        pinVerifier = mock(PinVerifier.class);
        Clock clock =
                Clock.fixed(
                        Instant.parse("2026-09-03T00:00:00Z"),
                        ZoneId.of("Asia/Seoul"));
        transferService =
                new TransferService(transferMapper, new FdsEvaluator(), pinVerifier, clock);
    }

    @Test
    void transfersToRegisteredRecipientAndStoresSnapshot() {
        TransferAccountRecord source = sourceAccount("50000000");
        RecipientRecord recipient = registeredRecipient();
        when(transferMapper.findRegisteredRecipient(1L, 20L)).thenReturn(recipient);
        when(transferMapper.findSourceAccountForUpdate(1L, 1L)).thenReturn(source);
        when(pinVerifier.matches("1234", "hash")).thenReturn(true);
        when(transferMapper.debitAccount(1L, 1L, new BigDecimal("100000")))
                .thenReturn(1);
        doAnswer(
                        invocation -> {
                            TransactionCommand command = invocation.getArgument(0);
                            command.setTransactionId(30L);
                            return 1;
                        })
                .when(transferMapper)
                .insertTransaction(any());

        var response =
                transferService.transfer(
                        1L,
                        new TransferRequest(
                                1L, 20L, null, new BigDecimal("100000"), null, "1234"));

        assertEquals("COMPLETED", response.status());
        assertEquals(30L, response.transactionId());
        assertEquals(new BigDecimal("49900000"), response.balanceAfter());
        ArgumentCaptor<TransactionCommand> captor =
                ArgumentCaptor.forClass(TransactionCommand.class);
        verify(transferMapper).insertTransaction(captor.capture());
        assertEquals("김민수", captor.getValue().getRecipientName());
        assertEquals(20L, captor.getValue().getRecipientAccountId());
    }

    @Test
    void directTransferUsesRequestSnapshotWithoutSavingRecipientAccount() {
        when(transferMapper.findSourceAccountForUpdate(1L, 1L))
                .thenReturn(sourceAccount("50000000"));
        when(pinVerifier.matches("1234", "hash")).thenReturn(true);
        when(transferMapper.debitAccount(1L, 1L, new BigDecimal("50000"))).thenReturn(1);
        doAnswer(
                        invocation -> {
                            TransactionCommand command = invocation.getArgument(0);
                            command.setTransactionId(31L);
                            return 1;
                        })
                .when(transferMapper)
                .insertTransaction(any());

        transferService.transfer(
                1L,
                new TransferRequest(
                        1L,
                        null,
                        new DirectRecipientRequest(
                                "박친구", "003", "기업은행", "000-000-000003"),
                        new BigDecimal("50000"),
                        null,
                        "1234"));

        ArgumentCaptor<TransactionCommand> captor =
                ArgumentCaptor.forClass(TransactionCommand.class);
        verify(transferMapper).insertTransaction(captor.capture());
        assertEquals(null, captor.getValue().getRecipientAccountId());
        assertEquals("000-000-000003", captor.getValue().getRecipientAccountNumber());
        verify(transferMapper, never()).findRegisteredRecipient(1L, 20L);
    }

    @Test
    void wrongPinDoesNotChangeBalanceOrCreateTransaction() {
        when(transferMapper.findRegisteredRecipient(1L, 20L))
                .thenReturn(registeredRecipient());
        when(transferMapper.findSourceAccountForUpdate(1L, 1L))
                .thenReturn(sourceAccount("50000000"));
        when(pinVerifier.matches("0000", "hash")).thenReturn(false);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                transferService.transfer(
                                        1L,
                                        new TransferRequest(
                                                1L,
                                                20L,
                                                null,
                                                new BigDecimal("100000"),
                                                null,
                                                "0000")));

        assertEquals("PIN_MISMATCH", exception.getCode());
        verify(transferMapper, never()).debitAccount(any(Long.class), any(Long.class), any());
        verify(transferMapper, never()).insertTransaction(any());
    }

    @Test
    void insufficientBalanceDoesNotCreateTransaction() {
        when(transferMapper.findRegisteredRecipient(1L, 20L))
                .thenReturn(registeredRecipient());
        when(transferMapper.findSourceAccountForUpdate(1L, 1L))
                .thenReturn(sourceAccount("1000"));
        when(pinVerifier.matches("1234", "hash")).thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                transferService.transfer(
                                        1L,
                                        new TransferRequest(
                                                1L,
                                                20L,
                                                null,
                                                new BigDecimal("2000"),
                                                null,
                                                "1234")));

        assertEquals("INSUFFICIENT_BALANCE", exception.getCode());
        verify(transferMapper, never()).insertTransaction(any());
    }

    private TransferAccountRecord sourceAccount(String balance) {
        TransferAccountRecord source = new TransferAccountRecord();
        source.setAccountId(1L);
        source.setPinHash("hash");
        source.setBalance(new BigDecimal(balance));
        return source;
    }

    private RecipientRecord registeredRecipient() {
        RecipientRecord recipient = new RecipientRecord();
        recipient.setAccountId(20L);
        recipient.setName("김민수");
        recipient.setBankCode("020");
        recipient.setBankName("우리은행");
        recipient.setAccountNumber("1002-000-000001");
        return recipient;
    }
}
