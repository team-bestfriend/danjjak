package com.bestfriend.danjjak.transfer.dto;

import java.math.BigDecimal;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public final class TransferDtos {

    private TransferDtos() {}

    public record DirectRecipientRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 20) String bankCode,
            @NotBlank @Size(max = 50) String bankName,
            @NotBlank
                    @Size(min = 8, max = 50)
                    @Pattern(regexp = "^[0-9-]+$")
                    String accountNumber) {}

    public record TransferRequest(
            @NotNull @Positive Long sourceAccountId,
            @Positive Long registeredRecipientAccountId,
            @Valid DirectRecipientRequest directRecipient,
            @NotNull @DecimalMin("1") @Digits(integer = 15, fraction = 0) BigDecimal amount,
            @Positive Long patternExecutionId,
            @NotBlank @Size(max = 30) String pin) {}

    public record TransferResponse(
            String status,
            String riskLevel,
            List<String> reasons,
            int recentTransferCount,
            Long anomalyEventId,
            Long transactionId,
            BigDecimal balanceAfter) {}

    public record ResolveAnomalyRequest(@NotBlank String action, @NotNull Boolean rechecked) {}

    public record ResolveAnomalyResponse(
            long anomalyEventId,
            String action,
            Long transactionId,
            BigDecimal balanceAfter) {}
}
