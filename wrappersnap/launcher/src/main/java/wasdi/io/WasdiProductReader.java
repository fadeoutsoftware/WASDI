package wasdi.io;

import java.io.File;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;

import wasdi.LauncherMain;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.ProductViewModel;

/**
 * Read Product utility class
 * 
 * This class was born when only SNAP products where available.
 * With the evolution of WASDI more data types has been added and the class changed.
 * 
 * Now it is an abstract class; different file types are supported. For each a sub-class is derived.
 * 
 * The main methods are:
 * 	.getSnapProduct: return a valid SNAP product if available, null otherwhise
 * 	.getProductViewModel: return a view model representing the product
 * 
 * Created by s.adamo on 18/05/2016.
 * Refactoring of 21/10/2019 (p.campanella):
 * Changed class name
 * Added support to different file types (starting from shape files)
 **/
public abstract class WasdiProductReader {
	
	/**
	 * Reference to the product file
	 */
	protected File m_oProductFile;
	
	/**
	 * Flag to know if we already made a try to read the product with SNAP
	 */
	protected boolean m_bSnapReadAlreadyDone = false; 
	
	/**
	 * Snap Product, if the file is supported by Snap
	 */
	protected Product m_oProduct;
	

	/**
	 * Constructor with a File that links the product to be opened
	 * 
	 * @param oProductFile File to be read
	 */
	public WasdiProductReader(File oProductFile) {
		m_oProductFile = oProductFile;		
	}

	/**
	 * Get the SNAP product or null if this is not a product readable by Snap
	 * 
	 * @return
	 */
	public Product getSnapProduct() {
		
		if (m_bSnapReadAlreadyDone == false) {
			m_oProduct = readSnapProduct();
		}
		
		return m_oProduct;
	}
	
	/**
	 * Get the product File Java Object
	 * @return
	 */
	public File getProductFile() {
		return m_oProductFile;
	}
	
    /**
     * Converts a product in a View Model
     * @param oExportProduct
     * @return
     */
	public abstract ProductViewModel getProductViewModel();
	
	
	/**
	 * Get Product Bounding Box 
	 * @return "%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", minY, minX, minY, maxX, maxY, maxX, maxY, minX, minY, minX
	 */
	public abstract String getProductBoundingBox();
	
	
	/**
	 * Get the metadata View Model of a Product
	 * @return
	 * @throws IOException
	 */
    public abstract MetadataViewModel getProductMetadataViewModel();	
	
	
    /**
     * Read a SNAP Product: this is supported directly in the main class
     * 
     * @param oFile File to open
     * @param sFormatName Format, if known.
     * @return Product object
     */
    protected Product readSnapProduct() {
    	
    	m_bSnapReadAlreadyDone = true;
    	
        Product oProduct = null;

        // P.Campanella 2019/04/16: deleted the static cache. There is a new instance of this class every time is used
        // so the cache was useless and could have memory problems
        
        if (m_oProductFile == null) {
        	LauncherMain.s_oLogger.debug("WasdiProductReader.readSnapProduct: file to read is null, return null ");
        	return null;
        }
        
        try {
            LauncherMain.s_oLogger.debug("WasdiProductReader.readSnapProduct: begin read " + m_oProductFile.getAbsolutePath());
            
            long lStartTime = System.currentTimeMillis();
            oProduct = ProductIO.readProduct(m_oProductFile);  
            long lEndTime = System.currentTimeMillis();
            
            LauncherMain.s_oLogger.debug("WasdiProductReader.readSnapProduct: read done in " + (lEndTime - lStartTime) + "ms");
            
            return oProduct;
            
        } catch (Throwable oEx) {
            oEx.printStackTrace();
            LauncherMain.s_oLogger.debug("WasdiProductReader.readSnapProduct: exception: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }

        return null;
    }
    
}
