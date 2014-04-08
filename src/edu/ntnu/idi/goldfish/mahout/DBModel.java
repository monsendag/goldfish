package edu.ntnu.idi.goldfish.mahout;

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
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.fieldByName;
import static org.jooq.impl.DSL.tableByName;

public class DBModel implements DataModel {

    public static final long EXPLICIT = 0;
    public static final long TIMEONPAGE = 1;
    public static final long TIMEONMOUSE = 2;

    private static final long serialVersionUID = 1L;
    DSLContext context;
    private Table<Record> table = tableByName("yow");
    private Field<Long> userField = fieldByName(long.class, "userid");
    private Field<Long> itemField = fieldByName(long.class, "itemid");
    private Field<Long> fbackField = fieldByName(long.class, "feedback");
    private Field<Float> valueField = fieldByName(Float.class, "value");
    private Field<Float> explicitField = fieldByName(Float.class, "explicit");
    private Field<Float> pageField = fieldByName(Float.class, "timeonpage");
    private Field<Float> mouseField = fieldByName(Float.class, "timeonmouse");

    public static void main(String[] args) throws Exception {
        DBModel model = new DBModel(new File("datasets/yow-userstudy/exdupes-like-timeonpage-timeonmouse.csv"));

        model.setPreference(5415, 12, 4L);

        model.setPreference(5415, 12, 2L);
        System.out.println(model.getPreferenceValue(5415, 12));
    }

    public void startServer() throws SQLException {
        Server webServer = Server.createWebServer("-webAllowOthers").start();
        System.out.println("Running server on http://localhost:8082 at jdbc:h2:mem:"+this.hashCode());
    }

    public DBModel(File f) throws Exception {
        initDB();
        parseFile(f);
    }

    public DBModel(FastByIDMap<PreferenceArray> userData) {
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
            Connection conn = DriverManager.getConnection(String.format("jdbc:h2:mem:%s;DATABASE_TO_UPPER=FALSE", hashCode()));

            context = DSL.using(conn, SQLDialect.MYSQL);

            // initialize db
            context.query("CREATE TABLE IF NOT EXISTS yow (" +
                    "userid BIGINT NOT NULL, " +
                    "itemid BIGINT NOT NULL, " +
                    "feedback BIGINT NOT NULL, " +
                    "value REAL NOT NULL, " +
                    "PRIMARY KEY (userid,itemid,feedback)" +
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
        return context.select().from(table)
                .where(userField.equal(userID)
                        .and(itemField.equal(itemID))
                        .and(fbackField.equal(EXPLICIT))).fetchOne(valueField);
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
        Condition cond = userField.equal(userID).and(itemField.equal(itemID)).and(fbackField.equal(EXPLICIT));
        int numRows = context.selectOne().from(table).where(cond).fetchCount();
        UpdateConditionStep update = context.update(table).set(valueField, value).where(cond);
        InsertValuesStep4 insert = context.insertInto(table, userField, itemField, fbackField, valueField).values(userID, itemID, EXPLICIT, value);

        if(numRows > 0) {
            update.execute();
        }
        else {
            insert.execute();
        }
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

    public List<DBRow> getFeedbackRows() {
        String query = "" +
        "(SELECT\n" +
        "yow.userid AS userid, \n" +
        "yow.itemid AS itemid,\n" +
        "cast(isNull(col0.value, 0) as real) AS explicit,\n" +
        "cast(IsNull(col1.value, 0) as real) AS timeonpage,\n" +
        "cast(IsNull(col2.value, 0) as real) AS timeonmouse\n" +
        "FROM yow \n" +
        "LEFT JOIN yow AS col0 ON col0.feedback = 0 \n" +
        "AND col0.userid=yow.userid AND col0.itemid=yow.itemid\n" +
        "LEFT JOIN yow AS col1 ON col1.feedback = 1 \n" +
        "AND col1.userid=yow.userid AND col1.itemid=yow.itemid\n" +
        "LEFT JOIN yow AS col2 ON col2.feedback = 2 \n" +
        "AND col2.userid=yow.userid AND col2.itemid=yow.itemid\n" +
        "GROUP BY userid, itemid)\n";

        Result<Record> result = context.fetch(query);

        List<DBRow> rows = new ArrayList<>();
        result.stream().forEach(r ->
            rows.add(new DBRow(r.getValue(userField), r.getValue(itemField), r.getValue(explicitField), r.getValue(pageField), r.getValue(mouseField)))
        );

        return rows;
    }
    
    public void DBModelToCsv(DBModel model, String path){
    	try {
            FileWriter writer = new FileWriter(new File(path));
            float rating = 0;
            long itemId = 0;
            long userId = 0;

            List<DBModel.DBRow> allResults = model.getFeedbackRows();
            List<DBModel.DBRow> results = allResults.stream().filter(row -> row.rating > 0).collect(Collectors.toList());
            for (DBRow r : results) {
            	rating = r.rating;
            	itemId = r.itemid;
            	userId = r.userid;

            	writer.append(String.format("%d,%d,%.0f", userId, itemId, rating));
                writer.append("\n");
                writer.flush();
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class DBRow {

        public final long userid;
        public final long itemid;
        public final float rating;
        public final float timeonpage;
        public final float timeonmouse;
        public final float pagetimesmouse;
        public final float[] implicitfeedback;

        public DBRow(long userid, long itemid, float rating, float timeonpage, float timeonmouse) {
            this.userid = userid;
            this.itemid = itemid;
            this.rating = rating;
            this.timeonpage = timeonpage;
            this.timeonmouse = timeonmouse;
            this.pagetimesmouse = timeonpage * timeonmouse;
            this.implicitfeedback = new float[]{timeonpage, timeonmouse, timeonpage*timeonmouse};
        }
    }
}
