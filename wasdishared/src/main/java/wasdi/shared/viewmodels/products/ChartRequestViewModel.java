package wasdi.shared.viewmodels.products;

/**
 * Request DTO for chart data endpoint
 */
public class ChartRequestViewModel {
	
	private String workspace;
	private String fileName;
	private String chartType;
	private String xAxis;
	private String yAxis;
	private String dataLabel;

	public ChartRequestViewModel() {
	}

	public ChartRequestViewModel(String sWorkspace, String sFileName, String sChartType, String sXAxis, String sYAxis, String sDataLabel) {
		this.workspace = sWorkspace;
		this.fileName = sFileName;
		this.chartType = sChartType;
		this.xAxis = sXAxis;
		this.yAxis = sYAxis;
		this.dataLabel = sDataLabel;
	}

	public String getWorkspace() {
		return workspace;
	}

	public void setWorkspace(String sWorkspace) {
		this.workspace = sWorkspace;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String sName) {
		this.fileName = sName;
	}

	public String getChartType() {
		return chartType;
	}

	public void setChartType(String sChartType) {
		this.chartType = sChartType;
	}

	public String getXAxis() {
		return xAxis;
	}

	public void setXAxis(String sXAxis) {
		this.xAxis = sXAxis;
	}

	public String getYAxis() {
		return yAxis;
	}

	public void setYAxis(String sYAxis) {
		this.yAxis = sYAxis;
	}

	public String getDataLabel() {
		return dataLabel;
	}

	public void setDataLabel(String sChartData) {
		this.dataLabel = sChartData;
	}
}
