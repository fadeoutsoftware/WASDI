package wasdi.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.esa.snap.core.datamodel.Product;

import com.google.common.io.Files;

import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class Sentinel6ProductReader extends CmNcProductReader {

	/**
	 * @param oProductFile the Seninel 6 (zip) file to be read
	 */
	public Sentinel6ProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		
		try {
			if (Utils.isNullOrEmpty(sDownloadedFileFullPath)) {
				WasdiLog.errorLog(
						"Sentinel6ProductReader.adjustFileAfterDownload. sDownloadedFileFullPath null or empty, aborting");
				return null;
			}
			if (Utils.isNullOrEmpty(sFileNameFromProvider)) {
				WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload. sFileNameFromProvider null or empty, aborting");
				return null;
			}
			if (!sFileNameFromProvider.toUpperCase().startsWith("S6")
					|| !sFileNameFromProvider.toLowerCase().endsWith(".zip")) {
				WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload: " + sFileNameFromProvider + " does not look like a Sentinel-6 file name");
				return null;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload. arguments checking failed due to: " + oE.getMessage() + ", aborting");
			return null;
		}
		

		try {
			// path and object of the workspace directory
			String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
			File oWorkspaceDir = new File(sDownloadPath);
			
			// name, path and object of the unzipped folder
			String sNewFolderName = sFileNameFromProvider.substring(0, sFileNameFromProvider.toLowerCase().lastIndexOf(".zip"));
			String sNewFolderPath = sDownloadPath + File.separator + sNewFolderName;
			File oTargetDirectory = new File(sNewFolderPath);
			
			WasdiLog.debugLog("Sentinel6ProductReader.adjustFileAfterDownload. File is a Sentinel-6 image, start unzip of file: " + sDownloadedFileFullPath);
			File oSourceFile = new File(sDownloadedFileFullPath);
			ZipFileUtils.cleanUnzipFile(oSourceFile, oTargetDirectory);
			
			
			boolean bIsUnzippedDirPresent = Arrays.asList(oWorkspaceDir.listFiles()).stream().anyMatch(oFile -> oFile.isDirectory() && oFile.getName().equals(sNewFolderName));
			
			if (!bIsUnzippedDirPresent) {
				WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload: the unzipped folder is not present");
				return null;
			}
				
			File oNcFile = null;
			for (File oFile : oTargetDirectory.listFiles()) {
				if (oFile.isFile() && oFile.getName().toLowerCase().endsWith(".nc")) {
					oNcFile = oFile;
					break;		// TODO: so far we get the fist NetCDF file found in the folder. Later, we will need to understand how to select the correct one
				}
			}
			
			if (oNcFile == null) {
				WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload. No NetCDF file found in the unzipped folder");
				return null;
			}
				
			WasdiLog.debugLog("Sentinel6ProductReader.adjustFileAfterDownload. Found NetCDF file: " + oNcFile.getAbsolutePath() );
			File oNcTargetFile = new File(sDownloadPath + File.separator + oNcFile.getName());
			Files.move(oNcFile, oNcTargetFile);
			final String sNcFileName = oNcFile.getName();
			boolean bIsFileMoved = Arrays.asList(oWorkspaceDir.listFiles()).stream().anyMatch(oFile -> oFile.isFile() && oFile.getName().equals(sNcFileName));
			if (bIsFileMoved) {
				WasdiLog.debugLog("Sentinel6ProductReader.adjustFileAfterDownload. File correctly moved in workspace folder: " + oNcTargetFile.getName());
				m_oProductFile = oNcTargetFile;
				return oNcTargetFile.getAbsolutePath();
			} else  {
				WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload. File not moved to the workspace folder.");
				return null;
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload. error ", oEx);
		}

		return null;
	}
	
	@Override
	public ProductViewModel getProductViewModel() {

		if (m_oProductFile == null) return null;

    	// Create the return value
    	GeorefProductViewModel oRetViewModel = null;

		try {
			NetcdfFile oFile = NetcdfFiles.open(m_oProductFile.getAbsolutePath());

	    	// Create the Product View Model
	    	oRetViewModel = new GeorefProductViewModel();

        	// Set name values
        	oRetViewModel.setFileName(m_oProductFile.getName());
        	oRetViewModel.setName(Utils.getFileNameWithoutLastExtension(m_oProductFile.getName()));
        	oRetViewModel.setProductFriendlyName(oRetViewModel.getName());

	    	NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
        	oNodeGroupViewModel.setNodeName("Bands");

    		Group oRootGroup = oFile.getRootGroup();
    		List<Group> aoRootGroupGroups = oRootGroup.getGroups();

        	List<BandViewModel> aoBands = new ArrayList<>();

    		for (Group oGroup : aoRootGroupGroups) {

    			List<Variable> aoVariableList = oGroup.getVariables();
    			for (Variable v : aoVariableList) {
    				String variableShortName = v.getShortName();
    			        	
		        	// Create the single band representing the shape
		        	BandViewModel oBandViewModel = new BandViewModel();
		        	oBandViewModel.setPublished(false);
		        	oBandViewModel.setGeoserverBoundingBox("");
		        	oBandViewModel.setPublished(false);
		        	oBandViewModel.setName(variableShortName);
			        aoBands.add(oBandViewModel);
    			}
    		}	

        	oNodeGroupViewModel.setBands(aoBands);
	    	oRetViewModel.setBandsGroups(oNodeGroupViewModel);
		} catch (Exception e) {
    		WasdiLog.debugLog("Sentinel5ProductReader.getProductViewModel: exception reading the shape file: " + e.toString());
		}
		
    	return oRetViewModel;
	}

	


	@Override
	public Product getSnapProduct() {
    	WasdiLog.debugLog("Sentinel6ProductReader.readSnapProduct. We do not want SNAP to read S6, return null ");
    	return null;   
	
	}
	
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		try {
			
			String sInputFile = m_oProductFile.getAbsolutePath();
			String sOutputFile = sLayerId + ".tif";		
			WasdiLog.debugLog("Sentinel6ProductReader.getFileForPublishBand. Product NetCDFFile path: " + sInputFile);
			WasdiLog.debugLog("Sentinel6ProductReader.getFileForPublishBand. Expected output TIF file path: " + sOutputFile);
			
			File oFile = new File(sInputFile);
			String sInputPath = oFile.getParentFile().getPath();
			if (!sInputPath.endsWith("/")) 
				sInputPath += "/";
			
			List<String> asGdalTranslateArgs = buildArgumentsGdalTranslate(sInputPath, sInputFile, sBand);

			// Execute the process
			ProcessBuilder oProcessBuidler = new ProcessBuilder(asGdalTranslateArgs.toArray(new String[0]));
			Process oProcess;
			
			oProcessBuidler.redirectErrorStream(true);
			oProcess = oProcessBuidler.start();
			
			BufferedReader oReader = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
			String sLine;
			while ((sLine = oReader.readLine()) != null)
				WasdiLog.debugLog("Sentinel6ProductReader.getFileForPublishBand. [gdal]: " + sLine);
			
			oProcess.waitFor();		
			WasdiLog.debugLog("Sentinel6ProductReader.getFileForPublishBand. gdal_translate command execution ended");

			
			List<String> asGdalWaepArgs = buildArgumentsGdalWarp(sInputPath, sOutputFile, sBand); 
			
			oProcessBuidler = new ProcessBuilder(asGdalWaepArgs.toArray(new String[0]));
			oProcessBuidler.redirectErrorStream(true);
			oProcess = oProcessBuidler.start();
			
			oReader = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
			while ((sLine = oReader.readLine()) != null)
				WasdiLog.debugLog("Sentinel6ProductReader.getFileForPublishBand. [gdal]: " + sLine);
			
			oProcess.waitFor();
			WasdiLog.debugLog("Sentinel6ProductReader.getFileForPublishBand. gdalwarp command execution ended");

			return new File(sInputPath + sOutputFile);
		}
		catch (Exception oEx) {
			
			WasdiLog.debugLog("Sentinel6ProductReader.getFileForPublishBand.: Exception = " + oEx.toString());
			
			return null;
		}
	}
	
	/**
	 * Populate the list of arguments to create a VRT file starting from a NetCDF file with GDAL
	 * @param sParentFolderPath the path of the folder where the VRT file will be created
	 * @param sInputFilePath the path of the NetCDF file
	 * @param sBandName the name of the band
	 * @return the list of arguments to execute the gdal_translate command
	 */
	private static List<String> buildArgumentsGdalTranslate(String sParentFolderPath, String sInputFilePath, String sBandName) {
		String sGdalCommand = "gdal_translate";
		sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
		
		List<String> asArgs = new ArrayList<>();
		
		asArgs.add(sGdalCommand);
		
		asArgs.add("-co");
		asArgs.add("WRITE_BOTTOMUP=NO");
		
		asArgs.add("-of");
		asArgs.add("VRT");

		String sGdalInput = "\"NETCDF:" + sInputFilePath + ":data_01/" + sBandName + "\"";  // e.g. "NETCDF:path_to_file/file_name.nc:/data_01/band_name"
		asArgs.add(sGdalInput);
		
		String sGdalOutput = sParentFolderPath + sBandName + ".vrt";
		asArgs.add(sGdalOutput);
		
		WasdiLog.debugLog("Sentinel6ProductReader.buildArgumentsGdalTranslate. gdal_translate input: " + sGdalInput);
		WasdiLog.debugLog("Sentinel6ProductReader.buildArgumentsGdalTranslate. gdal_translate output: " + sGdalOutput);
		
		return asArgs;
	}
	
	/**
	 * Populate the list of arguments to create a TIF file starting from a VRT file with GDAL
	 * @param sParentFolderPath the path of the folder containing the VRT file
	 * @param sOutputFileName the name of the output TIF file
	 * @param sBandName the name of the band
	 * @return the list of arguments to execute the gdalwarp command
	 */
	private static List<String> buildArgumentsGdalWarp(String sParentFolderPath, String sOutputFileName, String sBandName) {
		String sGdalCommand = "gdalwarp";
		sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
		
		List<String> asArgs = new ArrayList<String>();
		
		asArgs.add(sGdalCommand);
		
		asArgs.add("-geoloc");
		
		asArgs.add("-t_srs");
		asArgs.add("EPSG:4326");
		
		asArgs.add("-overwrite");
		
		String sGdalInput = sParentFolderPath + sBandName + ".vrt";
		String sGdalOutput = sParentFolderPath + sOutputFileName;
		asArgs.add(sGdalInput);
		asArgs.add(sGdalOutput);
		
		WasdiLog.debugLog("Sentinel6ProductReader.buildArgumentsGdalWarp. gdalwarp input: " + sGdalInput);
		WasdiLog.debugLog("Sentinel6ProductReader.buildArgumentsGdalWarp. gdalwarp output: " + sGdalOutput);
		
		return asArgs;
	}
	
	


	public static void main(String[] args) throws Exception {
		String sFilePath = "C:/Users/valentina.leone/.wasdi/S6A_P4_2__LR______20220814T223051_20220815T001047_20220815T002526_5996_065_017_009_EUM__OPE_NR_F06.SEN6.zip";
		Sentinel6ProductReader pr = new Sentinel6ProductReader(new File(sFilePath));
		String res = pr.adjustFileAfterDownload(sFilePath,
				"S6A_P4_2__LR______20220814T223051_20220815T001047_20220815T002526_5996_065_017_009_EUM__OPE_NR_F06.SEN6.zip");
		System.out.println(res);

	}

}
