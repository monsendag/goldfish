package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.Recommender;

public class PreprocessorMF extends Preprocessor {

    @Override
    public DataModel preprocess(Config config) throws TasteException {

        DBModel model = config.get("model");
        LongPrimitiveIterator userIter = model.getUserIDs();

        FastByIDMap<PreferenceArray> userData = new FastByIDMap<>();

        // iterate through each user slice
        while(userIter.hasNext()) {
            long userID = userIter.next();

            LongPrimitiveIterator fbackIter = model.getFeedbackIDs();
            FastByIDMap<PreferenceArray> feedbackData = new FastByIDMap<>();

            // create fastByIDMap<PreferenceArray> for each feedback row
            while(fbackIter.hasNext()) {
                long fbackID = fbackIter.next();
                feedbackData.put(fbackID, model.getPreferencesFromUserFeedback(userID, fbackID));
            }

            // create feedback/item matrix where we model feedback as users
            DataModel prefModel = new DBModel(feedbackData);

//            System.out.printf("user: %d  items: %d  0: %d  1: %d  2:%d \n", userID, prefModel.getNumItems(), prefModel.getPreferencesFromUser(0).length(), prefModel.getPreferencesFromUser(1).length(), prefModel.getPreferencesFromUser(2).length());

            // create recommeder and estimate explicit ratings for each model
//            Recommender rec = new SVDRecommender(prefModel, new ParallelSGDFactorizer(prefModel, 2, 0.01, 5));
            Recommender rec = new SVDRecommender(prefModel, new ALSWRFactorizer(prefModel, 6, 0.02, 10));
//            Recommender rec = new SVDRecommender(prefModel, new SVDPlusPlusFactorizer(prefModel, 5, 3));
//            Recommender rec = new GenericUserBasedRecommender(prefModel, new NearestNUserNeighborhood(2, new TanimotoCoefficientSimilarity(prefModel),  prefModel),  new TanimotoCoefficientSimilarity(prefModel));
            LongPrimitiveIterator itemIter = prefModel.getItemIDs();
            while(itemIter.hasNext()) {
                long itemID = itemIter.next();

                if(prefModel.getPreferenceValue(DBModel.EXPLICIT, itemID) == null) {
                    pseudoRatings.add(String.format("%d_%d", DBModel.EXPLICIT, itemID));
                    float pref = rec.estimatePreference(DBModel.EXPLICIT, itemID);
                    System.out.printf("estimated: user: %d, item: %d, pref: %.2f\n", userID, itemID, pref);
                    prefModel.setPreference(DBModel.EXPLICIT, itemID, pref);
                }
            }

            PreferenceArray explicitPrefs = prefModel.getPreferencesFromUser(DBModel.EXPLICIT);
            explicitPrefs.setUserID(0, userID);
            userData.put(userID, explicitPrefs);
        }
        return new GenericDataModel(userData);
    }
}
