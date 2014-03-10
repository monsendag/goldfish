package edu.ntnu.idi.goldfish.mahout;

import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;
import org.apache.mahout.cf.taste.impl.eval.AbstractDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.model.Preference;

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

}
