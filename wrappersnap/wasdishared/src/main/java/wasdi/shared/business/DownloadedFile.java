package wasdi.shared.business;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * Downloaded File Entity
 * Represents a EO File ingested in WASDI and all the products generated.
 * 
 * 
 * 
 * Created by p.campanella on 11/11/2016.
 * 
 */
public class DownloadedFile {
	
	/**
	 * File Name
	 */
    private String fileName;
    /**
     * Full File Path
     */
    private String filePath;
    /**
     * Boundig Box of the file
     */
    private String boundingBox;
    /**
     * Reference Date
     */
    private String refDate;
    /**
     * File Category
     */
    private String category = DownloadedFileCategory.PUBLIC.name();
    
    /**
     * Default style to use for the bands of this file
     */
    private String defaultStyle;
	/**
	 * Description
	 */
    private String description;
    
    /**
     * Platform (aka Mission)
     */
    private String platform;
    
	/**
     * Product View Model
     * NOTE: Usually do not use View Models in entities. But this is more an entity that not a view model...
     */
    private ProductViewModel productViewModel;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public ProductViewModel getProductViewModel() {
        return productViewModel;
    }

    public void setProductViewModel(ProductViewModel productViewModel) {
        this.productViewModel = productViewModel;
    }

    public String getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(String boundingBox) {
        this.boundingBox = boundingBox;
    }

	public String getRefDate() {
		return refDate;
	}

	public void setRefDate(String refDate) {
		this.refDate = refDate;
	}

	public void setRefDate(Date refDate) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(tz);
		this.refDate = df.format(refDate);
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
    
    public String getDefaultStyle() {
		return defaultStyle;
	}

	public void setDefaultStyle(String defaultStyle) {
		this.defaultStyle = defaultStyle;
	}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

}
