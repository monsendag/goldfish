package edu.ntnu.idi.goldfish;

import org.apache.mahout.cf.taste.eval.IRStatistics;


public class Result {

	RecommenderWrapper recommender;
	
	// RMSE
	double RMSE;
	
	// AAD/MAE 
	double AAD;
	
	int topN;
	
	// IR stats (precision, recall)
	IRStatistics irStats;
	
	// model build timing
	long buildTime;
	
	// recommendation timing
	long recTime;
	
	public Result(RecommenderWrapper recommender, int topN, double RMSE, double AAD, IRStatistics irStats, long buildTime, long recTime) {
		this.recommender = recommender;

		// NaN is messing up sorting, so we overwrite it
//		if(Double.isNaN(RMSE)) RMSE = 10;
		
		this.topN = topN;
		
		this.RMSE = RMSE;
		this.AAD = AAD;
		this.irStats = irStats;
		
		this.buildTime = buildTime;
		this.recTime = recTime;
	}
	
	public int getTopN() {
		return topN;
	}
	
	public double getKTL() {
		return recommender.getKTL();
	}
	
	public String getSimilarity() {
		return recommender instanceof MemoryBased ? ((MemoryBased) recommender).similarity.toString() : "";
	}
	
	public String toString() {
		return String.format(
			"%-40s | %19s | %6.2f | RMSE %6.3f | AAD: %6.3f | Precision: %6.3f | Recall %6.3f | Build time %7d | Rec time %7d", 
				recommender, getSimilarity(), getKTL(), getTopN(), RMSE, AAD, getPrecision(), getRecall(), getBuildTime(), getRecTime());
	}
	
	public String toCSV() {
		return String.format("%s,%.3f,%.3f,%.3f,%.3f,%d,%d", 
				recommender.toString(true), getSimilarity(), getKTL(), getTopN(), RMSE, AAD, getPrecision(), getRecall(), getBuildTime(), getRecTime());
	}
	
	public long getBuildTime() {
		return buildTime;
	}
	
	public long getRecTime() {
		return recTime;
	}
	
	public double getPrecision() {
		return irStats.getPrecision();
	}
	
	public double getRecall() {
		return irStats.getRecall();
	}

}
