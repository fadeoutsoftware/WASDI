package wasdi.shared.viewmodels.products;

/**
 * Product View Model.
 * Used as entry to fill the client editor product tree. 
 * 
 * Contains product name, file and links to metadata and band groups.
 *
 * This View Model is SAVED DIRECTLY IN THE DB. It is the only case, for historical reason: this
 * VM contains all the tree of the product bands that are not saved by wasdi in any other way.
 * 
 * Here .name is the file name without extension
 * 		.fileName is the file name with extension
 * 		.productFriendlyName is a name that the user can assign to this product
 * 
 * Created by s.adamo on 18/05/2016.
 */
public class ProductViewModel {

    private String name;

    private String fileName;

    private String productFriendlyName;

    private MetadataViewModel metadata;

    private NodeGroupViewModel bandsGroups;
    
    private String metadataFileReference;
    
    private boolean metadataFileCreated = false;
    
    private String style;

    private String description;
    
    private Boolean isChart = false;
    
    private ChartMetadata chartMetadata;

	/**
	 * Inner class to hold chart metadata when isChart is true
	 */
	public static class ChartMetadata {
		private String chartType;
		private String xAxisName;
		private String yAxisName;
		private String chartDataName;

		public ChartMetadata() {
		}

		public ChartMetadata(String sChartType, String sXAxisName, String sYAxisName, String sChartDataName) {
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
    
	public ProductViewModel() {
	}
    
    public ProductViewModel(ProductViewModel base) {
		setBandsGroups(base.getBandsGroups());
		setFileName(base.getFileName());
		setMetadata(base.getMetadata());
		setName(base.getName());
		setProductFriendlyName(base.getProductFriendlyName());
		setStyle(base.getStyle());
		setDescription(base.getDescription());
    }


    public MetadataViewModel getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataViewModel metadata) {
        this.metadata = metadata;
    }

    public NodeGroupViewModel getBandsGroups() {
        return bandsGroups;
    }

    public void setBandsGroups(NodeGroupViewModel bandsGroups) {
        this.bandsGroups = bandsGroups;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getProductFriendlyName() {
        return productFriendlyName;
    }

    public void setProductFriendlyName(String productFriendlyName) {
        this.productFriendlyName = productFriendlyName;
    }

	public String getMetadataFileReference() {
		return metadataFileReference;
	}

	public void setMetadataFileReference(String metadataFileReference) {
		this.metadataFileReference = metadataFileReference;
	}

	public boolean getMetadataFileCreated() {
		return metadataFileCreated;
	}

	public void setMetadataFileCreated(boolean metadataFileCreated) {
		this.metadataFileCreated = metadataFileCreated;
	}
	
    public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean isChart() {
        return isChart;
    }

    public void setChart(Boolean bIsChart) {
        this.isChart = bIsChart;
    }

    public ChartMetadata getChartMetadata() {
        return chartMetadata;
    }

    public void setChartMetadata(ChartMetadata oChartMetadata) {
        this.chartMetadata = oChartMetadata;
    }

}
