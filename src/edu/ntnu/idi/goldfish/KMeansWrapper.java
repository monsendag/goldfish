package edu.ntnu.idi.goldfish;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.Kluster;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.math.VectorWritable;

public class KMeansWrapper {
	
	
	public static Map<Long, Integer> getItemIndices(DataModel dataModel) throws TasteException {
		Map<Long, Integer> map = new HashMap<Long, Integer>();
		Iterator<Long> iter = dataModel.getItemIDs();
		int i = 0;
		while(iter.hasNext()) {
			map.put(iter.next(), i);
		}
		return map;
	}

	/**
	 * Split the dataModel into a set of datamodels based on a kMeans clustering of the users
	 * @throws TasteException 
	 */
	public static DataModel[] clusterUsers(DataModel dataModel, int N, DistanceMeasure measure, double convergenceDelta, int maxIterations, boolean runClustering, double clusterClassificationThreshold, boolean runSequential) throws IOException, InterruptedException, ClassNotFoundException, TasteException {
		
		StopWatch.start("clustering");
		
		File testData = new File("clusterdata/users/");
		testData.mkdirs();
		List<NamedVector> users = getUserVectors(dataModel);
		
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);

		// store users in sequence file
		writeUsers(users, new Path("clusterdata/users/part-00000"), conf);
		

		// write initial cluster centers
		writeClusterCenters(users, fs, conf, N, measure);
		
		Path output = new Path("clusterdata/output");

		// clean
		HadoopUtil.delete(conf, output);
		

		// Run k-means algorithm
		KMeansDriver.run(
				conf, 								// configuration
				new Path("clusterdata/users"), 		// the directory pathname for input points
				new Path("clusterdata/clusters"),	// the directory pathname for initial & computed clusters
				output, 							// the directory pathname for output points
				measure, 							// the DistanceMeasure to use
				convergenceDelta, 					// the convergence delta value
				maxIterations, 						// the maximum number of iterations
				runClustering, 						// true if points are to be clustered after iterations are completed
				clusterClassificationThreshold, 	// clustering strictness/ outlier removal parameter. 
				runSequential						// if true execute sequental algorithm
			);
		
		DataModel[] dataModels = readClusters(new Path("clusterdata/output/clusteredPoints/part-m-0"), fs, conf, N);
		
		int[] clusterSizes = new int[N];
		for(int i=0; i<N; i++) {
			clusterSizes[i] = dataModels[i].getNumUsers();
		}
		
		Arrays.sort(clusterSizes);
		ArrayUtils.reverse(clusterSizes);
		
		System.out.format("Delta: %.2f Threshold: %.2f %d clusters: %s \n", convergenceDelta, clusterClassificationThreshold, N, Arrays.toString(clusterSizes));

		return dataModels;
	}
	
	/**
	 * Write all users to SequenceFile
	 * Since kMeans is distributed on Hadoop, it needs the set of users available in this format
	 */
	public static void writeUsers(List<NamedVector> users, Path path, Configuration conf) throws IOException {
		FileSystem fs = FileSystem.get(path.toUri(), conf);
		Writer writer = new Writer(fs, conf, path, Text.class, VectorWritable.class);		
		for (NamedVector user : users) {
			writer.append(new Text(user.getName()), new VectorWritable(user));
		}
		writer.close();
	}
	
	/**
	 * Create N clusters, and use the first N users as cluster centers
	 */
	public static void writeClusterCenters(List<NamedVector>users, FileSystem fs, Configuration conf, int N, DistanceMeasure dm) throws IOException {
		Path path = new Path("clusterdata/clusters/part-00000");
		Writer writer = new Writer(fs, conf, path, Text.class, Kluster.class);
		
		//Write initial cluster centers
		for (int i = 0; i < N; i++) {
			Kluster cluster = new Kluster(users.get(i), i, dm);
			writer.append(new Text(cluster.getIdentifier()), cluster);
		}
		writer.close();
	}
	
	public static DataModel[] readClusters(Path path, FileSystem fs, Configuration conf, int N) throws IOException {
		@SuppressWarnings("unchecked")
		FastByIDMap<PreferenceArray>[] maps = new FastByIDMap[N];
		for(int i = 0; i < N; i++) {
			maps[i] = new FastByIDMap<PreferenceArray>();
		}
		// read clusters from Hadoop Sequence File
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, path, conf);
		// store objects
		IntWritable clusterId = new IntWritable();
		WeightedVectorWritable value = new WeightedVectorWritable();
		
		NamedVector vector;
		long userId;
		while (reader.next(clusterId, value)) {
			// Read output, print vector, cluster ID 
			vector = (NamedVector) value.getVector();
			userId = Long.parseLong(vector.getName());
			maps[clusterId.get()].put(userId, namedVecToPreferenceArr(vector));
		}
		reader.close();
		
		
		DataModel[] models = new DataModel[N];
		for(int i = 0; i < N; i++) {
			models[i] = new GenericDataModel(maps[i]);
		}
		return models;
	}
	
	public static List<NamedVector> getUserVectors(DataModel dataModel) throws IOException, TasteException {
		List<NamedVector> users = new ArrayList<NamedVector>();
		Iterator<Long> userIterator = dataModel.getUserIDs();
		int cardinality = dataModel.getNumItems();
		
		PreferenceArray prefs;
		
		while(userIterator.hasNext()) {
			prefs = dataModel.getPreferencesFromUser(userIterator.next());
			users.add(preferenceArrToNamedVec(prefs, cardinality));
		}
		return users;
	}

	public static NamedVector preferenceArrToNamedVec(PreferenceArray prefs, int cardinality) {
		RandomAccessSparseVector ratings = new RandomAccessSparseVector((int)10e10);
	    for (Preference preference : prefs) {
	      ratings.set((int) preference.getItemID(), preference.getValue());
	    }    
		return new NamedVector(ratings, ""+prefs.getUserID(0));
	}
	
	public static PreferenceArray namedVecToPreferenceArr(NamedVector vector) {
		long userId = Long.parseLong(vector.getName());
		
		// help for iterating over the sparse named vector
		RandomAccessSparseVector sparseVector = new RandomAccessSparseVector(vector);
		Iterator<Element> iter = sparseVector.iterateNonZero();

		// list for storing preferences
		List<Preference> prefs = new ArrayList<Preference>();
		Element next;
		Preference pref;
		while(iter.hasNext()) {
			next = iter.next();
			pref = new GenericPreference(userId, next.index(), (float) next.get());
			prefs.add(pref);
		}
		return new GenericUserPreferenceArray(prefs);
	}
}
