package edu.ntnu.idi.goldfish.configurations;

import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class KNN extends Config {

	public KNN() {
        this.set("neighborhoodSize", 2);
    }

    public RecommenderBuilder getBuilder() {
        return model -> {
            UserSimilarity similarity = get("similarity");
            UserNeighborhood neighborhood = new NearestNUserNeighborhood(get("neighborhoodSize"), similarity, model);

            if(model instanceof GenericBooleanPrefDataModel) {
                return new GenericBooleanPrefUserBasedRecommender(model, neighborhood, similarity);
            }
            return new GenericUserBasedRecommender(model, neighborhood, similarity);
        };
    }
}