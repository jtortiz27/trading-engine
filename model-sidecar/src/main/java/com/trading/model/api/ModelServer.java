package com.trading.model.api;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import com.trading.model.StockFeatures;
import com.trading.model.TradeRecommendation;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModelServer {
  private ZooModel<StockFeatures, TradeRecommendation> model;

  @SneakyThrows
  @PostConstruct
  public void init() {
    Criteria<StockFeatures, TradeRecommendation> criteria =
        Criteria.builder()
            .setTypes(StockFeatures.class, TradeRecommendation.class)
            .optModelPath(Paths.get("shared-models/onnx/trade-recommender"))
            .build();

    model = ModelZoo.loadModel(criteria);
  }

  @SneakyThrows
  @PostMapping("/infer")
  public TradeRecommendation infer(@RequestBody StockFeatures features) {
    try (Predictor<StockFeatures, TradeRecommendation> predictor = model.newPredictor()) {
      return predictor.predict(features);
    }
  }

  @PostMapping("/reload")
  public String reloadModel() throws IOException, MalformedModelException {
    model.close();
    init();
    return "Model reloaded";
  }
}
