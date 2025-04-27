package com.trading.api.resource.polygon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Underlying {
    private List<Aggregate> aggregates;
    private String url;
}
