package test_assignment.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class SendMoneyRequest {
    @JsonProperty("to")
    private String recipientLogin;
    private BigDecimal amount;
}
