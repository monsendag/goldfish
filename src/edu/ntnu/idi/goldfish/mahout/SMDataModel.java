package edu.ntnu.idi.goldfish.mahout;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveArrayIterator;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.AbstractDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericItemPreferenceArray;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.common.iterator.FileLineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;

/**
 * <p>
 * A {@link DataModel} backed by a delimited file. This class expects a file
 * where each line contains a user ID, followed by item ID, followed by optional
 * preference value, followed by optional timestamp. Commas or tabs delimit
 * fields:
 * </p>
 * 
 * <p>
 * {@code userID,itemID[,preference[,timestamp]]}
 * </p>
 * 
 * <p>
 * Preference value is optional to accommodate applications that have no notion
 * of a preference value (that is, the user simply expresses a preference for an
 * item, but no degree of preference).
 * </p>
 * 
 * <p>
 * The preference value is assumed to be parseable as a {@code double}. The user
 * IDs and item IDs are read parsed as {@code long}s. The preference value may
 * be empty, to indicate "no preference value", but cannot be empty. That is,
 * this is legal:
 * </p>
 * 
 * <p>
 * {@code 123,456,,129050099059}
 * </p>
 * 
 * <p>
 * But this isn't:
 * </p>
 * 
 * <p>
 * {@code 123,456,129050099059}
 * </p>
 * 
 * <p>
 * It is also acceptable for the lines to contain additional fields. An empty
 * line, or one that begins with '#' will be ignored as a comment.
 * </p>
 * 
 * <p>
 * This class will reload data from the data file when
 * {@link #refresh(Collection)} is called, unless the file has been reloaded
 * very recently already.
 * </p>
 * 
 * <p>
 * This class will also look for update "delta" files in the same directory,
 * with file names that start the same way (up to the first period). These files
 * have the same format, and provide updated data that supersedes what is in the
 * main data file. This is a mechanism that allows an application to push
 * updates to {@link FileDataModel} without re-copying the entire data file.
 * </p>
 * 
 * <p>
 * One small format difference exists. Update files must also be able to express
 * deletes. This is done by ending with a blank preference value, as in
 * "123,456,".
 * </p>
 * 
 * <p>
 * Note that it's all-or-nothing -- all of the items in the file must express no
 * preference, or the all must. These cannot be mixed. Put another way there
 * will always be the same number of delimiters on every line of the file!
 * </p>
 * 
 * <p>
 * This class is not intended for use with very large amounts of data (over,
 * say, tens of millions of rows). For that, a JDBC-backed {@link DataModel} and
 * a database are more appropriate.
 * </p>
 * 
 * <p>
 * It is possible and likely useful to subclass this class and customize its
 * behavior to accommodate application-specific needs and input formats. See
 * {@link #processLine(String, FastByIDMap, FastByIDMap, boolean)} and
 * {@link #processLineWithoutID(String, FastByIDMap, FastByIDMap)}
 */
public class SMDataModel extends AbstractDataModel {

	private static final Logger log = LoggerFactory.getLogger(SMDataModel.class);

	public static final long DEFAULT_MIN_RELOAD_INTERVAL_MS = 60 * 1000L; // 1
																			// minute?
	private static final char COMMENT_CHAR = '#';
	private static final char[] DELIMIETERS = {',', '\t'};

	private final File dataFile;
	private long lastModified;
	private long lastUpdateFileModified;
	private final char delimiter;
	private final Splitter delimiterPattern;
	private final boolean hasPrefValues;
	private final ReentrantLock reloadLock;
	private final boolean transpose;
	private final long minReloadIntervalMS;

	private long[] userIDs;
	private long[] itemIDs;
	private FastByIDMap<PreferenceArray> preferenceFromUsers;
	private FastByIDMap<PreferenceArray> preferenceForItems;

	/**
	 * @param dataFile
	 *            file containing preferences data. If file is compressed (and
	 *            name ends in .gz or .zip accordingly) it will be decompressed
	 *            as it is read)
	 * @throws FileNotFoundException
	 *             if dataFile does not exist
	 * @throws IOException
	 *             if file can't be read
	 */
	public SMDataModel(File dataFile) throws IOException {
		this(dataFile, false, DEFAULT_MIN_RELOAD_INTERVAL_MS);
	}

	/**
	 * @param delimiterRegex
	 *            If your data file don't use '\t' or ',' as delimiter, you can
	 *            specify a custom regex pattern.
	 */
	public SMDataModel(File dataFile, String delimiterRegex) throws IOException {
		this(dataFile, false, DEFAULT_MIN_RELOAD_INTERVAL_MS, delimiterRegex);
	}

	/**
	 * @param transpose
	 *            transposes user IDs and item IDs -- convenient for 'flipping'
	 *            the data model this way
	 * @param minReloadIntervalMS
	 *            the minimum interval in milliseconds after which a full reload
	 *            of the original datafile is done when refresh() is called
	 * @see #FileDataModel(File)
	 */
	public SMDataModel(File dataFile, boolean transpose, long minReloadIntervalMS)
			throws IOException {
		this(dataFile, transpose, minReloadIntervalMS, null);
	}

	/**
	 * @param delimiterRegex
	 *            If your data file don't use '\t' or ',' as delimiters, you can
	 *            specify user own using regex pattern.
	 * @throws IOException
	 */
	public SMDataModel(File dataFile, boolean transpose, long minReloadIntervalMS, String delimiterRegex) throws IOException {

		this.dataFile = Preconditions.checkNotNull(dataFile.getAbsoluteFile());
		if (!dataFile.exists() || dataFile.isDirectory()) {
			throw new FileNotFoundException(dataFile.toString());
		}
		
		Preconditions.checkArgument(dataFile.length() > 0L, "dataFile is empty");
		Preconditions.checkArgument(minReloadIntervalMS >= 0L, "minReloadIntervalMs must be non-negative");

		this.lastModified = dataFile.lastModified();
		this.lastUpdateFileModified = readLastUpdateFileModified();

		FileLineIterator iterator = new FileLineIterator(dataFile, false);
		String firstLine = iterator.peek();
		while (firstLine.isEmpty() || firstLine.charAt(0) == COMMENT_CHAR) {
			iterator.next();
			firstLine = iterator.peek();
		}
		Closeables.close(iterator, true);

		if (delimiterRegex == null) {
			delimiter = determineDelimiter(firstLine);
			delimiterPattern = Splitter.on(delimiter);
		} else {
			delimiter = '\0';
			delimiterPattern = Splitter.onPattern(delimiterRegex);
			if (!delimiterPattern.split(firstLine).iterator().hasNext()) {
				throw new IllegalArgumentException(
						"Did not find a delimiter(pattern) in first line");
			}
		}
		List<String> firstLineSplit = Lists.newArrayList();
		for (String token : delimiterPattern.split(firstLine)) {
			firstLineSplit.add(token);
		}
		// If preference value exists and isn't empty then the file is
		// specifying pref values
		hasPrefValues = firstLineSplit.size() >= 3 && !firstLineSplit.get(2).isEmpty();

		this.reloadLock = new ReentrantLock();
		this.transpose = transpose;
		this.minReloadIntervalMS = minReloadIntervalMS;

		reload();
	}

	public File getDataFile() {
		return dataFile;
	}

	protected void reload() {
		if (reloadLock.tryLock()) {
			try {
				buildModel();
			} catch (IOException ioe) {
				log.warn("Exception while reloading", ioe);
			} finally {
				reloadLock.unlock();
			}
		}
	}

	/**
	 * Swaps, in-place, {@link List}s for arrays in {@link Map} values .
	 * 
	 * @return input value
	 */
	public static FastByIDMap<PreferenceArray> toDataMap(FastByIDMap<Collection<Preference>> data, boolean byUser) {
		for (Map.Entry<Long, Object> entry : ((FastByIDMap<Object>) (FastByIDMap<?>) data).entrySet()) {
			List<Preference> prefList = (List<Preference>) entry.getValue();
			entry.setValue(new SMUserPreferenceArray(prefList));
		}
		return (FastByIDMap<PreferenceArray>) (FastByIDMap<?>) data;
	}

	protected void buildModel() throws IOException {
		lastModified = dataFile.lastModified();
		lastUpdateFileModified = readLastUpdateFileModified();

		FastByIDMap<Collection<Preference>> data = new FastByIDMap<Collection<Preference>>();
		FileLineIterator iterator = new FileLineIterator(dataFile, false);

		processFile(iterator, data);

		this.preferenceFromUsers = toDataMap(data, true);
		FastByIDMap<Collection<Preference>> prefsForItems = new FastByIDMap<Collection<Preference>>();
		FastIDSet itemIDSet = new FastIDSet();
		int currentCount = 0;
		float maxPrefValue = Float.NEGATIVE_INFINITY;
		float minPrefValue = Float.POSITIVE_INFINITY;
		
		// create list prefs for items
		// record max and min
		for (Map.Entry<Long, PreferenceArray> entry : preferenceFromUsers.entrySet()) {
			PreferenceArray prefs = entry.getValue();
			prefs.sortByItem();
			for (Preference preference : prefs) {
				long itemID = preference.getItemID();
				itemIDSet.add(itemID);
				Collection<Preference> prefsForItem = prefsForItems.get(itemID);
				if (prefsForItem == null) {
					prefsForItem = Lists.newArrayListWithCapacity(2);
					prefsForItems.put(itemID, prefsForItem);
				}
				prefsForItem.add(preference);
				float value = preference.getValue();
				if (value > maxPrefValue) {
					maxPrefValue = value;
				}
				if (value < minPrefValue) {
					minPrefValue = value;
				}
			}
			if (++currentCount % 10000 == 0) {
				log.info("Processed {} users", currentCount);
			}
		}

		setMinPreference(minPrefValue);
		setMaxPreference(maxPrefValue);

		this.itemIDs = itemIDSet.toArray();
		itemIDSet = null; // Might help GC -- this is big
		Arrays.sort(itemIDs);

		this.preferenceForItems = toDataMap(prefsForItems, false);

		for (Map.Entry<Long, PreferenceArray> entry : preferenceForItems.entrySet()) {
			entry.getValue().sortByUser();
		}

		this.userIDs = new long[preferenceFromUsers.size()];
		int i = 0;
		LongPrimitiveIterator it = preferenceFromUsers.keySetIterator();
		while (it.hasNext()) {
			userIDs[i++] = it.next();
		}
		Arrays.sort(userIDs);

	}

	/**
	 * Finds update delta files in the same directory as the data file. This
	 * finds any file whose name starts the same way as the data file (up to
	 * first period) but isn't the data file itself. For example, if the data
	 * file is /foo/data.txt.gz, you might place update files at
	 * /foo/data.1.txt.gz, /foo/data.2.txt.gz, etc.
	 */
	private Iterable<File> findUpdateFilesAfter(long minimumLastModified) {
		String dataFileName = dataFile.getName();
		int period = dataFileName.indexOf('.');
		String startName = period < 0 ? dataFileName : dataFileName.substring(0, period);
		File parentDir = dataFile.getParentFile();
		Map<Long, File> modTimeToUpdateFile = new TreeMap<Long, File>();
		FileFilter onlyFiles = new FileFilter() {
			public boolean accept(File file) {
				return !file.isDirectory();
			}
		};
		for (File updateFile : parentDir.listFiles(onlyFiles)) {
			String updateFileName = updateFile.getName();
			if (updateFileName.startsWith(startName) && !updateFileName.equals(dataFileName)
					&& updateFile.lastModified() >= minimumLastModified) {
				modTimeToUpdateFile.put(updateFile.lastModified(), updateFile);
			}
		}
		return modTimeToUpdateFile.values();
	}

	private long readLastUpdateFileModified() {
		long mostRecentModification = Long.MIN_VALUE;
		for (File updateFile : findUpdateFilesAfter(0L)) {
			mostRecentModification = Math.max(mostRecentModification, updateFile.lastModified());
		}
		return mostRecentModification;
	}

	public static char determineDelimiter(String line) {
		for (char possibleDelimieter : DELIMIETERS) {
			if (line.indexOf(possibleDelimieter) >= 0) {
				return possibleDelimieter;
			}
		}
		throw new IllegalArgumentException("Did not find a delimiter in first line");
	}

	protected void processFile(FileLineIterator dataOrUpdateFileIterator, FastByIDMap<?> data) {
		while (dataOrUpdateFileIterator.hasNext()) {
			processLine(dataOrUpdateFileIterator.next(), data);	
		}
	}

	/**
	 * <p>
	 * Reads one line from the input file and adds the data to a
	 * {@link FastByIDMap} data structure which maps user IDs to preferences.
	 * This assumes that each line of the input file corresponds to one
	 * preference. After reading a line and determining which user and item the
	 * preference pertains to, the method should look to see if the data
	 * contains a mapping for the user ID already, and if not, add an empty data
	 * structure of preferences as appropriate to the data.
	 * </p>
	 * 
	 * <p>
	 * Note that if the line is empty or begins with '#' it will be ignored as a
	 * comment.
	 * </p>
	 * 
	 * @param line
	 *            line from input data file
	 * @param userData
	 *            all data read so far, as a mapping from user IDs to
	 *            preferences
	 * @param fromPriorData
	 *            an implementation detail -- if true, data will map IDs to
	 *            {@link PreferenceArray} since the framework is attempting to
	 *            read and update raw data that is already in memory. Otherwise
	 *            it maps to {@link Collection}s of {@link Preference}s, since
	 *            it's reading fresh data. Subclasses must be prepared to handle
	 *            this wrinkle.
	 */
	protected void processLine(String line, FastByIDMap<?> userData) {

		// Ignore empty lines and comments
		if (line.isEmpty() || line.charAt(0) == COMMENT_CHAR) {
			return;
		}

		Iterator<String> tokens = delimiterPattern.split(line).iterator();

		long userID = Long.parseLong(tokens.next());
		long itemID = Long.parseLong(tokens.next());

		// store preference values in array
		float[] values = new float[2];

		int t = 0;
		while (tokens.hasNext()) {
			values[t++] = Float.parseFloat(tokens.next());
		}
		
		if (transpose) {
			long tmp = userID;
			userID = itemID;
			itemID = tmp;
		}

		// This is kind of gross but need to handle two types of storage
		Collection<SMPreference> prefs = Lists.newArrayListWithCapacity(2);
		prefs.add(new GenericSMPreference(userID, itemID, values));
		((FastByIDMap<Collection<SMPreference>>) userData).put(userID, prefs);
	}

	/**
	 * Subclasses may wish to override this if ID values in the file are not
	 * numeric. This provides a hook by which subclasses can inject an
	 * {@link org.apache.mahout.cf.taste.model.IDMigrator} to perform
	 * translation.
	 */
	protected long readUserIDFromString(String value) {
		return Long.parseLong(value);
	}

	/**
	 * Subclasses may wish to override this if ID values in the file are not
	 * numeric. This provides a hook by which subclasses can inject an
	 * {@link org.apache.mahout.cf.taste.model.IDMigrator} to perform
	 * translation.
	 */
	protected long readItemIDFromString(String value) {
		return Long.parseLong(value);
	}

	public LongPrimitiveIterator getUserIDs() throws TasteException {
		return new LongPrimitiveArrayIterator(userIDs);
	}

	public PreferenceArray getPreferencesFromUser(long userID) throws NoSuchUserException {
		PreferenceArray prefs = preferenceFromUsers.get(userID);
		if (prefs == null) {
			throw new NoSuchUserException(userID);
		}
		return prefs;
	}

	public FastIDSet getItemIDsFromUser(long userID) throws TasteException {
		PreferenceArray prefs = getPreferencesFromUser(userID);
		int size = prefs.length();
		FastIDSet result = new FastIDSet(size);
		for (int i = 0; i < size; i++) {
			result.add(prefs.getItemID(i));
		}
		return result;
	}

	public LongPrimitiveIterator getItemIDs() throws TasteException {
		return new LongPrimitiveArrayIterator(itemIDs);
	}

	public PreferenceArray getPreferencesForItem(long itemID) throws TasteException {
		PreferenceArray prefs = preferenceForItems.get(itemID);
		if (prefs == null) {
			throw new NoSuchItemException(itemID);
		}
		return prefs;
	}

	public Float getPreferenceValue(long userID, long itemID) throws TasteException {
		PreferenceArray prefs = getPreferencesFromUser(userID);
		int size = prefs.length();
		for (int i = 0; i < size; i++) {
			if (prefs.getItemID(i) == itemID) {
				return prefs.getValue(i);
			}
		}
		return null;

	}

	public int getNumItems() throws TasteException {
		return itemIDs.length;
	}

	public int getNumUsers() throws TasteException {
		return userIDs.length;
	}

	public int getNumUsersWithPreferenceFor(long itemID) throws TasteException {
		PreferenceArray prefs1 = preferenceForItems.get(itemID);
		return prefs1 == null ? 0 : prefs1.length();
	}

	public int getNumUsersWithPreferenceFor(long itemID1, long itemID2) throws TasteException {
		PreferenceArray prefs1 = preferenceForItems.get(itemID1);
		if (prefs1 == null) {
			return 0;
		}
		PreferenceArray prefs2 = preferenceForItems.get(itemID2);
		if (prefs2 == null) {
			return 0;
		}

		int size1 = prefs1.length();
		int size2 = prefs2.length();
		int count = 0;
		int i = 0;
		int j = 0;
		long userID1 = prefs1.getUserID(0);
		long userID2 = prefs2.getUserID(0);
		while (true) {
			if (userID1 < userID2) {
				if (++i == size1) {
					break;
				}
				userID1 = prefs1.getUserID(i);
			} else if (userID1 > userID2) {
				if (++j == size2) {
					break;
				}
				userID2 = prefs2.getUserID(j);
			} else {
				count++;
				if (++i == size1 || ++j == size2) {
					break;
				}
				userID1 = prefs1.getUserID(i);
				userID2 = prefs2.getUserID(j);
			}
		}
		return count;
	}

	public void refresh(Collection<Refreshable> alreadyRefreshed) {
		if (dataFile.lastModified() > lastModified + minReloadIntervalMS
				|| readLastUpdateFileModified() > lastUpdateFileModified + minReloadIntervalMS) {
			log.debug("File has changed; reloading...");
			reload();
		}
	}

	public boolean hasPreferenceValues() {
		return true;
	}

	@Override
	public String toString() {
		return "SMDataModel[dataFile:" + dataFile + ']';
	}

	/**
	 * Write dataset to file with csv format
	 * 
	 * @param filename
	 *            url with location and filename
	 */
	public void writeDatasetToFile(String filename) {
		try {
			FileWriter writer = new FileWriter(filename);

			writer.append("# UserID,ItemID,Rating,ReadIndex \n");

			Iterator<Long> users = getUserIDs();
			while (users.hasNext()) {
				long userId = users.next();
				PreferenceArray preferences = getPreferencesFromUser(userId);
				Iterator<Preference> it = preferences.iterator();

				float rating = 0;
				float readIndex = 0;
				float itemId = 0;
				while (it.hasNext()) {
					SMPreference p = (SMPreference) it.next();
					rating = p.getValue(0); // explicit
					readIndex = p.getValue(1); // readindex
					itemId = p.getItemID(); // item
					writer.append(userId + "," + itemId + "," + rating + "," + readIndex);
					writer.append("\n");
				}

				writer.flush();
				writer.close();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TasteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Long getPreferenceTime(long userID, long itemID) throws TasteException {
		return null;
	}

	public void setPreference(long userID, long itemID, float value) throws TasteException {
		// TODO Auto-generated method stub

	}

	public void removePreference(long userID, long itemID) throws TasteException {
		// TODO Auto-generated method stub

	}

}
