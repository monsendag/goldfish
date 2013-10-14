package edu.ntnu.idi;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
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
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class Algorithms {
	
	public static GenericUserBasedRecommender userBasedRecommender(String ratings, int neighborhoodSize, String similarityMetric) 
			throws TasteException, IOException {
		FileDataModel model = new GroupLensDataModel(new File(ratings));
		
		UserSimilarity userSimilarity = selectSimilarity(similarityMetric, model);
		
		UserNeighborhood neighborhood = new NearestNUserNeighborhood(neighborhoodSize, userSimilarity, model);
		
		System.out.format("\n A user-based recommendation algorithm was created. \n Similarity metric is %s. \n Size of " +
				"neighborhood is %d \n", similarityMetric, neighborhoodSize);
		
		return new GenericUserBasedRecommender(model, neighborhood, userSimilarity);
	}
	
	public static GenericItemBasedRecommender itemBasedRecommender (String ratings, String similarityMetric) throws TasteException, IOException {
		FileDataModel model = new GroupLensDataModel(new File(ratings));
		
		UserSimilarity userSimilarity = selectSimilarity(similarityMetric, model);
		
		System.out.format("\n An item-based recommendation algorithm was created. \n Similarity metric is %s. \n", similarityMetric);
		
		return new GenericItemBasedRecommender(model, (ItemSimilarity) userSimilarity);
	}
	
	public static SlopeOneRecommender slopeOneRecommender (String ratings) throws IOException, TasteException {
		FileDataModel model = new GroupLensDataModel(new File(ratings));
		
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
