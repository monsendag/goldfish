package edu.ntnu.idi.goldfish;

import edu.ntnu.idi.goldfish.mahout.SMDataModel;
import edu.ntnu.idi.goldfish.preprocessors.Preprocessor;

import java.io.File;

public class Test {

	public static void main(String[] args) throws Exception {
        SMDataModel model;
        model = new SMDataModel(new File("datasets/yow-userstudy/like-timeonpage-timeonmouse.csv"));

        System.out.println(String.format("Density unprocessed: %f", model.getDensity()));
        Preprocessor pre = new Preprocessor();
        pre.preprocess(model);
        System.out.println(String.format("Density processed: %f", model.getDensity()));

	}
	
}
