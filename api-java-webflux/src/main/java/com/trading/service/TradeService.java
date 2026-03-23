package com.trading.service;

import com.trading.api.resource.polygon.option.OptionContractSnapshotResource;
import com.trading.api.resource.polygon.stock.*;
import com.trading.api.resource.ticker.summary.Macro;
import com.trading.api.resource.ticker.summary.TickerSummaryResource;
import com.trading.api.resource.ticker.summary.Trend;
import com.trading.config.AlgoConfig;
import com.trading.model.TradeRecommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.trading.api.resource.ticker.summary.Macro.*;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final AlgoConfig algoConfig;
    private final WebClient webClient;
    private final StockMarketIntegrationService stockMarketIntegrationService;

    public Mono<TickerSummaryResource> generateTickerSummary(String symbol) {

        TickerSummaryResource tickerSummaryResource = new TickerSummaryResource();

        //Determine Macro Tier
        Mono<Macro> macroTierMono =  stockMarketIntegrationService.retrieveIndexSnapshot("I:VIX")
                .map(vixSnapshot -> vixSnapshot.getResults().get(0).getValue())
                .map(vixValue -> {
                    if (vixValue < 16) return 0;
                    else if (vixValue < 22) return 1;
                    else if (vixValue < 35) return 2;
                    else return 3;
                })
                .map(tier -> {
                    switch (tier) {
                        case 0:
                            return CALM;
                        case 1:
                            return ELEV;
                        case 2:
                            return RISK_OFF;
                        default:
                            return CHAOS;
                    }
                });

        //Determine Volume Strength
        long regularTradingHoursSeconds = Duration.between(algoConfig.getRegularTradingHoursStart(), algoConfig.getRegularTradingHoursEnd()).toSeconds();
        long secondsIntoMarket = Duration.between(algoConfig.getRegularTradingHoursStart(), ZonedDateTime.now(ZoneId.of("America/New_York"))).toSeconds();

        double fracDay;
        if (secondsIntoMarket <= 0) {
            fracDay = 0;
        } else if (secondsIntoMarket >= regularTradingHoursSeconds) {
            fracDay = 1;
        } else {
            fracDay = (double) secondsIntoMarket / regularTradingHoursSeconds;
        }


        Mono<ExponentialMovingAverageResource> exponentialMovingAverageResourceMono = stockMarketIntegrationService.retrieveEMAForTicker(symbol);

        //Implied Volatility
        //https://polygon.io/docs/rest/options/snapshots/option-contract-snapshot
        //Compare to historical Volatility 1 year prior
        Mono<OptionContractSnapshotResource> optionContractSnapshotResourceMono = stockMarketIntegrationService.retrieveOptionContractSnapshot(symbol);

        //Historical Volatility
        Mono<StockAggregateBarsResource> aggregateBarsResourceMono = stockMarketIntegrationService.retrieveCustomBarsForTicker(symbol);

        //Momentum + Trends
        Mono<MACDResource> macdResourceMono = stockMarketIntegrationService.retrieveMACDForTicker(symbol);
        Mono<RSIResource> rsiResuorceMono = stockMarketIntegrationService.retrieveRSIForTicker(symbol);

        return Mono.zip(macroTierMono,macdResourceMono, rsiResuorceMono, exponentialMovingAverageResourceMono, optionContractSnapshotResourceMono,aggregateBarsResourceMono)
                        .map( tuple -> {
                            Macro macroTier = tuple.getT1();
                            MACDResource macdResource = tuple.getT2();
                            RSIResource rsiResource = tuple.getT3();
                            ExponentialMovingAverageResource emaResource = tuple.getT4();
                            OptionContractSnapshotResource contractSnapshotResource = tuple.getT5();
                            StockAggregateBarsResource aggregateBarsResource = tuple.getT6();

                            tickerSummaryResource.setMacro(macroTier);

                            Double averageVolume = aggregateBarsResource.getResults().stream().map(AggregateBarsResult::getTradingVolume).collect(Collectors.averagingLong(Long::longValue));
                            
                            //Volume Dashboard
                            Double impliedVolatility = contractSnapshotResource.getResults().getImpliedVolatility();
                            Double historicalVolatility = determineHistoricalVolatility(aggregateBarsResource).get(determineHistoricalVolatility(aggregateBarsResource).size() - 1);

                            //Momentum + Trends
                            tickerSummaryResource.setTimestamp(Instant.now());
                            tickerSummaryResource.setImpliedVolatility(impliedVolatility);
                            tickerSummaryResource.setHistoricalVolatility30Day(historicalVolatility);
                            tickerSummaryResource.setImpliedVolatilityHistoricalVolatilityRatio(impliedVolatility/historicalVolatility);

                            Long macdValue = macdResource.getResults().getValues().get(0).getValue();

                            //Condor Score
                            Double midPoint = aggregateBarsResource.getResults()
                                    .stream()
                                    .map(result -> result.getClosePrice())
                                    .collect(Collectors.averagingDouble(Double::doubleValue));

                            return tickerSummaryResource;
                        });

        //Strategy Selector

        //Risk Guard

        //Bollinger
    }

    private static List<Double> determineHistoricalVolatility(StockAggregateBarsResource aggregateBarsResource) {
        List<AggregateBarsResult> aggregateBarsResults = aggregateBarsResource.getResults();

        List<Double> out = new ArrayList<>(Collections.nCopies(aggregateBarsResults.size(), Double.NaN));
        double[] gkv = new double[aggregateBarsResults.size()];
        for (int i=0;i<aggregateBarsResults.size();i++) {
            double u = log(aggregateBarsResults.get(i).getHighPrice() / aggregateBarsResults.get(i).getOpenPrice());
            double d = log(aggregateBarsResults.get(i).getLowPrice() / aggregateBarsResults.get(i).getOpenPrice());
            double c = log(aggregateBarsResults.get(i).getClosePrice() / aggregateBarsResults.get(i).getOpenPrice());
            gkv[i] = 0.5*(u*u + d*d) - (2*log(2) - 1)*(c*c);
        }
        int window = 30;
        int annFactor = 252;
        for (int i=window-1;i<aggregateBarsResults.size();i++) {
            double sum=0; for (int j=i-window+1;j<=i;j++) sum+=gkv[j];
            out.set(i, sqrt(sum/window) * sqrt(annFactor));
        }
        return out;
    }
}
