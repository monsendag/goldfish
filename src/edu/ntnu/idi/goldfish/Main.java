package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.configurations.Config;
import edu.ntnu.idi.goldfish.configurations.Lynx;
import edu.ntnu.idi.goldfish.preprocessors.*;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorClustering.ClusterDataset;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorClustering.Clusterer;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorClustering.DistFunc;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorIBK.DistanceWeighting;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorIBK.ErrorMinimization;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorIBK.NeighborSearchMethod;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorPuddis.PredMethod;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorSMOreg.Kernel;
import edu.ntnu.idi.goldfish.preprocessors.PreprocessorStat.PredictionMethod;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.ntnu.idi.goldfish.DataSet.*;

public class Main {

    public static DataSet set;
	
	// disable Mahout logging output
	static {
		 System.setProperty("org.apache.commons.logging.Log",
		 "org.apache.commons.logging.impl.NoOpLog");
	}

	public static void main(String[] args) throws Exception {

        List<Config> configs = new ArrayList<>();
        ResultList results = new ResultList();
        Columns cols = new Columns();
        cols.add("name", "average", "RMSE", "evalTime");

        Config config;

        /***********************************************************************************/
        // Baseline

        if(false)
        {

            Config baseLine = new Lynx()
                    .set("name", "baseline")
                    .set("model", yowBaseline.getModel())
                    .set("average", 10000);

            configs.add(baseLine);
        }
        /***********************************************************************************/
        // PreprocessorPuddis

        if(false)
        {
            Config puddis = new Lynx()
                    .set("name", "puddis")
                    .set("model", yowSMImplicit.getModel())
                    .set("preprocessor", PreprocessorPuddis.class)
                    .set("average", 10000);

            for (int minT = 15000; minT <= 30000; minT += 5000) {
                for (double corrLimit = 0.4; corrLimit <= 0.8; corrLimit += 0.1) {
                    for (PredMethod method : PredMethod.values()) {
                        config = puddis.clone()
                                .set("minTimeOnPage", minT)
                                .set("correlationLimit", corrLimit)
                                .set("predictionMethod", method);

//                        configs.add(conf);
                    }
                }
            }

            cols.add("minTimeOnPage", "correlationLimit", "predictionMethod");
        }
        
        /***********************************************************************************/
        // PreprocessorStat
        
        if(false)
        {
        	Config stat = new Lynx()
		    	.set("name", "stat")
		    	.set("model", yowImplicit.getModel())
		    	.set("preprocessor", PreprocessorStat.class)
		    	.set("average", 10000);

            for (int minT = 15000; minT <= 30000; minT += 5000) {
                for (double corrLimit = 0.4; corrLimit <= 0.8; corrLimit += 0.1) {
                    for (PredictionMethod method : PredictionMethod.values()) {
                        config = stat.clone()
                                .set("minTimeOnPage", minT)
                                .set("correlationLimit", corrLimit)
                                .set("predictionMethod", method);

//                        configs.add(conf);
                    }
                }
            }

            cols.add("minTimeOnPage", "correlationLimit", "predictionMethod");
        }

        /***********************************************************************************/

        // PreprocessorClustering

        if(false)
        {
            Config clustering = new Lynx()
                    .set("name", "clustering")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorClustering.class)
                    .set("average", 10000);

            for(PreprocessorClustering.Clusterer clusterer : PreprocessorClustering.Clusterer.values()) {
                for(PreprocessorClustering.ClusterDataset dataset : PreprocessorClustering.ClusterDataset.values()) {
                    config = clustering.clone()
                            .set("clusterer", clusterer)
                            .set("clusterDataset", dataset);
                    configs.add(config);
                }
            }

            cols.add("clusterer", "clusterDataset");
        }

        /***********************************************************************************/
        // PreprocessorMLR

        if(false)
        {
            Config mlr = new Lynx()
                    .set("name", "MLR")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorMLR.class)
                    .set("average", 10);

            for (int i = 1; i <= 3; i++) {
                config = mlr.clone()
                        .set("IVs", i);
                configs.add(config);
            }

            cols.add("IVs");
        }

        /***********************************************************************************/
        // PreprocessorSMOreg

        if(false)
        {
            Config smoreg = new Lynx()
                    .set("name", "smoreg")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorSMOreg.class)
                    .set("average", 10);

            for(double C = 1.0; C <= 2.0; C += 0.1) {

                // RBFKernel
                for(double gamma = 1.0; gamma <= 2.0; gamma += 0.1) {
                    config = smoreg.clone()
                            .set("kernel", Kernel.RBFKernel)
                            .set("C", C)
                            .set("kernelGamma", gamma);
                    configs.add(config);
                }

                // PolyKernel, NormalizedPolyKernel
                List<Kernel> kernels = Arrays.asList(Kernel.PolyKernel, Kernel.NormalizedPolyKernel);
                for(Kernel kernel : kernels) {
                    for(double exponent = 1.0; exponent <= 2.0; exponent += 0.1) {
                        config = smoreg.clone()
                                .set("kernel", kernel)
                                .set("C", C)
                                .set("kernelExponent", exponent);
                        configs.add(config);
                    }
                }
            }

            cols.add("kernel", "C", "kernelGamma", "kernelExponent");
        }

        /***********************************************************************************/
        // PreprocessorANN
        if(false)
        {
            Config ann = new Lynx()
                    .set("name", "ann")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorANN.class)
                    .set("average", 10);


            for (double learningRate = 0.1; learningRate <= 1; learningRate += 0.1) {
                for (double momentum = 0.2; momentum < 1; momentum += 0.1) {

                    config = ann.clone()
                        .set("learningRate", learningRate)
                        .set("momentum", momentum)
                        .set("epochs", 500)
                        .set("neurons", "a");
                    configs.add(config);
                }
            }
            cols.add("learningRate");
        }

        /***********************************************************************************/
        // PreprocessorIBK
        if(false)
        {
            Config ibk = new Lynx()
                    .set("name", "ibk")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorIBK.class)
                    .set("window", 0)
                    .set("average", 10);

            for(DistanceWeighting weighting : DistanceWeighting.values()) {
                for(ErrorMinimization minimization : ErrorMinimization.values()) {
                    for(NeighborSearchMethod method : NeighborSearchMethod.values()) {

                        for (int K = 1; K < 5; K++) {
                            config = ibk.clone()
                                .set("K", 5)
                                .set("distanceMeasure", weighting)
                                .set("minimization", minimization)
                                .set("method", method);

                            configs.add(config);
                        }
                    }
                }
            }

        }
        /***********************************************************************************/
        // PreprocessorNaiveBayes
        if(false)
        {
            Config naivebayes = new Lynx()
                    .set("name", "naivebayes")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorNaiveBayes.class)
                    .set("average", 10);

            configs.add(naivebayes);
        }
        /***********************************************************************************/
        // PreprocessorClustering

        if(false)
        {
            Config clustering = new Lynx()
                    .set("name", "clustering")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorClustering.class)
                    .set("average", 10);

            for(Clusterer clusterer : Arrays.asList(Clusterer.SimpleKMeans, Clusterer.XMeans, Clusterer.DensityBased)) {
                for(ClusterDataset dataset : ClusterDataset.values()) {
                	for (DistFunc distFunc : Arrays.asList(DistFunc.Euclidean, DistFunc.Manhattan)) {
                		config = clustering.clone()
                				.set("clusterer", clusterer)
                				.set("clusterDataset", dataset)
                				.set("distFunc", distFunc);
                		configs.add(config);
					}
                }
            }
            
            for(Clusterer clusterer : Arrays.asList(Clusterer.Cobweb, Clusterer.EM, Clusterer.FarthestFirst)) {
            	for(ClusterDataset dataset : ClusterDataset.values()) {
                    config = clustering.clone()
                            .set("clusterer", clusterer)
                            .set("clusterDataset", dataset)
                            .set("distFunc", DistFunc.None);
                    configs.add(config);
                }
            }
            
            for (ClusterDataset dataset : ClusterDataset.values()) {
				config = clustering.clone()
						.set("clusterer", Clusterer.XMeans)
						.set("clusterDataset", dataset)
						.set("distFunc", DistFunc.Chebyshev);
				configs.add(config);
			}
            
            cols.add("clusterer", "clusterDataset", "distFunc");
        }

        /***********************************************************************************/
        // PreprocessorMLR
        if(false)
        {
            Config mlr = new Lynx()
                    .set("name", "MLR")
                    .set("model", yowImplicit.getModel())
                    .set("preprocessor", PreprocessorMLR.class)
                    .set("average", 10);

            for (int i = 1; i <= 3; i++) {
                config = mlr.clone()
                        .set("IVs", i);
                configs.add(config);
            }
            
            cols.add("IVs");
        }

        /***********************************************************************************/

		StopWatch.start("total evaluation");
        System.out.format("Starting evaluation of %d configurations \n", configs.size());
        Evaluator.evaluate(configs, results, res -> System.out.println(res.toString(cols.getPrintFormats())));

        System.out.println(StringUtils.repeat("=", 190));
        results.printSummary(cols.getPrintFormats());

        System.out.format("Evaluated %d configurations in %s \n", configs.size(), StopWatch.str("total evaluation"));
        results.save(cols.getSaveFormats());

    }
}
