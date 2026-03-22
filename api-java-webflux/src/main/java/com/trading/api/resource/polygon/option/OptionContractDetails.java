package com.trading.api.resource.polygon.option;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionContractDetails {
    @JsonProperty("contract_type)")
    private ContractType contractType;
    @JsonProperty("exercise_style)")
    private ExerciseStyle exerciseStyle;
    @JsonProperty("expiration_date")
    private LocalDate expirationDate;
    @JsonProperty("shares_per_contract")
    private Integer sharesPerContract;
    @JsonProperty("strike_price")
    private Double strikePrice;
}
