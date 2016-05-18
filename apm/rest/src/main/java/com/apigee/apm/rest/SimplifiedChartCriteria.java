package com.apigee.apm.rest;

public class SimplifiedChartCriteria {
	
	public static String TYPE_SESSION = "session";
	public static String TYPE_LOG = "log";
	public static String TYPE_NETWORK = "network";
	
	private Long chartCriteriaId;
	private Long instaOpsApplicationId;
	private String chartName;
	private String chartDescription;
	private String type; //session or log or network
	
	public Long getChartCriteriaId() {
		return chartCriteriaId;
	}
	public void setChartCriteriaId(Long chartCriteriaId) {
		this.chartCriteriaId = chartCriteriaId;
	}
	public Long getInstaOpsApplicationId() {
		return instaOpsApplicationId;
	}
	public void setInstaOpsApplicationId(Long instaOpsApplicationId) {
		this.instaOpsApplicationId = instaOpsApplicationId;
	}
	public String getChartName() {
		return chartName;
	}
	public void setChartName(String chartName) {
		this.chartName = chartName;
	}
	public String getChartDescription() {
		return chartDescription;
	}
	public void setChartDescription(String chartDescription) {
		this.chartDescription = chartDescription;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	
	

}
