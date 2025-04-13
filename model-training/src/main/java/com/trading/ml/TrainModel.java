package com.trading.ml;

import ai.djl.Model;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.djl.training.dataset.RandomAccessDataset;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.translate.TranslateException;
import com.trading.ml.dataset.InMemoryDataset;
import com.trading.ml.translate.StockFeatureTranslator;
import com.trading.model.StockFeatures;
import com.trading.model.TradeRecommendation;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class TrainModel {

  public static void main(String[] args) throws IOException, TranslateException {
    try (Model model = Model.newInstance("trade-recommender")) {
      Block block =
          new SequentialBlock()
              .add(Linear.builder().setUnits(10).build())
              .add(Linear.builder().setUnits(3).build());
      model.setBlock(block);

      DefaultTrainingConfig config =
          new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
              .optOptimizer(Optimizer.sgd().build())
              .addTrainingListeners(TrainingListener.Defaults.logging());

      try (Trainer trainer = model.newTrainer(config)) {
        trainer.setMetrics(new Metrics());
        trainer.initialize(new Shape(1, 10));

        List<StockFeatures> features = SampleLoader.loadFeatures();
        List<TradeRecommendation> labels = SampleLoader.loadLabels();

        RandomAccessDataset dataset =
            new InMemoryDataset(features, labels, new StockFeatureTranslator());

        for (int epoch = 0; epoch < 3; epoch++) {
          for (Batch batch : trainer.iterateDataset(dataset)) {
            EasyTrain.trainBatch(trainer, batch);
            trainer.step();
            batch.close();
          }
          trainer.notifyListeners(listener -> listener.onEpoch(trainer));
        }

        model.save(Paths.get("shared-models/onnx/"), "trade-recommender");
      }
    }
  }
}
