package edu.ntnu.idi.goldfish;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.antlr.grammar.v3.ANTLRv3Parser.finallyClause_return;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.Kluster;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.xerces.xs.ItemPSVI;

public class Clustering {
	
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
		for (int i = 0; i < userItemMatrix.length; i++) {
			user = new NamedVector(new DenseVector(userItemMatrix[i]), "User " + i);
			users.add(user);
		}
		
		return users;
	}
	
	public static double[][] getMatrix(String csvFile) throws FileNotFoundException {
		Map<Integer, Integer> itemPosistions = new HashMap<Integer, Integer>();
		Map<Integer, Integer> userPositions = new HashMap<Integer, Integer>();
		List<RatingPair> ratingPairs = new ArrayList<RatingPair>();
		
		int itemPosition = 0;
		int userPosition = 0;
		
		File ratings = new File(csvFile);
		Scanner sc = new Scanner(ratings);
		sc.useDelimiter("[\n,]");
		sc.nextLine(); // skip header
		int counter = 0;
		
		RatingPair pair;
		while(sc.hasNextLine() && counter++ <= 10000) {
			
			pair = new RatingPair(sc.nextInt(), sc.nextInt());
			sc.nextInt(); // skip rating
			
			ratingPairs.add(pair);
			
			if(!userPositions.containsKey(pair.userId)) {
				userPositions.put(pair.userId, userPosition);
				userPosition++;
			}
			
			if(!itemPosistions.containsKey(pair.itemId)) {
				itemPosistions.put(pair.itemId, itemPosition);
				itemPosition++;
			}
			
		}
		sc.close();
		
		double[][] userItemMatrix = new double[userPosition + 1][itemPosition + 1];
		
		for (RatingPair ratingPair : ratingPairs) {
			userItemMatrix[userPositions.get(ratingPair.userId)][itemPosistions.get(ratingPair.itemId)] = 1;
		}

		return userItemMatrix;
	}

	public static void main(String[] args) throws TasteException, IOException, InterruptedException, ClassNotFoundException {
		int numberOfCluster = 2;
		
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
		KMeansDriver.run(conf, new Path("testdata/points"), new Path("testdata/clusters"), output, 
				new EuclideanDistanceMeasure(), 0.001, 10, true, 0.0, false);
		
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path("output/" 
				+ Kluster.CLUSTERED_POINTS_DIR + "/part-m-00000"), conf);
		
		IntWritable key = new IntWritable();
		WeightedVectorWritable value = new WeightedVectorWritable();
		while (reader.next(key, value)) {
			// Read output, print vector, cluster ID 
			NamedVector vector = (NamedVector) value.getVector();
			String vectorName = vector.getName();
			System.out.println(vectorName + " belongs to cluster " + key.toString());
		}
		reader.close();
	
	}
	

}
