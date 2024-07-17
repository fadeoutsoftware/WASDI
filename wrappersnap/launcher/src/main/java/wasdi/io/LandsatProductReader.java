package wasdi.io;

import java.io.File;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;

import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * Product reader class for Landsat-5, Landsat-7 products and Landsat-8 L2 products
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
    		WasdiLog.warnLog("LandsatProductReader.readSnapProduct: TIFF folder with Landsat files not found. Will try to look for MTL file in the main product folder.");
    		oTIFFolder = m_oProductFile;
    		// return oSNAPProduct;
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
				
	        	WasdiLog.debugLog("LandsatProductReader.adjustFileAfterDownload: File is a Landsat product, start unzip");
	        	String sDownloadFolderPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
	        	ZipFileUtils oZipExtractor = new ZipFileUtils();
	        	oZipExtractor.unzip(sDownloadFolderPath + File.separator + sFileNameFromProvider, sDownloadFolderPath);
	        	deleteDownloadedZipFile(sDownloadedFileFullPath);
	        	
	        	
	        	String sLandsat5UnzippedFolderPath = sDownloadFolderPath + File.separator + sFileNameFromProvider.replace(".zip", "");
	        	File oLandsatUnzippedFolder = new File(sLandsat5UnzippedFolderPath);
	        	
	        	if (!oLandsatUnzippedFolder.exists() || oLandsatUnzippedFolder.isFile()) {
	        		WasdiLog.warnLog("LandsatProductReader.adjustFileAfterDownload: file does not exists or it is not a folder " + sLandsat5UnzippedFolderPath);
	        		return sFileName;
	        	}
	        	
	        	sFileName = oLandsatUnzippedFolder.getAbsolutePath();
	        	m_oProductFile = oLandsatUnzippedFolder;
	        	WasdiLog.debugLog("LandsatProductReader.adjustFileAfterDownload: unzipped Landsat folder path" + sFileName);        	
	        	
	        } else {
	        	WasdiLog.warnLog("LandsatProductReader.adjustFileAfterDownload: the product is not in zipped format");
	        }
 		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LandsatProductReader.adjustFileAfterDownload: error ", oEx);
		}
		
		return sFileName;
	}
	
	/**
	 * @param sFileNameFromProvider
	 * @param sDownloadPath
	 */
	private void deleteDownloadedZipFile(String sDownloadedFileFullPath) {
		try {
			File oZipFile = new File(sDownloadedFileFullPath);
			if(!oZipFile.delete()) {
				WasdiLog.errorLog("LandsatProductReader.deleteZipFile: cannot delete zip file");
			} else {
				WasdiLog.debugLog("LandsatProductReader.deleteZipFile: file zip successfully deleted");
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("LandsatProductReader.deleteZipFile: exception while trying to delete zip file: " + oE ); 
		}
	}
	
	
}
