package wasdi.io;

import java.util.Optional;
import java.util.function.Predicate;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

import ucar.ma2.Array;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.BandViewModel;




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
				
				Optional<Group> oMaybeData20Group = oRootGroup.getGroups().stream().filter(oGroup -> oGroup.getShortName().equals("data_20")).findFirst();
				if (!oMaybeData20Group.isEmpty()) {
					Group oData20Group = oMaybeData20Group.get();
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
		String sRelevantNetCdfFile = "C:/Users/valentina.leone/Desktop/WORK/SENTINEL-6/S6A_P4_2__LR______20240501T152036_20240501T165910_20240501T171410_5914_128_043_022_EUM__OPE_NR_F09.SEN6/S6A_P4_2__LR_STD__NR_128_043_20240501T152036_20240501T165910_F09.nc"; // getRelevantNetCDFFile();
		
		if (!Utils.isNullOrEmpty(sRelevantNetCdfFile)) {
	  		
			try {
				NetcdfFile oNetcdfFile = NetcdfFiles.open(sRelevantNetCdfFile);
				Group oRootGroup = oNetcdfFile.getRootGroup();
				
				Optional<Group> oMaybeData20Group = oRootGroup.getGroups().stream().filter(oGroup -> oGroup.getShortName().equals("data_20")).findFirst();
				if (!oMaybeData20Group.isEmpty()) {
					Group oData20Group = oMaybeData20Group.get();
					List<Group> aoGroups = oData20Group.getGroups();
					
					for (Group oGroup : aoGroups) {			// here we are at group level (c, ku)
						System.out.println(oGroup.getName().toUpperCase());
						Variable oLatitude = oGroup.findVariable("latitude");
						Variable oLongitude = oGroup.findVariable("longitude");
						
						if (oLatitude != null && oLongitude != null) {
							Array oArrayLatitude = oLatitude.read();
							Array oArrayLongitude = oLongitude.read();
							
							if (oArrayLatitude != null && oArrayLongitude != null) {
								
								Object oStorageLatitude = oArrayLatitude.getStorage();
								Object oStorageLongitude = oArrayLongitude.getStorage();
								
								if (oStorageLatitude != null && oStorageLongitude != null
										&& oStorageLatitude instanceof int[] && oStorageLongitude instanceof int[]) {
									List<Integer> aiLatitudeValues = Arrays.asList(Arrays.stream((int[]) oStorageLatitude)
                                            .boxed()
                                            .toArray(Integer[]::new));
									List<Integer> aiLongitudeValues = Arrays.asList(Arrays.stream((int[]) oStorageLongitude)
                                            .boxed()
                                            .toArray(Integer[]::new));
									Integer iMaxLatitude = Collections.max(aiLatitudeValues);
									Integer iMinLatitude = Collections.min(aiLatitudeValues);;
									Integer iMaxLongitude = Collections.max(aiLongitudeValues);
									Integer iMinLongitude = Collections.min(aiLongitudeValues);
									System.out.println("Lat (" + iMinLatitude + "," + iMaxLatitude + ") Long (" + iMinLongitude + ", " + iMaxLongitude + ")");
								}
							}
							
						}
						
					}
					
				} else {
					WasdiLog.warnLog("Sentinel6ProductReader.getProductViewModel: no 'data_20' group found");
				}
				
	
	        	
			} catch (Exception oEx) {
				WasdiLog.errorLog("Sentinel6ProductReader.getProductViewModel: error reading the bands", oEx);
			}
    	}
		
		return "";
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
			
			/*
			if (sUnzippedFolderName.contains("P4_1B_LR_____") 
					&& Arrays.stream(oSentinelDirectory.listFiles()).anyMatch(oFile -> oFile.getName().equals("measurement.nc"))) {
				// level 1 products have a single 'measurement.nc' file

				
				// we need to rename the file and move it in the parent folder
				String sNewMeasurementFileName = sUnzippedFolderName.replace(".SEN6", "") + ".nc";
				String sNewDestinationPath = sParentFolderPath + File.separator + sNewMeasurementFileName;
				Path oSourcePath = Paths.get(oUnzippedFolderPath.toString() + File.separator +  "measurement.nc");
				Path oDestinationPath = Paths.get(sNewDestinationPath);
				Files.move(oSourcePath, oDestinationPath);
				
				File oMovedFile = oDestinationPath.toFile();
				if (oMovedFile.exists()) {
					
					m_oProductFile = oMovedFile;
					
					// we remove the folder and all its contents
					Arrays.asList(oSentinelDirectory.listFiles()).forEach(File::delete);
					
					if (oSentinelDirectory.delete()) 
						WasdiLog.debugLog("Sentinel6ProductReader.adjustFileAfterDownload: folder deleted " + oUnzippedFolderPath.toString());
					else 
						WasdiLog.warnLog("Sentinel6ProductReader.adjustFileAfterDownload: folder not deleted " + oUnzippedFolderPath.toString() );
					
					return oMovedFile.getAbsolutePath();
					
				} else {
					WasdiLog.warnLog("Sentinel6ProductReader.adjustFileAfterDownload: file not moved in the expected location " + oDestinationPath.toString());
					return null;
				}
				
			} 
			else if (sUnzippedFolderName.contains("")) 
			return null;	
			*/
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
	
	
	public static void main(String[]args) throws Exception {
		
		// TEST PER UNZIP OK - verificato che una cartella zippata viene estratta in una cartella con lo stesso nome
		// come secondo parametro posso passare 
		//ZipFileUtils oZipUtils = new ZipFileUtils();
		String sZipFilePath = "C:/Users/valentina.leone/Desktop/WORK/SENTINEL-6/test_code/S6A_P4_1B_LR______20240430T063350_20240430T073003_20240501T074454_3373_128_009_004_EUM__OPE_ST_F09.SEN6.zip";
		String sDestinationPath = "C:/Users/valentina.leone/Downloads";
		//System.out.println(oZipUtils.unzip(sZipFilePath, sDestinationPath));
		
		// posso passare a SNAP l'intera cartella?No, c'e' bisogno del measurement
		String sFolderPath = "C:/Users/valentina.leone/Desktop/WORK/SENTINEL-6/test_code/S6A_P4_1B_LR/S6A_P4_1B_LR______20240430T063350_20240430T073003_20240501T074454_3373_128_009_004_EUM__OPE_ST_F09.SEN6.zip";
//		Path oPath = Paths.get(sFolderPath);
//		System.out.println(oPath.toString());
		
		/*
		File oSentinelFile = new File(sFolderPath);
		Product oSnapProd = ProductIO.readProduct(oSentinelFile);
		for (Band oBand : oSnapProd.getBands()) {s
			System.out.println("Band: " + oBand.getName());
		}
		*/
		
		Sentinel6ProductReader oProductReader = new Sentinel6ProductReader(new File(sZipFilePath));
		/*
		System.out.println(oProductReader.adjustFileAfterDownload(sZipFilePath, "S6A_P4_1B_LR______20240430T063350_20240430T073003_20240501T074454_3373_128_009_004_EUM__OPE_ST_F09.SEN6"));
		ProductViewModel oVM = oProductReader.getProductViewModel();
		List<BandViewModel> aoBands = oVM.getBandsGroups().getBands();
		aoBands.forEach(oB -> System.out.println(oB.getName()));
		*/
		oProductReader.getProductBoundingBox();
		
	}
	
	
	
	

}
