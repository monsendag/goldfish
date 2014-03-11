package edu.ntnu.idi.goldfish;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.correlation.*;
import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.mahout.SMPreference;

public class Preprocessor {

	private final int THRESHOLD = 2;
	private Map<String, Float> correlations = new HashMap<String, Float>();
	private static Set<String> pseudoRatings = new HashSet<String>();

	public static void main(String[] args) throws IOException, TasteException {
		// pre.readFile("datasets/yow-userstudy/like-timeonpage-timeonmouse.csv");

		Preprocessor.getPreprocessedDataModel("datasets/yow-userstudy/like-timeonpage-timeonmouse.csv");

		// pre.calculateRMSE();
	}

	public static DataModel getPreprocessedDataModel(String path) throws TasteException, IOException {
		SMDataModel model;
		model = new SMDataModel(new File(path));
		Preprocessor pre = new Preprocessor();
		pre.preprocess(model);
		return model;
	}

	public Preprocessor() {

	}

	public static boolean isPseudoPreference(Preference pref) {
		return pseudoRatings.contains(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
	}

	public void preprocess(SMDataModel model) throws TasteException {
		// iterate through all users
		LongPrimitiveIterator it = model.getItemIDs();
		while (it.hasNext()) {
			long itemID = it.next();

			// iterate through all prefs for user
			PreferenceArray prefs = model.getPreferencesForItem(itemID);
			for (Preference p : prefs) {
				SMPreference pref = (SMPreference) p;
				boolean hasExplicit = pref.getValue(0) >= 1;

				if (!hasExplicit) {

					boolean hasImplicit = false;
					float[] vals = pref.getValues();
					for (int i = 1; i < vals.length; i++) {
						if (vals[i] >= 1) {
							hasImplicit = true;
							break;
						}
					}
					// iterate through implicit values and use the one with
					// highest correlation

					// find out if we have enough explicit-implicit rating
					// pars
					if (hasImplicit && prefs.length() >= THRESHOLD) {

						int bestCorrelated = -1;
						for (int i = 1; i < vals.length; i++) {
							if (bestCorrelated == -1
									|| getCorrelation(model, itemID, i) > getCorrelation(model, itemID, bestCorrelated)) {
								bestCorrelated = i;
							}
						}

						// check if abs(correlation) > 0.5
						double correlation = getCorrelation(model, itemID, bestCorrelated);
						if (Math.abs(correlation) > 0.5) {
							float pseudoRating = getPseudoRatingClosestNeighbor(prefs, pref, bestCorrelated);
							pref.setValue(pseudoRating, 0); // set explicit
															// value
							pseudoRatings.add(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
						}
					}
				}
			}
		}
	}

	private float getPseudoRatingClosestNeighbor(PreferenceArray prefs, SMPreference currentPref, int bestCorrelated) {
		float diff = Float.MAX_VALUE;
		SMPreference closestPref = null;

		System.out.println(bestCorrelated);
		for (Preference pref : prefs) {
			SMPreference p = (SMPreference) pref;

			float tempDiff = Math.abs(currentPref.getValue(1) - p.getValue(bestCorrelated));
			if (tempDiff < diff) {
				diff = tempDiff;
				closestPref = p;
			}
		}

		return closestPref.getValue(0);
	}

	public double getCorrelation(SMDataModel model, long itemID, int implicitIndex) {
		PreferenceArray prefs;
		try {
			prefs = model.getPreferencesForItem(itemID);

			double[] expl = new double[prefs.length()];
			double[] impl = new double[prefs.length()];

			for (int i = 0; i < prefs.length(); i++) {
				SMPreference p = (SMPreference) prefs.get(i);
				expl[i] = p.getValue(0);
				impl[i] = p.getValue(implicitIndex);
			}

			PearsonsCorrelation pc = new PearsonsCorrelation();
			return pc.correlation(expl, impl);

		} catch (NoSuchItemException e) {
			e.printStackTrace();
			return -1;
		}
	}

}
