package edu.ntnu.idi.goldfish.configurations;

import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.RatingSGDFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;

public class RatingSGD extends Config {

    public RatingSGD() {
        this
                .set("numFeatures", 10)
                .set("numIterations", 20)
                .set("learningRate", 0.01) // Learning rate (step size)
                .set("preventOverfitting", 0.1) // Parameter used to prevent overfitting
                .set("randomNoise", 0.01)  // Standard deviation for random initialization of features
                .set("learningRateDecay", 1.0); // Multiplicative decay factor for learning_rate
    }

    public RecommenderBuilder getBuilder() {
        return model -> {
            Factorizer factorizer = new RatingSGDFactorizer(model,
                    get("numFeatures"),
                    get("learningRate"),
                    get("preventOverfitting"),
                    get("randomNoise"),
                    get("numIterations"),
                    get("learningRateDecay"));

            return new SVDRecommender(model, factorizer);
        };
    }
}
