package edu.ntnu.idi.goldfish;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class Test {

//	public static void main(String[] args) throws Exception {
//        SMDataModel model;
//        model = new SMDataModel(new File("datasets/yow-userstudy/like-timeonpage-timeonmouse.csv"));
//
//        System.out.println(String.format("Density unprocessed: %f", model.getDensity()));
//        PreprocessorPuddis pre = new PreprocessorPuddis();
//        pre.preprocess(model);
//        System.out.println(String.format("Density processed: %f", model.getDensity()));
//
//	}
	
	public static void main(String[] args) {
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		double[] y = new double[]{11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0};
		double[][] x = new double[7][];
		x[0] = new double[]{0, 0, 0, 0, 0};
		x[1] = new double[]{2.0, 0, 0, 0, 0};
		x[2] = new double[]{0, 3.0, 0, 0, 0};
		x[3] = new double[]{0, 0, 4.0, 0, 0};
		x[4] = new double[]{0, 0, 0, 5.0, 0};
		x[5] = new double[]{0, 0, 0, 0, 6.0}; 
		x[6] = new double[]{0, 0, 0, 2.0, 6.0}; 
		regression.newSampleData(y, x);

		double[] beta = regression.estimateRegressionParameters();       
		double[] residuals = regression.estimateResiduals();
		double[][] parametersVariance = regression.estimateRegressionParametersVariance();
		double regressandVariance = regression.estimateRegressandVariance();
		double rSquared = regression.calculateRSquared();
		double sigma = regression.estimateRegressionStandardError();
		
		System.out.println("Estimations:");
		double[] first = {1.0,2.0,3.0,4.0,5.0};
		double[] last = {0,0,0,0,6.0};
		System.out.println("x = {1,2,3,4,5}, y = " + calculateEstimation(first, beta));
		System.out.println("x = {0,0,0,0,6}, y = " + calculateEstimation(last, beta));
		
		System.out.println("Beta:");
		printSingleForLoop(beta);
		System.out.println("Residuals:");
		printSingleForLoop(residuals);
		System.out.println("Parameter Variance");
		printDoubleForLoop(parametersVariance);
		System.out.println("Regression Variance: " + regressandVariance);
		System.out.println("rSquared: " + rSquared);
		System.out.println("Sigma: " + sigma);
	}
	
	public static double calculateEstimation(double[] x, double[] beta){
		double result = beta[0];
		for (int i = 1; i < beta.length; i++) {
			result += beta[i] * x[i-1];
		}
		return result;
	}
	
	public static void printSingleForLoop(double[] array){
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i] + ", ");
		}
		System.out.println("");
	}
	
	public static void printDoubleForLoop(double[][] array){
		for (int i = 0; i < array.length; i++) {
			System.out.println(i + ":");
			for (int j = 0; j < array[i].length; j++) {
				System.out.print(array[i][j] + ", ");
			}
			System.out.println("");
		}
	}
	
}
