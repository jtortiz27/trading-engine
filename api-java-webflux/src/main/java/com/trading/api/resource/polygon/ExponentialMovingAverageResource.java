package com.trading.api.resource.polygon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExponentialMovingAverageResource {
    private Underlying underlying = new Underlying();
    private List<Value> values;
}
