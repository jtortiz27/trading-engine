package com.trading.ml.dataset;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.translate.Translator;
import com.trading.model.StockFeatures;
import com.trading.model.TradeRecommendation;
import java.util.List;

public class InMemoryDataset extends ArrayDataset {

  public InMemoryDataset(
      List<StockFeatures> features,
      List<TradeRecommendation> labels,
      Translator<StockFeatures, TradeRecommendation> translator) {

    super(
        new Builder()
            .setData(buildFeatures(NDManager.newBaseManager(), features))
            .optLabels(buildLabels(NDManager.newBaseManager(), labels))
            .setSampling(features.size(), false));
  }

  private static NDArray[] buildFeatures(NDManager manager, List<StockFeatures> inputs) {
    return inputs.stream()
        .map(
            input -> {
              float[] featureVec = new float[] {(float) input.getPriceChange()
                    // Extend with more normalized features if needed
                  };
              return manager.create(featureVec);
            })
        .toArray(NDArray[]::new);
  }

  private static NDArray[] buildLabels(NDManager manager, List<TradeRecommendation> labels) {
    return labels.stream()
        .map(
            label -> {
              float[] encoded = new float[3];
              switch (label.getLabel()) {
                case "BUY" -> encoded[0] = 1f;
                case "SELL" -> encoded[1] = 1f;
                default -> encoded[2] = 1f;
              }
              return manager.create(encoded);
            })
        .toArray(NDArray[]::new);
  }
}
