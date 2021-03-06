package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.preprocessors.*;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorClustering.*;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorIBK.*;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorSMOreg.Kernel;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorStat.PredictionMethod;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static edu.ntnu.idi.goldfish.DataSet.*;


public class Main {

    public int i = 1;

    public static DataSet set;
	
	// disable Mahout logging output
	static {
		 System.setProperty("org.apache.commons.logging.Log",
		 "org.apache.commons.logging.impl.NoOpLog");
	}

	public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {

        System.out.println("Loading datasets and configurations");

        Set<String> options = new HashSet<String>(Arrays.asList(args));

        boolean doBaseline, doStat, doClustering, doMlr, doSmoreg, doAnn, doIbk, doNaiveBayes, doTime;
        doBaseline = options.contains("-baseline");
        doStat = options.contains("-stat");
        doTime = options.contains("-time");
        doClustering = options.contains("-clustering");
        doMlr = options.contains("-mlr");
        doSmoreg = options.contains("-smoreg");
        doAnn = options.contains("-ann");
        doIbk = options.contains("-ibk");
        doNaiveBayes = options.contains("-naivebayes");
//        doBaseline = doStat = doTime = doClustering = doMlr = doSmoreg = doAnn = doIbk = doNaiveBayes = true;


        List<Config> configs = new ArrayList<>();
        ResultList results = new ResultList();
        Columns cols = new Columns();

        cols.add("name", "%-11s", "%s");
        cols.add("average", "%4d", "%d");
        cols.add("RMSE", "%6.3f", "%.3f");
        cols.add("pseudoRatings", "%4d", "%d");
        cols.add("evalTime", "%6.0f", "%.0f");

        Config config;

        int average = 3000;

        /***********************************************************************************/
        // Baseline

        if(doBaseline)
        {

            Config baseLine = new Lynx()
                    .set("name", "baseline")
                    .set("model", yowBaseline.getModel())
                    .set("average", average);

            configs.add(baseLine);
        }
        
        /***********************************************************************************/
        // PreprocessorStat

        if(doStat)
        {
        	Config stat = new Lynx()
		    	.set("name", "stat")
		    	.set("model", yowImplicit.getModel())
		    	.set("preprocessor", PreprocessorStat.class)
		    	.set("average", average)
                .set("rating", 4);

            for (int minT = 15000; minT <= 30000; minT += 5000) {
		for (double corrLimit : new double[]{0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9}) {
                    for (PredictionMethod method : PredictionMethod.values()) {
                        config = stat.clone()
                                .set("minTimeOnPage", 0)
                                .set("correlationLimit", corrLimit)
                                .set("predictionMethod", method);

                        configs.add(config);
                    }
                }
            }


            cols.add("minTimeOnPage", "%5d", "%d");
            cols.add("correlationLimit", "%2.1f", "%.1f");
            cols.add("predictionMethod", "%16s", "%s");
        }

        /***********************************************************************************/
        // Time

        if(doTime)
        {

        	 Config time = new Lynx()
             .set("name", "time")
             .set("model", yowImplicit.getModel())
             .set("preprocessor", PreprocessorTime.class)
             .set("average", average);

		     for (int i = 15000; i <= 30000; i += 5000) {
		     	for (int j = 4; j <= 5; j++) {
		     		config = time.clone()
		     				.set("minTimeOnPage", i)
		     				.set("rating", j);
		     		configs.add(config);
					}
		     	}
		     
		     cols.add("minTimeOnPage", "%5d", "%d");
		     cols.add("rating", "%d", "%d");
        }
        
        /***********************************************************************************/
        // PreprocessorClustering

      if(doClustering)
      {
          Config clustering = new Lynx()
                  .set("name", "clustering")
                  .set("model", yowImplicit.getModel())
                  .set("preprocessor", PreprocessorClustering.class)
                  .set("average", average);


          for(double threshold : new double[]{0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1}) {
              for (Clusterer clusterer : Arrays.asList(Clusterer.SimpleKMeans, Clusterer.XMeans, Clusterer.DensityBased)) {
                  for (ClusterDataset dataset : ClusterDataset.values()) {
                      for (DistFunc distFunc : Arrays.asList(DistFunc.Euclidean, DistFunc.Manhattan)) {
                          config = clustering.clone()
                                  .set("clusterer", clusterer)
                                  .set("clusterDataset", dataset)
                                  .set("distFunc", distFunc)
                                  .set("threshold", threshold);
                          configs.add(config);
                      }
                  }
              }

              for (Clusterer clusterer : Arrays.asList(Clusterer.Cobweb, Clusterer.EM, Clusterer.FarthestFirst)) {
                  for (ClusterDataset dataset : ClusterDataset.values()) {
                      config = clustering.clone()
                              .set("clusterer", clusterer)
                              .set("clusterDataset", dataset)
                              .set("distFunc", DistFunc.None)
                              .set("threshold", threshold);
                      configs.add(config);
                  }
              }

              for (ClusterDataset dataset : ClusterDataset.values()) {
                  config = clustering.clone()
                          .set("clusterer", Clusterer.XMeans)
                          .set("clusterDataset", dataset)
                          .set("distFunc", DistFunc.Chebyshev)
                          .set("threshold", threshold);
                  configs.add(config);
              }

          }
          cols.add("clusterer", "%13s", "%s");
          cols.add("clusterDataset", "%18s", "%s");
          cols.add("distFunc", "%9s", "%s");
          cols.add("threshold", "%2.1f", "%.1f");
      }
        /***********************************************************************************/
        // PreprocessorMLR

        if(doMlr)
        {
            Config mlr = new Lynx()
                    .set("name", "MLR")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorMLR.class)
                    .set("average", average);

            for (int i = 1; i <= 3; i++) {
                config = mlr.clone()
                        .set("IVs", i);
                configs.add(config);
            }

            cols.add("IVs", "%2d", "%d");
        }

        /***********************************************************************************/
        // PreprocessorSMOreg

        if(doSmoreg)
        {
            Config smoreg = new Lynx()
                    .set("name", "smoreg")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorSMOreg.class)
                    .set("average", average)
                    .set("threshold", 0.0);

            for(double Cn = -15; Cn <= 15; Cn += 1) {

                // RBFKernel
                for(double gammaN = -6; gammaN <= 6; gammaN += 1) {
                    config = smoreg.clone()
                            .set("kernel", Kernel.RBFKernel)
                            .set("C", Math.pow(2, Cn))
                            .set("kernelGamma", Math.pow(2, gammaN));
                    configs.add(config);
                }
            }

            cols.add("kernel", "%20s", "%s");
            cols.add("C", "%6.5f", "%.5f");
            cols.add("kernelGamma", "%6.5f", "%.5f");
        }

        /***********************************************************************************/
        // PreprocessorANN
        if(doAnn)
        {
            Config ann = new Lynx()
                    .set("name", "ann")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorANN.class)
                    .set("average", average)
                    .set("threshold", 0.0);


            for (int epochs = 10; epochs <= 1000; epochs += 100) {
                for (double learningRate = 0.001; learningRate <= 0.3; learningRate += 0.02) {
                    for (double momentum : new double[]{0.1}) {
                        for (double threshold : new double[]{0.1, 0.2, 0.3}) {

                            config = ann.clone()
                            .set("learningRate", learningRate)
                            .set("momentum", momentum)
                            .set("epochs", epochs)
                            .set("threshold", threshold)
                            .set("neurons", "5");
                            configs.add(config);
                        }
                    }
                }
            }

            cols.add("learningRate", "%6.5f", "%.5f");
            cols.add("momentum", "%6.5f", "%.5f");
            cols.add("epochs", "%4d", "%d");
            cols.add("neurons", "%s", "%s");
        }

        /***********************************************************************************/
        // PreprocessorIBK
        if(doIbk)
        {
            Config ibk = new Lynx()
                    .set("name", "ibk")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorIBK.class)
                    .set("window", 0)
                    .set("method", NeighborSearchMethod.LinearNN)
                    .set("distanceMeasure", DistanceWeighting.Distance)
                    .set("average", average);

                for(ErrorMinimization minimization : ErrorMinimization.values()) {
                    for (int K = 1; K < 20; K += 2) {
                        for(double threshold : new double[]{0.8, 0.7, 0.6, 0.5, 0.4, 0.3}) {
                            config = ibk.clone()
                                .set("K", K)
                                .set("minimization", minimization)
                                .set("threshold", threshold);
                            configs.add(config);
                        }
                }
            }

            cols.add("threshold", "%2.1f", "%.1f");
            cols.add("K", "%2d", "%d");
            cols.add("distanceMeasure", "%15s", "%s");
            cols.add("minimization", "%25s", "%s");
            cols.add("method", "%10s", "%s");
        }
        /***********************************************************************************/
        // PreprocessorNaiveBayes
        if(doNaiveBayes)
        {
            Config naivebayes = new Lynx()
                    .set("name", "naivebayes")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorNaiveBayes.class)
                    .set("average", average);

            for(double threshold : new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7}) {
                config = naivebayes.clone()
                        .set("threshold", threshold);

                configs.add(config);
            }

            cols.add("threshold", "%2.1f", "%.1f");
        }
       
        /***********************************************************************************/

        int total = configs.size();

		StopWatch.start("total evaluation");
        System.out.format("Starting evaluation of %d configurations.. \n", total);

        Evaluator.evaluate(configs, results, res ->
                System.out.format("%s  %5d/%d | %s\n", String.format("%1$tm-%1$td %1$tH:%1$tM:%1$tS", new Date()), this.i++, total, res.toString(cols.getPrintFormats())));

        System.out.println(StringUtils.repeat("=", 190));
        results.printSummary(cols.getPrintFormats());
//        results.print(cols.getPrintFormats());

        System.out.format("Evaluated %d configurations in %s \n", configs.size(), StopWatch.str("total evaluation"));
        results.save(cols.getSaveFormats());

    }
}
