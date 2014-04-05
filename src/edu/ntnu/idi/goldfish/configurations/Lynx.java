package edu.ntnu.idi.goldfish.configurations;

import edu.ntnu.idi.goldfish.mahout.LynxFactorizer;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;

public class Lynx extends Config {

	
	public Lynx() {
        this
            .set("numFeatures", 10)
            .set("numIterations", 20)
            .set("lambda", 0.01)
            .set("mu0", 0.01)
            .set("decayFactor", 1.0)
            .set("stepOffset", 0) // these next two control 1/steps^forget type annealing
            .set("forgettingExponent", 0.0) // -1 equals even weighting of all examples, 0 means only use
            .set("biasMuRatio", 0.5)
            .set("biasLambdaRatio", 0.1) // The following two should be inversely proportional :)
            .set("learningRate", 0.01) // Learning rate (step size)
            .set("preventOverfitting", 0.1) // Parameter used to prevent overfitting
            .set("randomNoise", 0.01)  // Standard deviation for random initialization of features
            .set("learningRateDecay", 1.0); // Multiplicative decay factor for learning_rate

    }

	public RecommenderBuilder getBuilder() {
        return model -> {
            Factorizer factorizer = new LynxFactorizer(model,
                get("numFeatures"),
                get("lambda"),
                get("numIterations"),
                get("mu0"),
                get("decayFactor"),
                get("stepOffset"),
                get("forgettingExponent"),
                get("biasMuRatio"),
                get("biasLambdaRatio"));

            return new SVDRecommender(model, factorizer);
        };
	}
}
