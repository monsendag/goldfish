package edu.ntnu.idi.goldfish.preprocessors;

import edu.ntnu.idi.goldfish.configurations.Config;
import org.apache.mahout.cf.taste.model.DataModel;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMOreg;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils;

public class PreprocessorSMOreg extends PreprocessorClassifier {

    public enum Kernel {
        PolyKernel,
        NormalizedPolyKernel,
        RBFKernel
    }

    @Override
    public DataModel preprocess(Config config) throws Exception {
        Classifier classifier = new SMOreg();

        Kernel kernel = config.get("kernel");
        String kernelString = "weka.classifiers.functions.supportVector."+kernel;

        double C = config.get("C", 1.0);
        int kernelCacheSize = config.get("kernelCacheSize", 0);
        double kernelGamma = config.get("kernelGamma", 0.01);
        double kernelExponent = config.get("kernelExponent", 2.0);

        String kernelOptions;
        switch(kernel){
            default:
            case PolyKernel:
            case NormalizedPolyKernel:
                kernelOptions = String.format("\"%s -C %d -E %f\"", kernelString, kernelCacheSize, kernelExponent);
                break;
            case RBFKernel:
                kernelOptions = String.format("\"%s -C %d -G %f\"", kernelString, kernelCacheSize, kernelGamma);
        }

        String optimizer = "\"weka.classifiers.functions.supportVector.RegSMOImproved -L 0.001 -W 1 -P 1.0E-12 -T 0.001 -V\"";
        String options = String.format("-C %f -N 0 -I %s -K %s", C, optimizer, kernelOptions).replace(",",".");

        // set options on classifier
        classifier.setOptions(Utils.splitOptions(options));

        return classify(config, classifier, getDataset());
    }

    public static Instances getDataset() throws Exception {
        String file = "real/yow-preprocess-clustering-timeonpage-timeonmouse.arff";
        Instances dataset = new ConverterUtils.DataSource("datasets/yow-userstudy/arff/" + file).getDataSet();
        dataset.setClassIndex(0);
        return dataset;
    }
}