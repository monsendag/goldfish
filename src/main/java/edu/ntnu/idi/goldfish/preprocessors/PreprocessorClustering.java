package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.mahout.DBModel;
import weka.classifiers.meta.ClassificationViaClustering;
import weka.clusterers.*;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PreprocessorClustering extends Preprocessor {

	private ClassificationViaClustering cvc = null;
	private Instances data = null;
	private final int NUM_CLUSTERS = 5;
	
	public static enum Clusterer { SimpleKMeans, DensityBased, EM, FarthestFirst, Cobweb, XMeans }
	public static enum ClusterDataset { TimeOnPage, TimeOnPageAndMouse, PageTimesMouse}
	public static enum DistFunc { Euclidean, Manhattan, Chebyshev, None }
	
	public void buildFarthestFirstClusterer() throws Exception{
		FarthestFirst ff = new FarthestFirst();
		ff.setNumClusters(NUM_CLUSTERS);
		
		cvc.setClusterer(ff);
	}
	
	public void buildHierarchicalClusterer(){
		HierarchicalClusterer hc = new HierarchicalClusterer();
		hc.setNumClusters(NUM_CLUSTERS);
		
		cvc.setClusterer(hc);
	}
	
	public void buildSimpleKMeansClusterer(DistanceFunction distanceFunction) throws Exception{
		SimpleKMeans skm = new SimpleKMeans();
		skm.setNumClusters(NUM_CLUSTERS);
		skm.setDistanceFunction(distanceFunction);

		cvc.setClusterer(skm);
	}
	
	public void buildDensityBasedClusterer(DistanceFunction distanceFunction) throws Exception{
		SimpleKMeans skm = new SimpleKMeans();
		skm.setNumClusters(NUM_CLUSTERS);
		skm.setDistanceFunction(distanceFunction);
		
		MakeDensityBasedClusterer dbc = new MakeDensityBasedClusterer();
		dbc.setClusterer(skm);
		
		cvc.setClusterer(dbc);
	}
	
	public void buildEMClusterer() throws Exception{
		EM em = new EM();
		em.setNumClusters(NUM_CLUSTERS);
		
		cvc.setClusterer(em);
	}
	
	public void buildCobwebClusterer(){
		Cobweb c = new Cobweb();
		
		cvc.setClusterer(c);
	}
	
	public void buildsIBClusterer(){
		sIB s = new sIB();
		s.setNumClusters(NUM_CLUSTERS);
		
		cvc.setClusterer(s);
	}
	
	public void buildXMeansClusterer(DistanceFunction distanceFunction){
		XMeans xm = new XMeans();
		xm.setMaxNumClusters(NUM_CLUSTERS);
		xm.setMinNumClusters(NUM_CLUSTERS);
		xm.setDistanceF(distanceFunction);
		
		cvc.setClusterer(xm);
	}
		
	@Override
	public DBModel getProcessedModel(Config config) throws Exception {
        DBModel model = config.get("model");
        Clusterer clusterer = config.get("clusterer");
        ClusterDataset clusterDataset = config.get("clusterDataset");
        DistFunc distFunc = config.get("distFunc");
        DistanceFunction distanceFunction = null;
        
        switch (distFunc) {
		case Euclidean:
			distanceFunction = new EuclideanDistance();
			break;
		case Manhattan:
			distanceFunction = new ManhattanDistance();
			break;
		case Chebyshev:
			distanceFunction = new ChebyshevDistance();
		case None:
			distanceFunction = null;
			break;
		default:
			distanceFunction = new EuclideanDistance();
			break;
		}
        
        // read the dataset and create instances for training and evaluating
        String path = "";
        switch (clusterDataset) {
		case TimeOnPage:
			path = "datasets/yow-userstudy/arff/yow-preprocess-clustering-timeonpage.arff";
			break;
		case TimeOnPageAndMouse:
			path = "datasets/yow-userstudy/arff/yow-preprocess-clustering-timeonpage-timeonmouse.arff";
			break;
		case PageTimesMouse:
			path = "datasets/yow-userstudy/arff/yow-preprocess-clustering-timeonpage-timeonmouse-pagetimesmouse.arff";
			break;
		default:
			path = "datasets/yow-userstudy/arff/yow-preprocess-clustering-timeonpage.arff";
			break;
		}
     	BufferedReader reader = new BufferedReader(
     			new FileReader(path));
     	data = new Instances(reader);
     	data.setClassIndex(0);
 		
 		// initialize ClassificationViaClustering
 		cvc = new ClassificationViaClustering();
 		
 		switch (clusterer) {
 		case SimpleKMeans:
 			buildSimpleKMeansClusterer(distanceFunction);
 			break;
 		case DensityBased:
 			buildDensityBasedClusterer(distanceFunction);
 			break;
 		case EM:
 			buildEMClusterer();
 			break;
 		case FarthestFirst:
 			buildFarthestFirstClusterer();
 			break;
 		case Cobweb:
 			buildCobwebClusterer();
 			break;
// 		case sIB:
// 			buildsIBClusterer();
// 			break;
 		case XMeans:
 			buildXMeansClusterer(distanceFunction);
 			break;
 		default:
 			buildSimpleKMeansClusterer(distanceFunction);
 			break;
 		}
 		
 		cvc.buildClassifier(data);

		List<DBModel.DBRow> results = model.getFeedbackRows().stream().filter(row -> row.rating == 0).collect(Collectors.toList());
		for(DBModel.DBRow row : results) {
			Instance i = null;
			switch (clusterDataset) {
			case TimeOnPage:
				i = new Instance(1, new double[]{-1, row.timeonpage});
				break;
			case TimeOnPageAndMouse:
				i = new Instance(1, new double[]{-1, row.timeonpage, row.timeonmouse});
			case PageTimesMouse:
				i = new Instance(1, new double[]{-1, row.timeonpage, row.timeonmouse, row.pagetimesmouse});
			default:
				i = new Instance(1, new double[]{-1, row.timeonpage});
				break;
			}
			
			i.setDataset(data);
			int index = (int) (cvc.classifyInstance(i)); // zero indexed
            // get rating value of classification
            double[] values = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};
            double rating = values[index];

			model.setPreference(row.userid, row.itemid, (float) Math.round(rating));
			addPseudoPref(row.userid, row.itemid);
		}
		
		return model;
	}
	
	public void ManualClassificationViaClustering() throws Exception{
		
		// create the simpleKMeans cluster
		SimpleKMeans clusterer = new SimpleKMeans();
		String[] options = new String[1];
		options[0] = "-V";
		clusterer.setOptions(options);
		clusterer.setNumClusters(5);
		
		//read the dataset and create instances for training and evaluating
		BufferedReader reader = new BufferedReader(
				new FileReader("datasets/yow-userstudy/arff/yow-preprocess-clustering.arff"));
		Instances data = new Instances(reader);
		Instances evalData = new Instances(data);
		evalData.setClassIndex(0);

		// remove the class index (user_like) since we are going to create clusters with time on page and mouse
		data = removeAttribute(data,1);
		
		clusterer.buildClusterer(data);
		
		// evaluate clusterer
	    ClusterEvaluation eval = new ClusterEvaluation();
	    eval.setClusterer(clusterer);
	    eval.evaluateClusterer(evalData);
	    
		System.out.println(eval.clusterResultsToString());

		Map<Integer,Integer> clusterToRating = new HashMap<Integer,Integer>();
		// map clusters to ratings
		int[] classToCluster = eval.getClassesToClusters();
		for (int i = 0; i < classToCluster.length; i++) {
			clusterToRating.put(i, (classToCluster[i]+1));
		}
		
//		<--- Put this where the pseduorating is going to be calculated --->
//		int cluster = clusterer.clusterInstance(new Instance(1, 
//		new double[]{pref.getValue(TIME_ON_PAGE_INDEX),pref.getValue(TIME_ON_MOUSE_INDEX)}));
//
//		int pseudorating = clusterToRating.get(cluster);
	}
	
	private Instances removeAttribute(Instances data, int attributeIndex) throws Exception {
		Remove remove = new Remove(); // new instance of filter
		String[] removeOptions = new String[2];
		removeOptions[0] = "-R"; // "range"
		removeOptions[1] = ""+attributeIndex; // we want to ignore the attribute that is in the position '1' (first)
		remove.setOptions(removeOptions); // set options
		remove.setInputFormat(data); // inform filter about dataset
		data = Filter.useFilter(data, remove);
		return data;
	}
}
