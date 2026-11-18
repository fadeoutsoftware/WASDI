package wasdi.shared.viewmodels.products;

/**
 * Request DTO for chart data endpoint
 * 
 * @author Copilot
 */
public class ChartRequestViewModel {
	
	private String workspace;
	private String name;
	private String chartType;
	private String xAxis;
	private String yAxis;
	private String chartData;

	public ChartRequestViewModel() {
	}

	public ChartRequestViewModel(String sWorkspace, String sName, String sChartType, String sXAxis, String sYAxis, String sChartData) {
		this.workspace = sWorkspace;
		this.name = sName;
		this.chartType = sChartType;
		this.xAxis = sXAxis;
		this.yAxis = sYAxis;
		this.chartData = sChartData;
	}

	public String getWorkspace() {
		return workspace;
	}

	public void setWorkspace(String sWorkspace) {
		this.workspace = sWorkspace;
	}

	public String getName() {
		return name;
	}

	public void setName(String sName) {
		this.name = sName;
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

	public String getChartData() {
		return chartData;
	}

	public void setChartData(String sChartData) {
		this.chartData = sChartData;
	}
}
