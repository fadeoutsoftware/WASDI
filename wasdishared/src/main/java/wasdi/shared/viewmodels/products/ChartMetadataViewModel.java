package wasdi.shared.viewmodels.products;

public class ChartMetadataViewModel {

    /** Chart type */
    private String chartType;

    /** name used in the corresponding product to idenfity the x axis */
    private String xAxisName;

    /** name used in the corresponding product to idenfity the y axis */
    private String yAxisName;

    /** name used in the corresponding product to idenfity the chart data */
    private String chartDataName;

    public ChartMetadataViewModel() {
    }

    public ChartMetadataViewModel(String sChartType, String sXAxisName, String sYAxisName, String sChartDataName) {
        this.chartType = sChartType;
        this.xAxisName = sXAxisName;
        this.yAxisName = sYAxisName;
        this.chartDataName = sChartDataName;
    }

    public String getChartType() {
        return chartType;
    }

    public void setChartType(String sChartType) {
        this.chartType = sChartType;
    }

    public String getXAxisName() {
        return xAxisName;
    }

    public void setXAxisName(String sXAxisName) {
        this.xAxisName = sXAxisName;
    }

    public String getYAxisName() {
        return yAxisName;
    }

    public void setYAxisName(String sYAxisName) {
        this.yAxisName = sYAxisName;
    }

    public String getChartDataName() {
        return chartDataName;
    }

    public void setChartDataName(String sChartDataName) {
        this.chartDataName = sChartDataName;
    }

}
