package edu.ntnu.idi.goldfish.configurations;

import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class Threshold extends Config {

	public Threshold() {
        super();
        this.set("threshold", 0.4);
	}

    public RecommenderBuilder getBuilder() {
        return model -> {
            UserSimilarity similarity = get("similarity");
            UserNeighborhood neighborhood = new ThresholdUserNeighborhood(get("threshold"), similarity, model);

            if(model instanceof GenericBooleanPrefDataModel) {
                return new GenericBooleanPrefUserBasedRecommender(model, neighborhood, similarity);
            }
            return new GenericUserBasedRecommender(model, neighborhood, similarity);
        };
    }
}

