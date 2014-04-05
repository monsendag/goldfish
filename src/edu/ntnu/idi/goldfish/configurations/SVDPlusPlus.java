package edu.ntnu.idi.goldfish.configurations;

import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDPlusPlusFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;

public class SVDPlusPlus extends Config {

	public SVDPlusPlus() {
		super();

        this
            .set("lerningRate", 10)
            .set("preventOverfitting", 10)
            .set("randomNoise", 10)
            .set("learningRateDecay", 10);
	}

    public RecommenderBuilder getBuilder() {
        return model -> {
            Factorizer factorizer = new SVDPlusPlusFactorizer(model,
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
