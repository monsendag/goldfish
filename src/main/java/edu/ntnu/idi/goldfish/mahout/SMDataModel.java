package edu.ntnu.idi.goldfish.mahout;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveArrayIterator;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.AbstractDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.common.iterator.FileLineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class SMDataModel extends AbstractDataModel {

	private static final Logger log = LoggerFactory.getLogger(SMDataModel.class);

	public static final long DEFAULT_MIN_RELOAD_INTERVAL_MS = 60 * 1000L; // 1
																			// minute?
	private static final char COMMENT_CHAR = '#';
	private static final char[] DELIMIETERS = {',', '\t'};

	private File dataFile;
	private long lastModified;
	private long lastUpdateFileModified;
	private char delimiter;
	private Splitter delimiterPattern;
	private ReentrantLock reloadLock;
	private boolean transpose;
	private long minReloadIntervalMS;

	private long[] userIDs;
	private long[] itemIDs;
	private FastByIDMap<? extends PreferenceArray> preferenceFromUsers;
	private FastByIDMap<? extends PreferenceArray> preferenceForItems;

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
	
	public SMDataModel(FastByIDMap<PreferenceArray> data) {
		processByIdMap(data);
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

                SMPreference.NUM_VALUES = determineNumValues(firstLine);

		List<String> firstLineSplit = Lists.newArrayList();
		for (String token : delimiterPattern.split(firstLine)) {
			firstLineSplit.add(token);
		}

		this.reloadLock = new ReentrantLock();
		this.transpose = transpose;
		this.minReloadIntervalMS = minReloadIntervalMS;

		reload();
	}

        private int determineNumValues(String firstLine) {
                int count = -2; // ignore user and item
                for(String token : delimiterPattern.split(firstLine)) {
                        count++;
                }
                return count;
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
	  public static FastByIDMap<SMPreferenceArray> toDataMap(FastByIDMap<List<SMPreference>> data, boolean byUser) {
		for (Map.Entry<Long,Object> entry : ((FastByIDMap<Object>) (FastByIDMap<?>) data).entrySet()) {
		List<SMPreference> prefList = (List<SMPreference>) entry.getValue();
		entry.setValue(byUser ? new SMUserPreferenceArray(prefList) : new SMItemPreferenceArray(
		prefList));
		}
		return (FastByIDMap<SMPreferenceArray>) (FastByIDMap<?>) data;
	  }


	protected void buildModel() throws IOException {
		lastModified = dataFile.lastModified();
		lastUpdateFileModified = readLastUpdateFileModified();

		FastByIDMap<List<SMPreference>> data = new FastByIDMap<List<SMPreference>>();
		FileLineIterator iterator = new FileLineIterator(dataFile, false);

		processFile(iterator, data);
		processByIdMap(toDataMap(data, true));
		
	}
	
	protected void processByIdMap(FastByIDMap<? extends PreferenceArray> data) {
		this.preferenceFromUsers = data;
		
		FastByIDMap<List<SMPreference>> prefsForItems = new FastByIDMap<List<SMPreference>>();
		FastIDSet itemIDSet = new FastIDSet();
		int currentCount = 0;
		float maxPrefValue = Float.NEGATIVE_INFINITY;
		float minPrefValue = Float.POSITIVE_INFINITY;
		
		// create list prefs for items
		// record max and min
		for (Map.Entry<Long, ? extends PreferenceArray> entry : preferenceFromUsers.entrySet()) {
			SMPreferenceArray prefs = (SMPreferenceArray) entry.getValue();
			prefs.sortByItem();
			for (Preference preference : prefs) {
				long itemID = preference.getItemID();
				itemIDSet.add(itemID);
				List<SMPreference> prefsForItem = prefsForItems.get(itemID);
				if (prefsForItem == null) {
					prefsForItem = Lists.newArrayListWithCapacity(2);
					prefsForItems.put(itemID, prefsForItem);
				}
				prefsForItem.add((SMPreference) preference);
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

		for (Map.Entry<Long, ? extends PreferenceArray> entry : preferenceForItems.entrySet()) {
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

	protected void processFile(FileLineIterator dataOrUpdateFileIterator, FastByIDMap<List<SMPreference>> userData) {
		while (dataOrUpdateFileIterator.hasNext()) {
			processLine(dataOrUpdateFileIterator.next(), userData);	
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
	protected void processLine(String line, FastByIDMap<List<SMPreference>> userData) {

		// Ignore empty lines and comments
		if (line.isEmpty() || line.charAt(0) == COMMENT_CHAR) {
			return;
		}

                Iterator<String> tokens = delimiterPattern.split(line).iterator();

		long userID = Long.parseLong(tokens.next());
		long itemID = Long.parseLong(tokens.next());

		// store preference values in array
		float[] values = new float[SMPreference.NUM_VALUES];

		int t = 0;
		while (tokens.hasNext()) {
			values[t++] = Float.parseFloat(tokens.next());
		}
		
		if(values.length == 1 && values[0] <= 0) {
			return;
		}
		
		if (transpose) {
			long tmp = userID;
			userID = itemID;
			itemID = tmp;
		}
		
		List<SMPreference> prefs = userData.get(userID);
		if(prefs == null) prefs = Lists.newArrayList();
		prefs.add(new GenericSMPreference(userID, itemID, values));
		userData.put(userID, prefs);
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

	public SMPreferenceArray getSMPreferencesFromUser(long userID) throws NoSuchUserException {
		PreferenceArray prefs = preferenceFromUsers.get(userID);
		if (prefs == null) {
			throw new NoSuchUserException(userID);
		}
		return (SMPreferenceArray) prefs;
	}
	
	public PreferenceArray getPreferencesFromUser(long userID)
			throws TasteException {
		PreferenceArray prefs = preferenceFromUsers.get(userID);
		if (prefs == null) {
			throw new NoSuchUserException(userID);
		}
		return prefs;
	}

	public FastIDSet getItemIDsFromUser(long userID) throws TasteException {
		SMPreferenceArray prefs = getSMPreferencesFromUser(userID);
		int size = prefs.length();
		FastIDSet result = new FastIDSet(size);
		for (int i = 0; i < size; i++) {
			result.add(prefs.getItemID(i));
		}
		return result;
	}
	
	public PreferenceArray getPreferencesForItem(long itemID) throws NoSuchItemException {
		PreferenceArray prefs = preferenceForItems.get(itemID);
		if (prefs == null) {
			throw new NoSuchItemException(itemID);
		}
		return prefs;
	}

	public LongPrimitiveIterator getItemIDs() throws TasteException {
		return new LongPrimitiveArrayIterator(itemIDs);
	}

	public SMPreferenceArray getSMPreferencesForItem(long itemID) throws TasteException {
		PreferenceArray prefs = preferenceForItems.get(itemID);
		if (prefs == null) {
			throw new NoSuchItemException(itemID);
		}
		return (SMPreferenceArray) prefs;
	}

	public Float getPreferenceValue(long userID, long itemID) throws TasteException {
		SMPreferenceArray prefs = getSMPreferencesFromUser(userID);
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

	public Long getPreferenceTime(long userID, long itemID) throws TasteException {
		return null;
	}

	public void setPreference(long userID, long itemID, float value) throws TasteException {

	}

	public void removePreference(long userID, long itemID) throws TasteException {
		
	}

    public double getDensity() throws TasteException {
        LongPrimitiveIterator it = getItemIDs();
        double count = 0;
        while (it.hasNext()) {
            PreferenceArray prefs = getPreferencesForItem(it.next());
            for(Preference p : prefs) {
                SMPreference pref = (SMPreference) p;
                if(pref.getValue(0) >= 1) {
                    count += 1;
                }
            }
        }
        double total = getNumUsers() * getNumItems();

        return count/total;
    }

    public static void removeInvalidPrefs(DataModel dataModel) throws TasteException {
        // iterate over users
        LongPrimitiveIterator userIterator = dataModel.getUserIDs();
        while(userIterator.hasNext()) {
            // iterate over users preferences
            long userID = userIterator.next();
            PreferenceArray preferencesFromUser = dataModel.getPreferencesFromUser(userID);
            for(Preference p : preferencesFromUser) {
                // if preference is less than 1, remove it from model
                if(p.getValue() <= 0) {
                	dataModel.removePreference(userID, p.getItemID());
                }
            }
        }

    }
}
