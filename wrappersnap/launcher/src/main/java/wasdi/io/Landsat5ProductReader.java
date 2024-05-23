package wasdi.io;

import java.io.File;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;

import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class Landsat5ProductReader extends SnapProductReader {

	public Landsat5ProductReader(File oProductFile) {
		super(oProductFile);	
	}
	
	@Override
	public ProductViewModel getProductViewModel() {
		WasdiLog.debugLog("Landsat5ProductReader.getProductViewModel. Product file path " + m_oProductFile.getAbsolutePath());
		
		ProductViewModel oViewModel = new ProductViewModel();

        // Get Bands
        this.getSnapProductBandsViewModel(oViewModel, getSnapProduct());
        
        if (m_oProductFile!=null) {
        	oViewModel.setFileName(m_oProductFile.getName());
        	oViewModel.setName(m_oProductFile.getName());
        }

        WasdiLog.debugLog("Landsat5ProductReader.getProductViewModel: done");
		return oViewModel;
	}
	
	@Override
    protected Product readSnapProduct() {
		m_bSnapReadAlreadyDone = true;
    	
        Product oSNAPProduct = null;
        
        if (m_oProductFile == null) {
        	WasdiLog.debugLog("Landsat5ProductReader.readSnapProduct: file to read is null, return null ");
        	return oSNAPProduct;
        }
        
        if (!m_oProductFile.isDirectory()) {
        	WasdiLog.debugLog("Landsat5ProductReader.readSnapProduct: the referenced product is a file, but it should be a folder");
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
    		WasdiLog.warnLog("Landsat5ProductReader.readSnapProduct: TIFF folder with Landsat-5 files not found");
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
    		WasdiLog.warnLog("Landsat5ProductReader.readSnapProduct: no MTL file that can be read by SNAP");
    		return oSNAPProduct;
    	}
    	        
        try {
            WasdiLog.debugLog(".readSnapProduct: begin read " + oMTLFile.getAbsolutePath());
            
            long lStartTime = System.currentTimeMillis();
            oSNAPProduct = ProductIO.readProduct(oMTLFile);  
            long lEndTime = System.currentTimeMillis();
            
            WasdiLog.debugLog("Landsat5ProductReader.readSnapProduct: read done in " + (lEndTime - lStartTime) + "ms");

            if(oSNAPProduct == null) {
            	WasdiLog.errorLog("Landsat5ProductReader.readSnapProduct: SNAP could not read the MTL file, the returned product is null");
            }
            
            return oSNAPProduct;
            
        } 
        catch (Throwable oEx) {
            WasdiLog.errorLog("Landsat5ProductReader.readSnapProduct: exception " + oEx.toString());
        }        
        
		return oSNAPProduct;
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		String sFileName = sDownloadedFileFullPath;
		
		WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: downloaded file path " + sDownloadedFileFullPath + ", file name from provider " + sFileNameFromProvider);
		
		try {
			if(sFileNameFromProvider.endsWith(".zip")) {
				
	        	WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: File is a Landsat-5 product, start unzip");
	        	String sDownloadFolderPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
	        	ZipFileUtils oZipExtractor = new ZipFileUtils();
	        	oZipExtractor.unzip(sDownloadFolderPath + File.separator + sFileNameFromProvider, sDownloadFolderPath);
	        	
	        	String sLandsat5UnzippedFolderPath = sDownloadFolderPath + File.separator + sFileNameFromProvider.replace(".zip", "");
	        	File oLandsat5UnzippedFolder = new File(sLandsat5UnzippedFolderPath);
	        	
	        	if (!oLandsat5UnzippedFolder.exists() || oLandsat5UnzippedFolder.isFile()) {
	        		WasdiLog.warnLog("Landsat5ProductReader.adjustFileAfterDownload: file does not exists or it is not a folder " + sLandsat5UnzippedFolderPath);
	        		return sFileName;
	        	}
	        	
	        	sFileName = oLandsat5UnzippedFolder.getAbsolutePath();
	        	m_oProductFile = oLandsat5UnzippedFolder;
	        	WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: unzipped Landsat-5 folder path" + sFileName);        	
	        	
	        } else {
	        	WasdiLog.warnLog("Landsat5ProductReader.adjustFileAfterDownload: the product is not in zipped format");
	        }
 		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Landsat5ProductReader.adjustFileAfterDownload: error ", oEx);
		}
		
		return sFileName;
	}
	
	
	public static void main(String [] args) throws Exception {
		String sFilePath = "C:/Users/valentina.leone/Desktop/WORK/Landsat-5/test_code/LS05_RMTI_TM__GTC_1P_20050511T093014_20050511T093042_112727_0190_0021_DC6B.zip";
		Landsat5ProductReader oPR = new Landsat5ProductReader(new File(sFilePath));
		oPR.adjustFileAfterDownload(sFilePath, "LS05_RMTI_TM__GTC_1P_20050511T093014_20050511T093042_112727_0190_0021_DC6B.zip");
		/* File oFile = oPR.getFileForPublishBand("radiance_6", "1111-1111-1111");
		if (oFile == null)
			System.out.println("ERROR: file is null");
		else {
			System.out.println("File path: " + oFile.getAbsolutePath());
		}*/
		System.out.println(oPR.getProductBoundingBox());
	}
	
}
