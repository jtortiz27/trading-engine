package com.trading.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.*;

@Getter
@Configuration
public class AlgoConfig {

    //Volume
    @Value("${volume.days}")
    private Integer volumeDaysToLookBack;
    @Value("${volume.strongThreshold}")
    private Integer strongThreshold;
    @Value("${volume.weakThreshold}")
    private Integer weakThreshold;

    //RTH
    @Value("${rth.start}")
    private Integer rthStart;
    @Value("${rth.end}")
    private Integer rthEnd;

    //Volatility
    @Value("${volatility.historical.days}")
    private Integer historicalVolatilityDaysToLookBack;
    @Value("${volatility.rank.days}")
    private Integer rankDaysToLookBack;
    @Value("${volatility.ivhv.rich}")
    private Double impliedVolatilityHistoricalVolatilityRatioRich;


    //MACD Inputs
    @Value("${macd.fast}")
    private Integer macdFast;
    @Value("${macd.rsiLen}")
    private Integer rsiLen;
    @Value("${macd.slow}")
    private Integer macdSlow;
    @Value("${macd.slow}")
    private Integer macdSigma;

    //Condor Score
    @Value("${condor.proxLen}")
    private Integer proxLen;
    @Value("${condor.proxThresh}")
    private Integer proxThresh;
    @Value("${condor.numDev}")
    private Double numDev;
    @Value("${condor.drift.length}")
    private Double driftLength;
    @Value("${condor.drift.high}")
    private Double driftHigh;
    @Value("${condor.drift.low}")
    private Double driftLow;

    private ZonedDateTime regularTradingHoursStart = ZonedDateTime.of(LocalDateTime.of(LocalDate.now(), LocalTime.of(9,30)), ZoneId.of("America/New_York"));
    private ZonedDateTime regularTradingHoursEnd = ZonedDateTime.of(LocalDateTime.of(LocalDate.now(), LocalTime.of(4,00)), ZoneId.of("America/New_York"));

}
