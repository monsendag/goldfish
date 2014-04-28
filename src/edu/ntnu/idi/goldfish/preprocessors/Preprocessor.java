package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.mahout.SMPreference;
import edu.ntnu.idi.goldfish.mahout.SMPreferenceArray;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class Preprocessor {
	protected static Set<String> pseudoRatings = new HashSet<>();
	protected final int RATING_INDEX = 0;
	protected final int TIME_ON_PAGE_INDEX = 1;
	protected final int TIME_ON_MOUSE_INDEX = 2;
	protected final int TIME_PAGE_TIMES_MOUSE = 3;
	
	public static boolean isPseudoPref(Preference pref) {
		return pseudoRatings.contains(String.format("%d_%d", pref.getUserID(), pref.getItemID()));
	}

    public static void addPseudoPref(long userID, long itemID) {
        pseudoRatings.add(String.format("%d_%d", userID, itemID));
    }

    public final DataModel preprocess(Config config) throws Exception {
        pseudoRatings.clear();

        DBModel processed = getProcessedModel(config);

        config.set("pseudoRatings", pseudoRatings.size());

        String tempPath = String.format("/tmp/preprocessor-classifier-remove-invalid-%s.csv", Thread.currentThread().hashCode());
        processed.toCSV(tempPath);

        for(String pseudo : pseudoRatings) {
            String[] split = pseudo.split("_");
            long userID = Long.parseLong(split[0]);
            long itemID = Long.parseLong(split[1]);
            processed.removePreference(userID, itemID);
        }
        return new FileDataModel(new File(tempPath));
    }

    public abstract DBModel getProcessedModel(Config config) throws Exception;

    /**
     * Write dataset to file with csv format
     * @param path url with location and filename
     */
    public static void writeDataModelToCsv(DataModel model, String path) {
        try {
            FileWriter writer = new FileWriter(new File(path));

            Iterator<Long> users = model.getUserIDs();
            while (users.hasNext()) {
                long userId = users.next();
                PreferenceArray preferences = model.getPreferencesFromUser(userId);
                Iterator<Preference> it = preferences.iterator();

                float rating = 0;
                long itemId = 0;
                while (it.hasNext()) {
                    Preference p = it.next();
                    rating = p.getValue(); // explicit
                    itemId = p.getItemID(); // item
                    writer.append(String.format("%d,%d,%.0f", userId, itemId, rating));
                    writer.append("\n");
                }
                writer.flush();
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TasteException e) {
            e.printStackTrace();
        }
    }

    public static void writeDatasetToFileExplicit(SMDataModel model, String path) {
        try {
            FileWriter writer = new FileWriter(new File(path));

            Iterator<Long> users = model.getUserIDs();
            while (users.hasNext()) {
                long userId = users.next();
                SMPreferenceArray preferences = model.getSMPreferencesFromUser(userId);
                Iterator<Preference> it = preferences.iterator();

                float rating = 0;
                float readIndex = 0;
                long itemId = 0;
                while (it.hasNext()) {
                    SMPreference p = (SMPreference) it.next();
                    rating = p.getValue(); // explicit
                    if(rating <= 0) continue;
                    itemId = p.getItemID(); // item
                    writer.append(String.format("%d,%d,%.0f", userId, itemId, rating));
                    writer.append("\n");
                }
                writer.flush();
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TasteException e) {
            e.printStackTrace();
        }
    }

}
