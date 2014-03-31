package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.mahout.SMPreference;
import edu.ntnu.idi.goldfish.mahout.SMPreferenceArray;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

public abstract class Preprocessor {

    public abstract void preprocess(YowModel model);

    /**
     * Write dataset to file with csv format
     *
     * @param filename
     *            url with location and filename
     */
    public static void writeModel(SMDataModel model, String path) {
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
                    rating = p.getValue(0); // explicit
                    readIndex = p.getValue(1); // readindex
                    itemId = p.getItemID(); // item
                    writer.append(String.format("%d,%d,%.0f,%.1f", userId, itemId, rating, readIndex));
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
                    itemId = p.getItemID(); // item
                    writer.append(String.format("%d,%d,%f", userId, itemId, rating));
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
