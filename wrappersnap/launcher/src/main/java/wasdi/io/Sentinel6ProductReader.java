package wasdi.io;

import java.util.Optional;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
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

		
		try {
			NetcdfFile oNetcdfFile = NetcdfFiles.open(m_oProductFile.getAbsolutePath());
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
			
			oNodeGroupVM.setBands(oBands);
        	oProductVM.setBandsGroups(oNodeGroupVM);
        	
			
		} catch (Exception oEx) {
			
		}
		
		return oProductVM;
		
	}
	

	
	@Override
	public String getProductBoundingBox() {
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
			
			// find the measurement file
			File oSentinelDirectory = new File(oUnzippedFolderPath.toString());
			
			if (sUnzippedFolderName.contains("P4_1B_LR_____") 
					&& Arrays.stream(oSentinelDirectory.listFiles()).anyMatch(oFile -> oFile.getName().equals("measurement.nc"))) {
				
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
			return null;	
			
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
		for (Band oBand : oSnapProd.getBands()) {
			System.out.println("Band: " + oBand.getName());
		}
		*/
		
		Sentinel6ProductReader oProductReader = new Sentinel6ProductReader(new File(sZipFilePath));
		System.out.println(oProductReader.adjustFileAfterDownload(sZipFilePath, "S6A_P4_1B_LR______20240430T063350_20240430T073003_20240501T074454_3373_128_009_004_EUM__OPE_ST_F09.SEN6"));
		oProductReader.getProductViewModel();
		
		
		
		
	}
	
	
	
	

}
