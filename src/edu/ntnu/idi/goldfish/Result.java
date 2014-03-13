package edu.ntnu.idi.goldfish;

import java.util.List;

public class Result {

	Configuration configuration;
	
	String name = null;
	
	// RMSE
	double RMSE;
	
	// AAD/MAE 
	double AAD;
	
	// IR stats (precision, recall)
	double precision;
	double recall;
	
	// model build timing
	long buildTime;
	
	// recommendation timing
	long recTime;
	
	// configuration time
	long evalTime;
	

	public Result(Configuration configuration, double RMSE, double AAD, double precision, double recall, long buildTime, long recTime, long evalTime) {
		this.configuration = configuration;

		this.RMSE = RMSE;
		this.AAD = AAD;
		
		this.precision = precision;
		this.recall = recall;
		
		this.buildTime = buildTime;
		this.recTime = recTime;
		this.evalTime = evalTime;
	}
	
	
	public int getTopN() {
		return configuration.getTopN();
	}
	
	public double getKTL() {
		return configuration.getKTL();
	}
	
	public String getName() {
		return name != null ? name : configuration.toString();
	}
	
	public String getSimilarity() {
		return configuration instanceof MemoryBased ? ((MemoryBased) configuration).similarity.toString() : "";
	}
	
	public String toString() {
		
		Formatter formats = new Formatter();
		formats.put("%-11s", getName());
		formats.put("%19s", getSimilarity());
		formats.put("K/T/L: %5.2f", getKTL());
		formats.put("Top-N: %3d", getTopN());
		formats.put("RMSE: %6.3f", RMSE);
		formats.put("AAD: %6.3f", AAD);
		formats.put("Precision: %6.3f", precision);
		formats.put("Recall: %6.3f", recall);
		formats.put("Build time: %4d", buildTime);
		formats.put("Rec time: %2d", recTime);
		formats.put("Eval time: %2d", evalTime);

		return formats.join(" | ");
	}
	
	public String toTSV() {
		Formatter formats = new Formatter();
		formats.put("%s", getName());
		formats.put("%s", getSimilarity());
		formats.put("%.2f", getKTL());
		formats.put("%d", getTopN());
		formats.put("%.3f", RMSE);
		formats.put("%.3f", AAD);
		formats.put("%.3f", precision);
		formats.put("%.3f", recall);
		formats.put("%d", buildTime);
		formats.put("%d", recTime);
		formats.put("%d", evalTime);
		
		return formats.join("\t");
	}
	
	public static Result getTotal(List<Result> results) {
		double totalRMSE = 0, totalAAD = 0, totalPrecision = 0, totalRecall = 0;
		long totalBuildTime = 0, totalRecTime = 0, totalEvalTime = 0;
		for(Result res : results) {
			totalRMSE += res.RMSE;
			totalAAD += res.AAD;
			totalPrecision += res.precision;
			totalRecall += res.recall;
			totalBuildTime += res.buildTime;
			totalRecTime += res.recTime;
			totalEvalTime += res.evalTime;
		}
		Result result = new Result(results.get(0).configuration, totalRMSE, totalAAD, totalPrecision, totalRecall, totalBuildTime, totalRecTime, totalEvalTime);
		result.name = "# TOTALS";
		return result;	}
	
	public static Result getAverage(List<Result> results) {
		int N = results.size();
		Result total = getTotal(results);
		total = new Result(results.get(0).configuration, total.RMSE / N, total.AAD / N, total.precision / N, total.recall / N, total.buildTime / N, total.recTime / N, total.evalTime / N);
		total.name = "# AVERAGE"; 
		return total;
	}
}
