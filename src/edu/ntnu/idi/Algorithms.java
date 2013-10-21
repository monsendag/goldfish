package edu.ntnu.idi;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.example.grouplens.GroupLensDataModel;
import org.apache.mahout.cf.taste.hadoop.slopeone.SlopeOneAverageDiffsJob;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.CachingRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.slopeone.SlopeOneRecommender;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class Algorithms {
	
	public static GenericUserBasedRecommender userBasedRecommender(DataModel dataModel, int neighborhoodSize, UserSimilarity userSimilarity) 
			throws TasteException, IOException {
		DataModel model = dataModel;
		
		UserSimilarity similarity = userSimilarity;
		
		UserNeighborhood neighborhood = new NearestNUserNeighborhood(neighborhoodSize, userSimilarity, model);
		
		System.out.format("\n A user-based recommendation algorithm was created. \n Similarity metric is %s. \n Size of " +
				"neighborhood is %d \n", userSimilarity.toString(), neighborhoodSize);
		
		return new GenericUserBasedRecommender(model, neighborhood, userSimilarity);
	}
	
	public static GenericItemBasedRecommender itemBasedRecommender (DataModel dataModel, ItemSimilarity itemSimilarity) 
			throws TasteException, IOException {
		DataModel model = dataModel;
		
		ItemSimilarity similarity = itemSimilarity;
		
		System.out.format("\n An item-based recommendation algorithm was created. \n Similarity metric is %s. \n", itemSimilarity.toString());
		
		return new GenericItemBasedRecommender(model, similarity);
	}
	
	public static SlopeOneRecommender slopeOneRecommender (DataModel dataModel) throws IOException, TasteException {
		DataModel model = dataModel;
		
		System.out.println(" \n A slope-one recommender algorithm was created.");
		
		return new SlopeOneRecommender(model);
	}
	
	public static UserSimilarity selectSimilarity(String similarityMetric, FileDataModel model) throws TasteException {
		UserSimilarity userSimilarity = null;
		
		if(similarityMetric.equals("pearson")){
			userSimilarity = new PearsonCorrelationSimilarity(model);			
		} else if (similarityMetric.equals("euclidean")) {
			userSimilarity = new EuclideanDistanceSimilarity(model);
		} else if (similarityMetric.equals("cosine")) {
			userSimilarity = new PearsonCorrelationSimilarity(model);
		} else if (similarityMetric.equals("spearman")) {
			userSimilarity = new SpearmanCorrelationSimilarity(model);
		} else if (similarityMetric.equals("tanimoto")) {
			userSimilarity = new TanimotoCoefficientSimilarity(model);
		} else if (similarityMetric.equals("loglikelihood")) {
			userSimilarity = new LogLikelihoodSimilarity(model);
		} else { 
			// Default: Pearson correlation similarity
			userSimilarity = new PearsonCorrelationSimilarity(model);
		}
		
		return userSimilarity;
	}

	public static List<RecommendedItem> getRecommendations(int numberOfRatings, int userId, Recommender recommender) throws TasteException{
		Recommender cachingRecommender = new CachingRecommender(recommender);
		
		List<RecommendedItem> recommendations = cachingRecommender.recommend(userId, numberOfRatings);
	
		return recommendations;
	}
}
