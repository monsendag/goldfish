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
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.h2.tools.Server;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

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
    private Table<Record> table = tableByName("yow");
    private Field<Long> userField = fieldByName(Long.class, "userid");
    private Field<Long> itemField = fieldByName(Long.class, "itemid");
    private Field<Long> fbackField = fieldByName(Long.class, "feedback");
    private Field<Float> valueField = fieldByName(Float.class, "value");

    public static void main(String[] args) throws Exception {

        YowModel model = new YowModel(new File("datasets/yow-userstudy/exdupes-like-timeonpage-timeonmouse.csv"));
        Server webServer = Server.createWebServer("-webAllowOthers").start();
        System.out.println("jdbc:h2:mem:"+model.hashCode());
        System.out.printf("users: %d, items: %d", model.getNumUsers(), model.getNumItems());
    }

    public YowModel(File f) throws Exception {
        initDB();
        parseFile(f);
    }

    public YowModel(FastByIDMap<PreferenceArray> userData) {
        initDB();
        Field[] fields = new Field[]{userField, itemField, fbackField, valueField};
        Object[] values;

        LongPrimitiveIterator userIter = userData.keySetIterator();
        while(userIter.hasNext()) {
            long userID = userIter.next();
            PreferenceArray prefs = userData.get(userID);
            for(Preference p : prefs) {
                values = new Object[]{p.getUserID(), p.getItemID(), EXPLICIT, p.getValue()};
                context.insertInto(table, fields).values(values).execute();
            }
        }
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
            context.insertInto(table, fields).values(new Object[]{userID, itemID, EXPLICIT, explicit}).execute();
        }
        if(timeonpage > 0) {
            context.insertInto(table, fields).values(new Object[]{userID, itemID, TIMEONPAGE, timeonpage}).execute();
        }
        if(timeonmouse > 0) {
            context.insertInto(table, fields).values(new Object[]{userID, itemID, TIMEONMOUSE, timeonmouse}).execute();
        }
    }

    private void initDB() {
        try {
            // load driver
            Class.forName("org.h2.Driver");
            Connection conn = DriverManager.getConnection("jdbc:h2:mem:"+hashCode());
            context = DSL.using(conn, SQLDialect.MYSQL);

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
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                .from(table).fetchArray(fbackField);
        return new LongPrimitiveArrayIterator(ArrayUtils.toPrimitive(fbackIDs));
    }

    public LongPrimitiveIterator getUserIDs() throws TasteException {
        Long[] userIDs = context.selectDistinct(userField)
                .from(table).fetchArray(userField);
        return new LongPrimitiveArrayIterator(ArrayUtils.toPrimitive(userIDs));
    }

    public LongPrimitiveIterator getItemIDs() throws TasteException {
        Long[] itemID = context.selectDistinct(itemField)
                .from(table).fetchArray(itemField);
        return new LongPrimitiveArrayIterator(ArrayUtils.toPrimitive(itemID));
    }

    public PreferenceArray getPreferencesFromUserFeedback(long userID, long fbackID) throws TasteException {
        List<? extends Preference> prefs = context.select(fbackField, itemField, valueField).from(table)
                .where(userField.equal(userID))
                .and(fbackField.equal(fbackID))
                .fetch().into(GenericPreference.class);
        return new GenericUserPreferenceArray(prefs);
    }

    public PreferenceArray getPreferencesFromUser(long userID) throws TasteException {
        List<? extends Preference> prefs = context.select(userField, itemField, valueField).from(table)
                .where(userField.equal(userID))
                .and(fbackField.equal(EXPLICIT))
                .fetch().into(GenericPreference.class);
        return new GenericUserPreferenceArray(prefs);
    }

    public PreferenceArray getPreferencesForItem(long itemID) throws TasteException {
        List<? extends Preference> prefs = context.select(userField, itemField, valueField).from(table)
                .where(itemField.equal(itemID))
                .and(fbackField.equal(EXPLICIT))
                .fetch().into(GenericPreference.class);
        return new GenericItemPreferenceArray(prefs);
    }

    public FastIDSet getItemIDsFromUser(long userID) throws TasteException {
        // fetch all itemIDs for specific user, return a fastIDSet!
        List<Long> itemIDs = context.selectDistinct(itemField).from(table).where(userField.equal(userID)).fetch(itemField);
        FastIDSet set = new FastIDSet();
        itemIDs.stream().forEach(set::add);
        return set;
    }



    @Override
    public Float getPreferenceValue(long userID, long itemID) throws TasteException {
        return context.select(valueField).from(table)
                .where(userField.equal(userID)
                        .and(itemField.equal(itemID))
                        .and(fbackField.equal(EXPLICIT)))
                .fetchOne(valueField);
    }

    @Override
    public Long getPreferenceTime(long userID, long itemID) throws TasteException {
        return null;
    }

    @Override
    public int getNumItems() throws TasteException {
        return context.selectCount().from(table).groupBy(itemField).fetchCount();
    }

    @Override
    public int getNumUsers() throws TasteException {
        return context.selectCount().from(table).groupBy(userField).fetchCount();
    }

    @Override
    public int getNumUsersWithPreferenceFor(long itemID) throws TasteException {
        return context.selectDistinct(userField).from(table)
                .where(itemField.equal(itemID))
                .groupBy(userField).fetchCount();
    }

    @Override
    public int getNumUsersWithPreferenceFor(long itemID1, long itemID2) throws TasteException {
        return context.selectDistinct(userField).from(table)
                .where(itemField.equal(itemID1)
                        .or(itemField.equal(itemID2))).fetchCount();
    }

    @Override
    public void setPreference(long userID, long itemID, float value) throws TasteException {
        context.mergeInto(table, userField, itemField, fbackField, valueField).key(userField, itemField, fbackField).values(userID, itemID, EXPLICIT, value);
    }

    @Override
    public void removePreference(long userID, long itemID) throws TasteException {
        context.delete(table).where(userField.equal(userID).and(itemField.equal(itemID)));
    }

    @Override
    public boolean hasPreferenceValues() {
        return true;
    }

    @Override
    public float getMaxPreference() {
        return context.select(DSL.max(valueField)).from(table).fetchOne(DSL.max(valueField));
    }

    @Override
    public float getMinPreference() {
        return context.select(DSL.min(valueField)).from(table).fetchOne(DSL.min(valueField));
    }

    @Override
    public void refresh(Collection<Refreshable> alreadyRefreshed) {
        // reload file? Reload from mongo?
    }

    public DataModel toMemory() throws TasteException {
        return new GenericDataModel(getUserData());
    }

    public Map<Long, Float> getFeedback() {
        return context.select(valueField).from(table).groupBy(userField, itemField, fbackField).fetchMap(fbackField, valueField);
    }

    public Result<Record5<Long, Long, Float, Float, Float>> getFeedbackRows() {
        return context.select(userField, itemField, valueField, valueField, valueField).groupBy(userField, itemField).fetch();
    }
}
