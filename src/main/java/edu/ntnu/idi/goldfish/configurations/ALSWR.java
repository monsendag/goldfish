package edu.ntnu.idi.goldfish.configurations;

import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;

public class ALSWR extends Config {


	public ALSWR() {
		super();

        this
            .set("numFeatures", 10)
            .set("lambda", 10)
            .set("numIterations", 10)
            .set("usesImplicitFeedback", 10)
            .set("alpha", 10);

	}

    public RecommenderBuilder getBuilder() {
        return model -> {
            Factorizer factorizer = new ALSWRFactorizer(model,
                    get("numFeatures"),
                    get("lambda"),
                    get("numIterations"),
                    get("usesImplicitFeedback"),
                    get("alpha"));

            return new SVDRecommender(model, factorizer);
        };
    }

}
