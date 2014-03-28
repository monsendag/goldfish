package edu.ntnu.idi.goldfish.preprocessors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveArrayIterator;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericItemPreferenceArray;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import static org.jooq.impl.DSL.fieldByName;
import static org.jooq.impl.DSL.tableByName;

public class YowModel implements DataModel {

    static final long EXPLICIT = 0;
    static final long TIMEONPAGE = 1;
    static final long TIMEONMOUSE = 2;

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    DSLContext context;
    private Table<Record> DB = tableByName("yow");
    private Field<Long> userField = fieldByName(Long.class, "userid");
    private Field<Long> itemField = fieldByName(Long.class, "itemid");
    private Field<Long> fbackField = fieldByName(Long.class, "feedback");
    private Field<Float> valueField = fieldByName(Float.class, "value");

    public static void main(String[] args) throws Exception {
        File file = new File("datasets/yow-userstudy/ratings-fixed.csv");
        DataModel fm = new FileDataModel(file);
        DataModel ym = new YowModel(file);

        LongPrimitiveIterator users = fm.getUserIDs();
        while(users.hasNext()) {
            long userid = users.nextLong();
            PreferenceArray fprefs = fm.getPreferencesFromUser(userid);
            PreferenceArray yprefs = ym.getPreferencesFromUser(userid);

            fprefs.sortByItem();
            yprefs.sortByItem();

            Iterator<Preference> i1 = fprefs.iterator();
            Iterator<Preference> i2 = yprefs.iterator();

            while(i1.hasNext()) {
                Preference p1 = i1.next();
                Preference p2 = i2.next();

                if(p1.getValue() != p2.getValue() || p1.getUserID() != p2.getUserID() || p1.getItemID() != p2.getItemID()) {
                    System.out.println("koko");
                }
            }
        }
    }

    public YowModel(File f) throws Exception {
        // load driver
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:yow;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        context = DSL.using(conn, SQLDialect.MYSQL);

        //Server webServer = Server.createWebServer("-webAllowOthers").start();

        initTable();
        parseFile(f);

        System.out.println(getNumUsers());

        LongPrimitiveIterator users = getUserIDs();

        while(users.hasNext()) {
            long userID = users.next();

            LongPrimitiveIterator fbackIDs = getFeedbackIDs();

            FastByIDMap<PreferenceArray> feedbackData = new FastByIDMap<>();

            while(fbackIDs.hasNext()) {
                long fbackID = fbackIDs.next();
                feedbackData.put(fbackID, getPreferencesFromUserFeedback(userID, fbackID));
            }
            // create pref X item matrix
            DataModel model = new GenericDataModel(feedbackData);

            // do recommendation on model -- Fill all data for "user" EXPLICIT
            // model preference name as user id
        }

        // merge all datamodels to final model -> return it

    }

    private void parseFile(File f) throws FileNotFoundException {
        Scanner sc = new Scanner(f);
        while (sc.hasNextLine()) {
            parseLine(sc.nextLine());
        }
    }

    private void parseLine(String line) {
        // Ignore empty lines and comments
        if (line.isEmpty() || line.charAt(0) == '#') {
            return;
        }

        Scanner sc = new Scanner(line).useDelimiter(",");

        long userID = sc.nextLong();
        long itemID = sc.nextLong();

        float explicit = sc.nextFloat();
        float timeonpage = sc.nextFloat();
        float timeonmouse = sc.nextFloat();

        Field[] fields = new Field[]{userField, itemField, fbackField, valueField};

        if (explicit > 0) {
            context.insertInto(DB, fields).values(new Object[]{userID, itemID, EXPLICIT, explicit}).execute();
        }
        if(timeonpage > 0) {
            context.insertInto(DB, fields).values(new Object[]{userID, itemID, TIMEONPAGE, timeonpage}).execute();
        }
        if(timeonmouse > 0) {
            context.insertInto(DB, fields).values(new Object[]{userID, itemID, TIMEONMOUSE, timeonmouse}).execute();
        }
    }

    private void initTable() {
        // initialize db
        context.query("CREATE TABLE IF NOT EXISTS yow (" +
                "userid INT NOT NULL, " +
                "itemid INT NOT NULL, " +
                "feedback INT NOT NULL, " +
                "value INT NOT NULL " +
                "" +
                ")").execute();

        // set indices
        context.query("CREATE INDEX uindex ON yow (userid)").execute();
        context.query("CREATE INDEX iindex ON yow (itemid)").execute();
    }

    public FastByIDMap<PreferenceArray> getUserData() throws TasteException {
        FastByIDMap<PreferenceArray> userData = new FastByIDMap<>();
        LongPrimitiveIterator userids = getUserIDs();
        while(userids.hasNext()) {
            long userid = userids.nextLong();
            userData.put(userid, getPreferencesFromUser(userid));
        }
        return userData;
    }

    public LongPrimitiveIterator getFeedbackIDs() throws TasteException {
        Long[] fbackIDs = context.selectDistinct(fbackField)
                .from(DB).fetchArray(fbackField);
        return new LongPrimitiveArrayIterator(ArrayUtils.toPrimitive(fbackIDs));
    }

    public LongPrimitiveIterator getUserIDs() throws TasteException {
        Long[] userIDs = context.selectDistinct(userField)
                .from(DB).fetchArray(userField);
        return new LongPrimitiveArrayIterator(ArrayUtils.toPrimitive(userIDs));
    }

    public LongPrimitiveIterator getItemIDs() throws TasteException {
        Long[] itemID = context.selectDistinct(itemField)
                .from(DB).fetchArray(itemField);
        return new LongPrimitiveArrayIterator(ArrayUtils.toPrimitive(itemID));
    }

    public PreferenceArray getPreferencesFromUserFeedback(long userID, long fbackID) throws TasteException {
        List<? extends Preference> prefs = context.select(fbackField, itemField, valueField).from(DB)
                .where(userField.equal(fbackID))
                .and(fbackField.equal(fbackID))
                .fetch().into(GenericPreference.class);
        return new GenericUserPreferenceArray(prefs);
    }

    public PreferenceArray getPreferencesFromUser(long userID) throws TasteException {
        List<? extends Preference> prefs = context.select(userField, itemField, valueField).from(DB)
                .where(userField.equal(userID))
                .and(fbackField.equal(EXPLICIT))
                .fetch().into(GenericPreference.class);
        return new GenericUserPreferenceArray(prefs);
    }

    public PreferenceArray getPreferencesForItem(long itemID) throws TasteException {
        List<? extends Preference> prefs = context.select().from(DB)
                .where(itemField.equal(itemID))
                .and(fbackField.equal(EXPLICIT))
                .fetch().into(GenericPreference.class);
        return new GenericItemPreferenceArray(prefs);
    }

    public FastIDSet getItemIDsFromUser(long userID) throws TasteException {
        // fetch all itemIDs for specific user, return a fastIDSet!
        List<Long> itemIDs = context.selectDistinct(itemField).from(DB).where(userField.equal(userID)).fetch(itemField);
        FastIDSet set = new FastIDSet();
        itemIDs.stream().forEach(set::add);
        return set;
    }


    @Override
    public Float getPreferenceValue(long userID, long itemID) throws TasteException {
        return context.selectOne().from(DB)
                .where(userField.equal(userID)
                        .and(itemField.equal(itemID)))
                .and(fbackField.equal(EXPLICIT))
                .fetchOne(valueField);
    }

    @Override
    public Long getPreferenceTime(long userID, long itemID) throws TasteException {
        return null;
    }

    @Override
    public int getNumItems() throws TasteException {
        return context.selectCount().from(DB).groupBy(itemField).fetchCount();
    }

    @Override
    public int getNumUsers() throws TasteException {
        return context.selectCount().from(DB).groupBy(userField).fetchCount();
    }

    @Override
    public int getNumUsersWithPreferenceFor(long itemID) throws TasteException {
        return context.selectDistinct(userField).from(DB)
                .where(itemField.equal(itemID))
                .groupBy(userField).fetchCount();
    }

    @Override
    public int getNumUsersWithPreferenceFor(long itemID1, long itemID2) throws TasteException {
        return context.selectDistinct(userField).from(DB)
                .where(itemField.equal(itemID1)
                        .or(itemField.equal(itemID2))).fetchCount();
    }

    @Override
    public void setPreference(long userID, long itemID, float value) throws TasteException {

    }

    @Override
    public void removePreference(long userID, long itemID) throws TasteException {
        context.delete(DB).where(userField.equal(userID).and(itemField.equal(itemID)));
    }

    @Override
    public boolean hasPreferenceValues() {
        return true;
    }

    @Override
    public float getMaxPreference() {
        return context.select(DSL.max(valueField)).fetchOne(valueField);
    }

    @Override
    public float getMinPreference() {
        return context.select(DSL.min(valueField)).fetchOne(valueField);
    }

    @Override
    public void refresh(Collection<Refreshable> alreadyRefreshed) {
        // reload file? Reload from mongo?
    }
}
