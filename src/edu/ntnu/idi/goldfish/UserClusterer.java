package edu.ntnu.idi.goldfish;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.apache.mahout.cf.taste.impl.model.GenericItemPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.Kluster;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.VectorWritable;

public class UserClusterer {

	/**
	 * Split the dataModel into a set of datamodels based on a kMeans clustering of the users
	 * @throws TasteException 
	 */
	public static DataModel[] clusterUsers(DataModel dataModel, int N, DistanceMeasure dm) throws IOException, InterruptedException, ClassNotFoundException, TasteException {
		
		File testData = new File("clusterdata/users");
		testData.mkdirs();
		
		List<NamedVector> users = getUserVectors(dataModel);
		
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);
		
		// store users in sequence file
		writeUsers(users, new Path("clusterdata/users/part-00000"), conf);
		
		// write initial cluster centers
		writeClusterCenters(users, fs, conf, N, dm);
		
		Path output = new Path("clusterdata/output");

		// clean
		HadoopUtil.delete(conf, output);
		
		// Run k-means algorithm
		KMeansDriver.run(
				conf, 								// configuration
				new Path("clusterdata/users"), 	// the directory pathname for input points
				new Path("clusterdata/clusters"),	// the directory pathname for initial & computed clusters
				output, 							// the directory pathname for output points
				dm, 							// the DistanceMeasure to use
				0.001, 							// the convergence delta value
				10, 							// the maximum number of iterations
				true, 							// true if points are to be clustered after iterations are completed
				0.0, 							// clustering strictness/ outlier removal parameter. 
				false							// if true execute sequental algorithm
			);
		
		return readClusters(fs, conf, N);
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
	
	public static DataModel[] readClusters(FileSystem fs, Configuration conf, int N) throws IOException {
		FastByIDMap<PreferenceArray>[] maps = new FastByIDMap[N];
		for(int i = 0; i < N; i++) {
			maps[i] = new FastByIDMap<PreferenceArray>();
		}
		
		Path file = new Path("clusterdata/output/" + Kluster.CLUSTERED_POINTS_DIR + "/part-m-00000");
		// read clusters from Hadoop Sequence File
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, file, conf);
		// clusterId store
		IntWritable clusterId = new IntWritable();
		// value store
		VectorWritable value = new VectorWritable();
		
		while (reader.next(clusterId, value)) {
			// Read output, print vector, cluster ID 
			NamedVector vector = (NamedVector) value.get();
			long userId = (long) Integer.parseInt(vector.getName());
			maps[clusterId.get()].put(userId, namedVecToPreferenceArr(vector));
		}
		reader.close();
		
		DataModel[] models = new DataModel[N];
		for(int i = 0; i < N; i++) {
			models[i] = new GenericDataModel(maps[i]);
		}
		return models;
	}
	

	
	public static PreferenceArray namedVecToPreferenceArr(NamedVector vec) {
		PreferenceArray arr = new GenericItemPreferenceArray(vec.size());
		arr.setUserID(0, Integer.parseInt(vec.getName()));
		
		for(int i=0; i < vec.size(); i++) {
			arr.setValue(i, (float) vec.get(i));
		}
		return arr;
	}

	public static NamedVector preferenceArrToNamedVec(PreferenceArray arr) {
		DenseVector vec = new DenseVector(arr.length());
		for(int i=0; i < arr.length(); i++) {
			vec.set(i, arr.getValue(i));
		}
		return new NamedVector(vec, ""+arr.getUserID(0));
	}
	
	public static List<NamedVector> getUserVectors(DataModel dataModel) throws IOException, TasteException {
		List<NamedVector> users = new ArrayList<NamedVector>();
		Iterator<Long> userIterator = dataModel.getUserIDs();
		PreferenceArray prefs;
		while(userIterator.hasNext()) {
			prefs = dataModel.getPreferencesFromUser(userIterator.next());
			users.add(preferenceArrToNamedVec(prefs));
		}
		return users;
	}
}
