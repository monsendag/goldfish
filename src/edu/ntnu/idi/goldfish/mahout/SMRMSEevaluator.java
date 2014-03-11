package edu.ntnu.idi.goldfish.mahout;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.eval.AbstractDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AbstractDifferenceRecommenderEvaluator.PreferenceEstimateCallable;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.Recommender;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import edu.ntnu.idi.goldfish.Preprocessor;

public class SMRMSEevaluator extends AbstractDifferenceRecommenderEvaluator {

	private RunningAverage average;

	@Override
	protected void reset() {
		average = new FullRunningAverage();
	}

	@Override
	protected void processOneEstimate(float estimatedPreference, Preference realPref) {
		double diff = realPref.getValue() - estimatedPreference;
		average.addDatum(diff * diff);
	}

	@Override
	protected double computeFinalEvaluation() {
		return Math.sqrt(average.getAverage());
	}

	@Override
	public String toString() {
		return "RMSRecommenderEvaluator";
	}

	@Override
	public double evaluate(RecommenderBuilder recommenderBuilder, DataModelBuilder dataModelBuilder,
			DataModel dataModel, double trainingPercentage, double evaluationPercentage) throws TasteException {
		Preconditions.checkNotNull(recommenderBuilder);
		Preconditions.checkNotNull(dataModel);
		Preconditions.checkArgument(trainingPercentage >= 0.0 && trainingPercentage <= 1.0,
				"Invalid trainingPercentage: " + trainingPercentage);
		Preconditions.checkArgument(evaluationPercentage >= 0.0 && evaluationPercentage <= 1.0,
				"Invalid evaluationPercentage: " + evaluationPercentage);


		int numUsers = dataModel.getNumUsers();
		FastByIDMap<PreferenceArray> trainingPrefs = new FastByIDMap<PreferenceArray>(
				1 + (int) (evaluationPercentage * numUsers));
		FastByIDMap<PreferenceArray> testPrefs = new FastByIDMap<PreferenceArray>(
				1 + (int) (evaluationPercentage * numUsers));

		LongPrimitiveIterator it = dataModel.getUserIDs();
		while (it.hasNext()) {
			long userID = it.nextLong();
			splitOneUsersPrefs(trainingPercentage, trainingPrefs, testPrefs, userID, dataModel);
		}

		DataModel trainingModel = dataModelBuilder == null ? new GenericDataModel(trainingPrefs) : dataModelBuilder
				.buildDataModel(trainingPrefs);

//		System.out.println(String.format("Training set: %d users, %d items", trainingModel.getNumUsers(), trainingModel.getNumItems()));
//		System.out.println(String.format("Test set: %d prefs", testPrefs.size()));
		
		Recommender recommender = recommenderBuilder.buildRecommender(trainingModel);

		double result = getEvaluation(testPrefs, recommender);
		return result;
	}
	
	  private double getEvaluation(FastByIDMap<PreferenceArray> testPrefs, Recommender recommender)
			    throws TasteException {
			    reset();
			    Collection<Callable<Void>> estimateCallables = Lists.newArrayList();
			    AtomicInteger noEstimateCounter = new AtomicInteger();
			    for (Map.Entry<Long,PreferenceArray> entry : testPrefs.entrySet()) {
			      estimateCallables.add(
			          new PreferenceEstimateCallable(recommender, entry.getKey(), entry.getValue(), noEstimateCounter));
			    }
			    RunningAverageAndStdDev timing = new FullRunningAverageAndStdDev();
			    execute(estimateCallables, noEstimateCounter, timing);
			    return computeFinalEvaluation();
			  }

	protected void splitOneUsersPrefs(double trainingPercentage, FastByIDMap<PreferenceArray> trainingPrefs,
			FastByIDMap<PreferenceArray> testPrefs, long userID, DataModel dataModel) throws TasteException {
		List<Preference> oneUserTrainingPrefs = null;
		List<Preference> oneUserTestPrefs = null;
		PreferenceArray prefs = dataModel.getPreferencesFromUser(userID);
		int size = prefs.length();
		
//		int numberOfTestPrefs = 0;
//		int numberOfTrainingPrefs = 0;
//		int numberOfPseudoRatings = 0;

		for (int i = 0; i < size; i++) {
			Preference newPref = new GenericPreference(userID, prefs.getItemID(i), prefs.getValue(i));

			boolean isPseudo = Preprocessor.isPseudoPreference(newPref);
//			if(isPseudo) numberOfPseudoRatings++;
			// @TODO keep pseudoratings in training set while keeping correct
			// trainingPercentage

			if (Math.random() < trainingPercentage || isPseudo) {
				if (oneUserTrainingPrefs == null) {
					oneUserTrainingPrefs = Lists.newArrayListWithCapacity(3);
				}
				oneUserTrainingPrefs.add(newPref);
//				numberOfTestPrefs++;
			} else {
				if (oneUserTestPrefs == null) {
					oneUserTestPrefs = Lists.newArrayListWithCapacity(3);
				}
				oneUserTestPrefs.add(newPref);
//				numberOfTrainingPrefs++;
			}
		}
		if (oneUserTrainingPrefs != null) {
			trainingPrefs.put(userID, new GenericUserPreferenceArray(oneUserTrainingPrefs));
//			numberOfTrainingPrefs++;
			if (oneUserTestPrefs != null) {
				testPrefs.put(userID, new GenericUserPreferenceArray(oneUserTestPrefs));
//				numberOfTestPrefs++;
			}
		}
		
//		System.out.println(String.format("Test set: %d prefs", numberOfTestPrefs));
//		System.out.println(String.format("Training set: %d prefs", numberOfTrainingPrefs));
//		System.out.println(String.format("User %d har %d pseudo ratings", userID, numberOfPseudoRatings));
		
	}

}
