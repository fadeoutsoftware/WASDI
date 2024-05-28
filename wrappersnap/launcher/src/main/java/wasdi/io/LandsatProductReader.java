package wasdi.io;

import java.io.File;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;

import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * Product reader class for Landsat-5 and Landsat-7 products
 * @author valentina.leone
 *
 */
public class LandsatProductReader extends SnapProductReader {

	public LandsatProductReader(File oProductFile) {
		super(oProductFile);	
	}
	
	@Override
	public ProductViewModel getProductViewModel() {
		WasdiLog.debugLog("LandsatProductReader.getProductViewModel. Product file path " + m_oProductFile.getAbsolutePath());
		
		ProductViewModel oViewModel = new ProductViewModel();

        // Get Bands
        this.getSnapProductBandsViewModel(oViewModel, getSnapProduct());
        
        if (m_oProductFile!=null) {
        	oViewModel.setFileName(m_oProductFile.getName());
        	oViewModel.setName(m_oProductFile.getName());
        }

        WasdiLog.debugLog("LandsatProductReader.getProductViewModel: done");
		return oViewModel;
	}
	
	@Override
    protected Product readSnapProduct() {
		m_bSnapReadAlreadyDone = true;
    	
        Product oSNAPProduct = null;
        
        if (m_oProductFile == null) {
        	WasdiLog.debugLog("LandsatProductReader.readSnapProduct: file to read is null, return null ");
        	return oSNAPProduct;
        }
        
        if (!m_oProductFile.isDirectory()) {
        	WasdiLog.debugLog("LandsatProductReader.readSnapProduct: the referenced product is a file, but it should be a folder");
        	return oSNAPProduct;
        }
        
        // look for the ".TIFF" folder
    	File oTIFFolder = null;
    	for (File oFile : m_oProductFile.listFiles()) {
    		if (oFile.isDirectory() && oFile.getName().endsWith(".TIFF")) {
    			oTIFFolder = oFile;
    			break;
    		}
    	}
    	
    	if (oTIFFolder == null) {
    		WasdiLog.warnLog("LandsatProductReader.readSnapProduct: TIFF folder with Landsat-5 files not found");
    		return oSNAPProduct;
    	}
    	
    	// if we found the TIF folder, then we can access the "MTL" file
    	File oMTLFile = null;
    	for (File oFile : oTIFFolder.listFiles()) {
    		if (oFile.getName().endsWith("_MTL.txt")) {
    			oMTLFile = oFile;
    			break;
    		}
     	}
    	
    	if (oMTLFile == null) {
    		WasdiLog.warnLog("LandsatProductReader.readSnapProduct: no MTL file that can be read by SNAP");
    		return oSNAPProduct;
    	}
    	        
        try {
            WasdiLog.debugLog(".readSnapProduct: begin read " + oMTLFile.getAbsolutePath());
            
            long lStartTime = System.currentTimeMillis();
            oSNAPProduct = ProductIO.readProduct(oMTLFile);  
            long lEndTime = System.currentTimeMillis();
            
            WasdiLog.debugLog("LandsatProductReader.readSnapProduct: read done in " + (lEndTime - lStartTime) + "ms");

            if(oSNAPProduct == null) {
            	WasdiLog.errorLog("LandsatProductReader.readSnapProduct: SNAP could not read the MTL file, the returned product is null");
            }
            
            return oSNAPProduct;
            
        } 
        catch (Throwable oEx) {
            WasdiLog.errorLog("LandsatProductReader.readSnapProduct: exception " + oEx.toString());
        }        
        
		return oSNAPProduct;
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		String sFileName = sDownloadedFileFullPath;
		
		WasdiLog.debugLog("LandsatProductReader.adjustFileAfterDownload: downloaded file path " + sDownloadedFileFullPath + ", file name from provider " + sFileNameFromProvider);
		
		try {
			if(sFileNameFromProvider.endsWith(".zip")) {
				
	        	WasdiLog.debugLog("LandsatProductReader.adjustFileAfterDownload: File is a Landsat-5 product, start unzip");
	        	String sDownloadFolderPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
	        	ZipFileUtils oZipExtractor = new ZipFileUtils();
	        	oZipExtractor.unzip(sDownloadFolderPath + File.separator + sFileNameFromProvider, sDownloadFolderPath);
	        	
	        	String sLandsat5UnzippedFolderPath = sDownloadFolderPath + File.separator + sFileNameFromProvider.replace(".zip", "");
	        	File oLandsat5UnzippedFolder = new File(sLandsat5UnzippedFolderPath);
	        	
	        	if (!oLandsat5UnzippedFolder.exists() || oLandsat5UnzippedFolder.isFile()) {
	        		WasdiLog.warnLog("LandsatProductReader.adjustFileAfterDownload: file does not exists or it is not a folder " + sLandsat5UnzippedFolderPath);
	        		return sFileName;
	        	}
	        	
	        	sFileName = oLandsat5UnzippedFolder.getAbsolutePath();
	        	m_oProductFile = oLandsat5UnzippedFolder;
	        	WasdiLog.debugLog("LandsatProductReader.adjustFileAfterDownload: unzipped Landsat-5 folder path" + sFileName);        	
	        	
	        } else {
	        	WasdiLog.warnLog("LandsatProductReader.adjustFileAfterDownload: the product is not in zipped format");
	        }
 		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LandsatProductReader.adjustFileAfterDownload: error ", oEx);
		}
		
		return sFileName;
	}
	
}
