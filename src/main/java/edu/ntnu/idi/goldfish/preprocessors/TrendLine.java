package edu.ntnu.idi.goldfish.preprocessors;

public interface TrendLine {
	public void setValues(double[] x, double[] y); // y ~ f(x)
	public double predict(double x); // get a predicted y for a given x
}
