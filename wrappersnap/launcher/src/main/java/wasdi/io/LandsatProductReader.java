package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

import org.json.JSONObject;

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
        
        if (m_oProductFile != null) {
        	oViewModel.setFileName(m_oProductFile.getName());
        	oViewModel.setName(m_oProductFile.getName());
        }

        WasdiLog.debugLog("LandsatProductReader.getProductViewModel: done");
		return oViewModel;
	}
	
	@Override
	public String getProductBoundingBox() {
				
		try {
			
			if (m_oProductFile.getName().startsWith("LC08_L2SP_")) {
				WasdiLog.debugLog("LandsatProductReader.getProductBoundingBox. The product is a Landsat-8 L2 product");
				
				File oMTLFile = null;
				
				if (m_oProductFile.isDirectory()) {
					for (File oFile : m_oProductFile.listFiles()) {
			    		if (oFile.getName().endsWith("_MTL.json")) {
			    			oMTLFile = oFile;
			    			break;
			    		}
			     	}
				}
				
				if (oMTLFile == null) {
					WasdiLog.debugLog("LandsatProductReader.getProductBoundingBox. Could not find MTL json file for Landsat-8 L2 product");
					return "";
				}
				
				WasdiLog.debugLog("LandsatProductReader.getProductBoundingBox. Found MTL json file for Landsat-8 L2 product: " + oMTLFile.getAbsolutePath());
				
				String sJsonFileContent = new String(Files.readAllBytes(Paths.get(oMTLFile.getAbsolutePath())));
								
				JSONObject oJsonObject = new JSONObject(sJsonFileContent);
				
				WasdiLog.debugLog("LandsatProductReader.getProductBoundingBox. Json content of the file has been read");
				
				JSONObject oMetadataAttributes = oJsonObject.optJSONObject("LANDSAT_METADATA_FILE");
				
				if (oMetadataAttributes == null) {
					WasdiLog.debugLog("LandsatProductReader.getProductBoundingBox. LANDSAT_METADATA_FILE entry not available. Impossible to read bounding box");
					return "";
				}
				
				JSONObject oProjectionAttributes = oMetadataAttributes.optJSONObject("PROJECTION_ATTRIBUTES");
				
				if (oProjectionAttributes == null) {
					WasdiLog.debugLog("LandsatProductReader.getProductBoundingBox. PROJECTION_ATTRIBUTES entry not available. Impossible to read bounding box");
					return "";
				}
				
				String sNorth = oProjectionAttributes.optString("CORNER_UL_LAT_PRODUCT");
				String sSouth = oProjectionAttributes.optString("CORNER_LR_LAT_PRODUCT");
				String sWest = oProjectionAttributes.optString("CORNER_LL_LON_PRODUCT");
				String sEast = oProjectionAttributes.optString("CORNER_UR_LON_PRODUCT");
				
				if (Utils.isNullOrEmpty(sNorth) || Utils.isNullOrEmpty(sSouth) || Utils.isNullOrEmpty(sWest) || Utils.isNullOrEmpty(sEast)) {
					WasdiLog.debugLog("LandsatProductReader.getProductBoundingBox. One of the coordinates is null or empty");
					return "";
				}
				
				return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", sSouth, sWest, sSouth, sEast, sNorth, sEast, sNorth, sWest, sSouth, sWest);
				
			}
			
			return super.getProductBoundingBox();
		
		} catch (IOException oEx) {
			WasdiLog.errorLog("LandsatProductReader.getProductBoundingBox. Exception when trying to read the bounding box ", oEx);
		}
		
		return ""; 
	}
	
	@Override
    protected Product readSnapProduct() {
		
		if (m_oProductFile.getName().startsWith("LC08_L2SP_")) {
        	WasdiLog.debugLog("LandsatProductReader.readSnapProduct: file is a Landsat-8 L2 product. Snap cannot read it");
        	return null;
		}
		
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
	
	@Override
    protected void getSnapProductBandsViewModel(ProductViewModel oProductViewModel, Product oProduct) {
    	
		if (oProductViewModel == null) {
            WasdiLog.debugLog("LandsatProductReader.getSnapProductBandsViewModel: ViewModel null, return");
            return;
        }
		
		if (m_oProductFile.getName().startsWith("LC08_L2SP_")) {
            WasdiLog.debugLog("LandsatProductReader.getSnapProductBandsViewModel: Landsat-8 L2 product. Skipping reading of the bands");
			NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
	    	oNodeGroupViewModel.setNodeName("Bands");
	    	List<BandViewModel> oBands = new ArrayList<>();
	    	oNodeGroupViewModel.setBands(oBands);
	    	oProductViewModel.setBandsGroups(oNodeGroupViewModel);
	    	return;
		}
		
		super.getSnapProductBandsViewModel(oProductViewModel, oProduct);
    }
	
	
	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		if (m_oProductFile.getName().startsWith("LC08_L2SP_")) {
            WasdiLog.debugLog("LandsatProductReader.getProductMetadataViewModel: Landsat-8 L2 product. Skipping reading of the metadata");
            return new MetadataViewModel("Metadata");
		}
		return super.getProductMetadataViewModel();
	}
	
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		if (m_oProductFile.getName().startsWith("LC08_L2SP_")) {
            WasdiLog.debugLog("LandsatProductReader.getFileForPublishBand: Landsat-8 L2 product. Skipping publishing of bands");
            return null;
		}
		return super.getFileForPublishBand(sBand, sLayerId);
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
	
	
	
	public static void main(String[]args) throws Exception {
		
		File oZipFile = new File("C:/Users/valentina.leone/Desktop/WORK/Landsat-8/LC08_L2SP_196028_20150704_20200909_02_T1");
		LandsatProductReader oReader = new LandsatProductReader(oZipFile);
//		String sAdjustedFile = oReader.adjustFileAfterDownload("C:/Users/valentina.leone/Desktop/WORK/Landsat-8/LC08_L2SP_196028_20150704_20200909_02_T1.zip", "LC08_L2SP_196028_20150704_20200909_02_T1.zip");
//		System.out.println("Adjusted file: " + sAdjustedFile);
		
		System.out.println(oReader.getProductBoundingBox());
		
		/*
		ProductViewModel oViewModel = oReader.getProductViewModel();
		System.out.println(oViewModel.getName());
		oViewModel.getBandsGroups().getBands().forEach(oBand -> System.out.println(oBand.getName()));
		
		System.out.println(oReader.getProductBoundingBox());
		*/
		
		/*
		Product oSNAPProduct = ProductIO.readProduct("C:/Users/valentina.leone/Desktop/WORK/Landsat-8/LC08_L2SP_196028_20150704_20200909_02_T1/LC08_L2SP_196028_20150704_20200909_02_T1_MTL.txt"); 
		for (Band oBand : oSNAPProduct.getBands()) {
			System.out.println("Name: " + oBand.getName());
		}
		*/
		
	}
}
