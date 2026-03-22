package com.trading.api.resource.polygon.stock;

import com.trading.api.resource.polygon.Underlying;
import com.trading.api.resource.polygon.Value;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RSIResult {
    private Underlying underlying;
    private List<Value> values;
}
