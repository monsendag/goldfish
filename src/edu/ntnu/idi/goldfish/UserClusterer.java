package edu.ntnu.idi.goldfish;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.Kluster;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.common.distance.TanimotoDistanceMeasure;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.VectorWritable;

public class UserClusterer {
	
	private static Map<Integer, Integer> userPositions;
	private static Map<Integer, Integer> itemPositions;
	private static List<RatingPair> ratingPairs;
	private static double[][] userItemMatrix;

	/**
	 * Split the dataModel into a set of datamodels based on a kMeans clustering of the users
	 * @param dataModel
	 * @param N
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static DataModel[] clusterUsers(DataModel dataModel, int N) throws IOException, InterruptedException, ClassNotFoundException {

		return null;
	}
	
	
	
	public static Map<Integer, Integer> invert(Map<Integer, Integer> map) {

	    Map<Integer, Integer> inv = new HashMap<Integer, Integer>();

	    for (Entry<Integer, Integer> entry : map.entrySet())
	        inv.put(entry.getValue(), entry.getKey());

	    return inv;
	}
	
	public static void writeUsersToFile(List<NamedVector > users, Path path, Configuration conf) throws IOException {
		FileSystem fs = FileSystem.get(path.toUri(), conf);
		SequenceFile.Writer writer = new SequenceFile.Writer(fs, conf, path, Text.class, VectorWritable.class);
		VectorWritable vec = new VectorWritable();
		
		for (NamedVector user : users) {
			vec.set(user);
			// Serializes vector data
			writer.append(new Text(user.getName()), vec);
		}
		writer.close();
		
	}
	
	public static List<NamedVector> getNamedVector() throws FileNotFoundException {
		List<NamedVector> users = new ArrayList<NamedVector>();
		
		double[][] userItemMatrix = getMatrix("datasets/vtt-36k/VTT_I_data.csv");
		NamedVector user;
		
		Set<Integer> userIds = userPositions.keySet();

		int iterator = 0;
		for (Integer userId : userIds) {
			user = new NamedVector(new DenseVector(userItemMatrix[iterator]), ""+userId);
			users.add(user);
			iterator++;
		}
		
		return users;
	}
	
	public static double[][] getMatrix(String csvFile) throws FileNotFoundException {
		itemPositions = new HashMap<Integer, Integer>();
		userPositions = new HashMap<Integer, Integer>();
		ratingPairs = new ArrayList<RatingPair>();
		
		
		int itemPosition = 0;
		int userPosition = 0;
		
		File ratings = new File(csvFile);
		Scanner sc = new Scanner(ratings);
		sc.useDelimiter("[\n,]");
		sc.nextLine(); // skip header
		int counter = 0;
		
		RatingPair pair;

		while(sc.hasNextLine() && counter++ <= 5000) {
			
			pair = new RatingPair(sc.nextInt(), sc.nextInt());
			sc.nextInt(); // skip rating
			
			ratingPairs.add(pair);
			
			if(!userPositions.containsKey(pair.userId)) {
				userPositions.put(pair.userId, userPosition);
				userPosition++;
			}
			
			if(!itemPositions.containsKey(pair.itemId)) {
				itemPositions.put(pair.itemId, itemPosition);
				itemPosition++;
			}
			
		}
		sc.close();
		
		userItemMatrix = new double[userPosition][itemPosition];
		
		for (RatingPair ratingPair : ratingPairs) {
			userItemMatrix[userPositions.get(ratingPair.userId)][itemPositions.get(ratingPair.itemId)] = 1;
		}

		return userItemMatrix;
	}

	public static void main(String[] args) throws TasteException, IOException, InterruptedException, ClassNotFoundException {
		int numberOfCluster = 10;
		
		List<NamedVector> users = getNamedVector();
		
		File testData = new File("testdata");
		if (!testData.exists()) {
			testData.mkdir();
		}
		testData = new File("testdata/points");
		if (!testData.exists()) {
			testData.mkdir();
		}
		
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);
		//Write initial centers
		writeUsersToFile(users, new Path("testdata/points/file1"), conf);
		
		Path path = new Path("testdata/clusters/part-00000");
		SequenceFile.Writer writer = new SequenceFile.Writer(fs, conf, path, Text.class, Kluster.class);
		
		for (int i = 0; i < numberOfCluster; i++) {
			NamedVector vec = users.get(i);
			Kluster cluster = new Kluster(vec, i, new EuclideanDistanceMeasure());
			writer.append(new Text(cluster.getIdentifier()), cluster);
		}
		writer.close();
		
		Path output = new Path("output");
		HadoopUtil.delete(conf, output);
		
		// Run k-means algorithm
		KMeansDriver.run(
				conf, 							// configuration
				new Path("testdata/points"), 	// the directory pathname for input points
				new Path("testdata/clusters"),  // the directory pathname for initial & computed clusters
				output, 						// the directory pathname for output points
				new TanimotoDistanceMeasure(), // the DistanceMeasure to use
				0.001, 							// the convergence delta value
				10, 							// the maximum number of iterations
				true, 							// true if points are to be clustered after iterations are completed
				0.0, 							// clustering strictness/ outlier removal parameter. 
				false							// if true execute sequental algorithm
			);
		
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path("output/" 
				+ Kluster.CLUSTERED_POINTS_DIR + "/part-m-00000"), conf);
		
		IntWritable clusterId = new IntWritable();
		WeightedVectorWritable value = new WeightedVectorWritable();
		Map<String, String> clusters = new HashMap<String, String>(); 
		while (reader.next(clusterId, value)) {
			// Read output, print vector, cluster ID 
			NamedVector vector = (NamedVector) value.getVector();
			String vectorName = vector.getName();
			System.out.println(vectorName + " belongs to cluster " + clusterId.toString());
			clusters.put(vectorName, clusterId.toString());
		}
		reader.close();
		
		writeClustersToFile(clusters, numberOfCluster);

	
	}
	
	public static void writeClustersToFile(Map<String, String> clusters, int numberOfClusters) throws IOException {
		FileWriter fw = null;
		PrintWriter pw = null;
		String fileName;
		
		File testData = new File("datasets/vtt-clustered");
		if (!testData.exists()) {
			testData.mkdir();
		}
		
		for (File file : testData.listFiles()) {
			file.delete();
		}
		
		Map<Integer, Integer> invertedItemPositions = invert(itemPositions);
		
		int clusterCounter = 0;
		
		// for each cluster
		for (int i = 0; i < numberOfClusters; i++) {
			fileName = "datasets/vtt-clustered/cluster" + i + ".csv";
			fw = new FileWriter(fileName);
			pw = new PrintWriter(fw);
			
			// for each user in cluster i 
			for (Entry<String, String> entry : clusters.entrySet()) {
				String clusterId = entry.getValue();
				String userIdString = entry.getKey();
				int userId = Integer.parseInt(userIdString);
				
				if(i == Integer.parseInt(clusterId)) {
					clusterCounter++;
					// for each rating in userItemMatrix[user][i]
					for (int j = 0; j < userItemMatrix[userPositions.get(userId)].length; j++) {
						// if rating != 0
						if(userItemMatrix[userPositions.get(userId)][j] != 0) {
							pw.print(userId);
							pw.print(",");
							pw.print(invertedItemPositions.get(j));
							pw.print(",");
							pw.println(1);
						}
					}
				}
			}
			
			System.out.println("Cluster id " + i + " count " + clusterCounter);
			clusterCounter = 0;
			
			pw.flush();
		}
		
		pw.close();
		fw.close();
	}
	

}
