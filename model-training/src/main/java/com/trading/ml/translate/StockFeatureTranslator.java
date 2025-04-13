package com.trading.ml.translate;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.trading.model.StockFeatures;
import com.trading.model.TradeRecommendation;

public class StockFeatureTranslator implements Translator<StockFeatures, TradeRecommendation> {

  @Override
  public NDList processInput(TranslatorContext ctx, StockFeatures input) {
    float[] features = new float[] {(float) input.getPriceChange()
          // Add more normalized fields here as needed
        };
    NDManager manager = ctx.getNDManager();
    NDArray array = manager.create(features);
    return new NDList(array);
  }

  @Override
  public TradeRecommendation processOutput(TranslatorContext ctx, NDList list) {
    NDArray output = list.singletonOrThrow();
    float[] predictions = output.toFloatArray();
    TradeRecommendation rec = new TradeRecommendation();
    rec.setAction(predictions[0] > predictions[1] ? "BUY" : "SELL");
    rec.setConfidence(Math.max(predictions[0], predictions[1]));
    return rec;
  }

  @Override
  public Batchifier getBatchifier() {
    return Batchifier.STACK;
  }
}
