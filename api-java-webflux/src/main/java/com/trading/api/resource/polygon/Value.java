package com.trading.api.resource.polygon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Value {
    private Integer timestamp;
    private Long value;
}
