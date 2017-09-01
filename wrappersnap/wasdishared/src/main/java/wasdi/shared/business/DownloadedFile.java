package wasdi.shared.business;

import java.util.Date;

import wasdi.shared.viewmodels.ProductViewModel;

/**
 * Created by p.campanella on 11/11/2016.
 */
public class DownloadedFile {
    private String fileName;
    private String filePath;
    private String boundingBox;
    private Date refDate;
    private String category = DownloadedFileCategory.PUBLIC.name();
    
    // NOTE: Usually do not use View Models in entities. But this is more an entity that not a view model...
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

	public Date getRefDate() {
		return refDate;
	}

	public void setRefDate(Date refDate) {
		this.refDate = refDate;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
    
}
