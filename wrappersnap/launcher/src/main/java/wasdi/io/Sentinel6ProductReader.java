package wasdi.io;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;




public class Sentinel6ProductReader extends SnapProductReader {

	public Sentinel6ProductReader(File oProductFile) {
		super(oProductFile);
	}
	
	@Override
	public ProductViewModel getProductViewModel() {
		
		if (m_oProductFile == null) {
			WasdiLog.warnLog("Sentinel6ProductReader.getProductViewModel: pointer to product file is null");
			return null;
		}
		
		GeorefProductViewModel oProductVM = new GeorefProductViewModel();
		
		// set names
		String sFileName = m_oProductFile.getName();
		String sFileNameNoExtension = WasdiFileUtils.getFileNameWithoutLastExtension(sFileName); 
		oProductVM.setFileName(sFileName);
		oProductVM.setName(sFileNameNoExtension);
		oProductVM.setProductFriendlyName(sFileNameNoExtension);
		
		// prepare bands 
		NodeGroupViewModel oNodeGroupVM = new NodeGroupViewModel();
		oNodeGroupVM.setNodeName("Bands");
		
    	List<BandViewModel> oBands = new ArrayList<>();
    	
    	String sRelevantNetCdfFile = getRelevantNetCDFFile();
    	
    	if (!Utils.isNullOrEmpty(sRelevantNetCdfFile)) {
  		
			try {
				NetcdfFile oNetcdfFile = NetcdfFiles.open(sRelevantNetCdfFile);
				Group oRootGroup = oNetcdfFile.getRootGroup();
				List<Group> aoRootGroups = oRootGroup.getGroups();
				
				Group oData20Group = null;
				
				for (Group oGroup : aoRootGroups) {
					if (oGroup.getShortName().equals("data_20")) {
						oData20Group = oGroup;
						break;
					}
				}
				
				if (oData20Group != null) {
					List<Group> aoGroups = oData20Group.getGroups();
					
					for (Group oGroup : aoGroups) {
						BandViewModel oBandViewModel = new BandViewModel();
						oBandViewModel.setName(oGroup.getShortName());
						oBands.add(oBandViewModel);
					}
					
				} else {
					WasdiLog.warnLog("Sentinel6ProductReader.getProductViewModel: no 'data_20' group found");
				}
				
	
	        	
			} catch (Exception oEx) {
				WasdiLog.errorLog("Sentinel6ProductReader.getProductViewModel: error reading the bands", oEx);
			}
    	}
		
		oNodeGroupVM.setBands(oBands);
    	oProductVM.setBandsGroups(oNodeGroupVM);
    	
		return oProductVM;
	}
	
	private String getRelevantNetCDFFile() {
		// Sentinel-6 products are always directories
		String sNetCdfFilePath = "";
		
		if (!m_oProductFile.isDirectory()) {
			WasdiLog.warnLog("Sentinel6ProductReader.getBandsVMList: product should be a folder, but it is not");
			return sNetCdfFilePath;
		}
		
		String sProductFolderName = m_oProductFile.getName();
		String sProductFolderPath = m_oProductFile.getAbsolutePath();
				
		if (sProductFolderName.contains("P4_1B_LR_____")) {
			// data are in the file measurements.nc
			if (Arrays.stream(m_oProductFile.listFiles()).anyMatch(oFile -> oFile.getName().equals("measurement.nc"))) {
				sNetCdfFilePath = sProductFolderPath + File.separator + "measurement.nc";
			}
			
		} else if (sProductFolderName.contains("MW_2__AMR____")) {
			// data are in a ".nc" file, with the name starting in the same way
			
			for (File oFile : m_oProductFile.listFiles()) {
				String sFileName = oFile.getName();
				if ( (sFileName.startsWith("S6A_MW_2__AMR____") 
						|| sFileName.startsWith("S6B_MW_2__AMR____") 
						|| sFileName.startsWith("S6_MW_2__AMR____"))
						&& sFileName.endsWith(".nc")) {
					sNetCdfFilePath = oFile.getAbsolutePath();					
					break;
				}
			}
		} else if (sProductFolderName.contains("P4_2__LR_____")) {
			// the reference file is the one with Standard (STD) data
			
			for (File oFile : m_oProductFile.listFiles()) {
				String sFileName = oFile.getName();
				if ( (sFileName.contains("P4_2__LR_STD") 
						|| sFileName.contains("P4_2__LR_STD") 
						|| sFileName.contains("P4_2__LR_STD"))
						&& sFileName.endsWith(".nc")) {
					sNetCdfFilePath = oFile.getAbsolutePath();
					break;
				}	
			}
		}
		
		return sNetCdfFilePath;
	}
	
	@Override
	public String getProductBoundingBox() {
		String sResBBox = "";
		// we read the bounding box from the manifest.xml file. There, the tag <gml:posList> contains the coordinates of the polygon
		
		// first get the manifest file
		Pattern oManifestNamePattern = Pattern.compile("^xfd.*\\.xml$");
		Matcher oMatcher = null;
		File oManifestFile = null;

		
		try {
			if (!m_oProductFile.isDirectory()) {
				WasdiLog.warnLog("Sentinel6ProductReader.getProductBoundingBox: the pointer to the product file is not a folder " + m_oProductFile.getAbsolutePath());
			}
			
			File[] aoSAFEFolderContent = m_oProductFile.listFiles();
			
			
			for (File oFile : aoSAFEFolderContent) {
				oMatcher = oManifestNamePattern.matcher(oFile.getName());
				if (oMatcher.matches()) {
					oManifestFile = oFile;
					break;
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("Sentinel6ProductReader.getProductBoundingBox:  error retrieving the manifest file", oEx);
		}
		
		if (oManifestFile == null) {
			WasdiLog.warnLog("Sentinel6ProductReader.getProductBoundingBox: no manifest file found in the product folder "  + m_oProductFile.getAbsolutePath());
			return sResBBox;
		}
		
        double dMinY = Double.MAX_VALUE;  	// min latitude
        double dMaxY = Double.MIN_VALUE;	// max latitude
        double dMinX = Double.MAX_VALUE;	// min longitude
        double dMaxX = Double.MIN_VALUE; // max longitude
        
		try {
			// Parse the XML file
			DocumentBuilderFactory oFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder oBuilder = oFactory.newDocumentBuilder();
	        Document oDocument = oBuilder.parse(oManifestFile);
	        Element oRoot = oDocument.getDocumentElement();
	        
	        // Find all elements with the tag <gml:posList>, describing the polygons of the product
	        NodeList oNodeList = oRoot.getElementsByTagName("gml:posList");
	        
	        // If there are no such elements, return null
	        if (oNodeList.getLength() == 0) {
	            WasdiLog.warnLog("Sentinel6ProductReader.getProductBoundingBox: no element 'gml:posList' in xml file " + oManifestFile.getAbsolutePath());
	            return sResBBox;
	        }
   
	        int iItem = 0;
	        while (iItem < oNodeList.getLength()) {
	        	String sCoordinates = oNodeList.item(iItem).getTextContent();
	        	String[] asCoordinates = sCoordinates.split(" ");
	        	List<Double> asLatitude = new ArrayList<>();
	        	List<Double> asLongitude = new ArrayList<>();
	        	
	        	int i = 0;
	        	while (i < asCoordinates.length) {
	        		if (i % 2 == 0) 
	        			asLatitude.add(Double.parseDouble(asCoordinates[i]));
		        	else 
		        		asLongitude.add(Double.parseDouble(asCoordinates[i]));
		        	i ++;
	        	}
	        	
	        	Double dMaxLat = Collections.max(asLatitude);
		        Double dMinLat = Collections.min(asLatitude);
		        Double dMaxLong = Collections.max(asLongitude);
		        Double dMinLong = Collections.min(asLongitude);
		        
		        if (dMaxLat > dMaxY) dMaxY = dMaxLat;
		        if (dMinLat < dMinY) dMinY = dMinLat;
		        if (dMaxLong > dMaxX) dMaxX = dMaxLong;
		        if (dMinLong < dMinX) dMinX = dMinLong;   
		        
		        iItem ++;
	        }
		} catch (Exception oEx) {
			WasdiLog.errorLog("Sentinel6ProductReader.getProductBoundingBox:  error reading manifest xml file", oEx);
		}
	        
        if (dMinY >= - 90 && dMinY <= 90
        		&& dMaxY >= -90 && dMaxY <= 90
        		&& dMinX >= -180 && dMinX <= 180
        		&& dMaxX >= -180 && dMaxX <= 180) {
        	sResBBox = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", 
							(float) dMinY, (float) dMinX, (float) dMinY, (float) dMaxX, (float) dMaxY, (float) dMaxX, (float) dMaxY, (float) dMinX, (float) dMinY, (float) dMinX);
        } else {
        	WasdiLog.warnLog(String.format("Sentinel6ProductReader.getProductBoundingBox: some coordinates out of expected range lat (%f, %f) long (%f, %f)", dMinY, dMaxY, dMinX, dMaxX));
        }
        
        return sResBBox;
	        
	}
	
	@Override
    public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		
		if (Utils.isNullOrEmpty(sDownloadedFileFullPath)) {
			WasdiLog.warnLog("Sentinel6ProductReader.adjustFileAfterDownload: downloaded full path is null or empty");
		}
		
		if (Utils.isNullOrEmpty(sFileNameFromProvider)) {
			WasdiLog.warnLog("Sentinel6ProductReader.adjustFileAfterDownload: file name from data provider is null or empty");
		}
		
		if(!sFileNameFromProvider.toUpperCase().startsWith("S6") || !sDownloadedFileFullPath.toLowerCase().endsWith(".zip")) {
			WasdiLog.warnLog("Sentinel6ProductReader.adjustFileAfterDownload: " + sFileNameFromProvider + " does not look like a Sentinel-6 file name");
			return null;
		}
		
		WasdiLog.debugLog("Sentinel6ProductReader.adjustFileAfterDownload: downloaded file full path: " + sDownloadedFileFullPath);
		WasdiLog.debugLog("Sentinel6ProductReader.adjustFileAfterDownload: file name from provider: " + sDownloadedFileFullPath);
		
		
		try {
			// get the parent folder of the path
			File oDownloadedFile = new File(sDownloadedFileFullPath);
			String sParentFolderPath = oDownloadedFile.getParentFile().getAbsolutePath();
			WasdiLog.debugLog("Sentinel6ProductReader.adjustFileAfterDownload: parent folder " + sParentFolderPath);
			
			
			ZipFileUtils oZipUtils = new ZipFileUtils();
			oZipUtils.unzip(sDownloadedFileFullPath, sParentFolderPath);
			deleteDownloadedZipFile(sDownloadedFileFullPath);
			
			// if the file has been unzipped, this should be the name of the created folder
			String sUnzippedFolderName = oDownloadedFile.getName().replace(".zip", "");
			Path oUnzippedFolderPath = Paths.get(sParentFolderPath + File.separator + sUnzippedFolderName);
			
			if (!Files.exists(oUnzippedFolderPath) || !Files.isDirectory(oUnzippedFolderPath)) {
				WasdiLog.warnLog(": unzipped folder " + sUnzippedFolderName + " not found in path " + sParentFolderPath);
				return null;
			}
			
			File oSentinelDirectory = new File(oUnzippedFolderPath.toString());
			
			m_oProductFile = oSentinelDirectory;
			
			return oSentinelDirectory.getAbsolutePath();
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload: error unzipping Sentinel-6 product", oEx);
		}
		
		return null;
	}
	
	/**
	 * @param sFileNameFromProvider
	 * @param sDownloadPath
	 */
	private void deleteDownloadedZipFile(String sDownloadedFileFullPath) {
		// TODO: this can be moved to the zip utils
		try {
			File oZipFile = new File(sDownloadedFileFullPath);
			if(!oZipFile.delete()) {
				WasdiLog.errorLog("Sentinel6ProductReader.deleteZipFile: cannot delete zip file");
			} else {
				WasdiLog.debugLog("Sentinel6ProductReader.deleteZipFile: file zip successfully deleted");
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Sentinel6ProductReader.deleteZipFile: exception while trying to delete zip file: " + oE ); 
		}
	}
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		return null;
	}
	
	
	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return null;
	}
	
	public static void main(String[]args) throws Exception {
		String sPath = "C:/Users/valentina.leone/Desktop/WORK/SENTINEL-6/test_code/S6A_MW_2__AMR_____20240503T054610_20240503T064222_20240504T073349_3373_128_085_042_EUM__OPE_ST_F09.SEN6.zip";
		File oSentinel = new File(sPath);
		Sentinel6ProductReader oReader = new Sentinel6ProductReader(oSentinel);
		oReader.adjustFileAfterDownload(sPath, "S6A_MW_2__AMR_____20240503T054610_20240503T064222_20240504T073349_3373_128_085_042_EUM__OPE_ST_F09.SEN6");
		System.out.println(oReader.getProductBoundingBox());
	}
}
