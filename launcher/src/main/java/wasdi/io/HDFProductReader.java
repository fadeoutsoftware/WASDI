package wasdi.io;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import wasdi.dataproviders.CloudferroProviderAdapter;
import wasdi.shared.business.ecostress.EcoStressItemForReading;
import wasdi.shared.data.ecostress.EcoStressRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.gis.GdalBandInfo;
import wasdi.shared.utils.gis.GdalInfoResult;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class HDFProductReader extends WasdiProductReader {

	public HDFProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {
		ProductViewModel oViewModel = new ProductViewModel();
		oViewModel.setFileName(m_oProductFile.getName());
		oViewModel.setName(WasdiFileUtils.getFileNameWithoutLastExtension(m_oProductFile.getName()));

		NodeGroupViewModel oBandsGroup = new NodeGroupViewModel("Bands");
		oBandsGroup.setBands(new ArrayList<BandViewModel>());

		GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(m_oProductFile);
		if (oInfo != null && oInfo.bands != null && !oInfo.bands.isEmpty()) {
			int iWidth = (oInfo.size != null && oInfo.size.size() >= 2) ? oInfo.size.get(0) : 0;
			int iHeight = (oInfo.size != null && oInfo.size.size() >= 2) ? oInfo.size.get(1) : 0;
			int iBandIndex = 1;
			for (GdalBandInfo oGdalBand : oInfo.bands) {
				String sBandName = Utils.isNullOrEmpty(oGdalBand.description)
						? "Band_" + iBandIndex
						: oGdalBand.description;
				BandViewModel oBand = new BandViewModel(sBandName);
				oBand.setWidth(iWidth);
				oBand.setHeight(iHeight);
				oBandsGroup.getBands().add(oBand);
				iBandIndex++;
			}
		}
		else if (m_oProductFile.getName().toUpperCase().startsWith("EEH2TES_L2_LSTE")) {
			for (String sBand : Arrays.asList("LST", "Emis2", "Emis4", "Emis5", "BBE", "qa")) {
				oBandsGroup.getBands().add(new BandViewModel(sBand));
			}
		}

		oViewModel.setBandsGroups(oBandsGroup);
		return oViewModel;
	}
	
	@Override
	public String getProductBoundingBox() {
		try {
			GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(m_oProductFile);
			if (oInfo != null && oInfo.wgs84East != 0.0) {
				return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f",
						(float) oInfo.wgs84South, (float) oInfo.wgs84West,
						(float) oInfo.wgs84South, (float) oInfo.wgs84East,
						(float) oInfo.wgs84North, (float) oInfo.wgs84East,
						(float) oInfo.wgs84North, (float) oInfo.wgs84West,
						(float) oInfo.wgs84South, (float) oInfo.wgs84West);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("HDFProductReader.getProductBoundingBox. Error", oEx);
		}

		return "";
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}

	@Override
	public String getEPSG() {
		return super.getEPSG();
	}
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
		
		WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. Band: " + sBand + ", layer id: " + sLayerId + ", platform: " + sPlatform + " v1");
				
		boolean bIsGeoFileInWorkspace = false;
		
		String sVRTFilePath = null;
		
		String sWarpedFilePath = null;
		
		String sGeoFilePath = null;
		
		try {
			String sProductName = m_oProductFile.getName();		
						
			if (!sProductName.toUpperCase().startsWith("EEH2TES_L2_LSTE")) {
				return super.getFileForPublishBand(sBand, sLayerId, sPlatform);
			}

			// we need to publish the bands of LSTE ECOSTRESS products
			String sHdfDataset = "";
	        double dScaleFactor = 1.0;
	        boolean bIsScientific = true;

	        switch (sBand) {
		        case "LST":
		            sHdfDataset = "//LST";
		            dScaleFactor = 0.02;
		            break;
		        case "Emis2":
		        case "Emis4":
		        case "Emis5":
		            sHdfDataset = "//" + sBand; 
		            dScaleFactor = 0.0001; 
		            break;
		        case "BBE":
		            sHdfDataset = "//BBE";
		            dScaleFactor = 0.0001;
		            break;
		        case "qa":
		            sHdfDataset = "//qa";
		            dScaleFactor = 1.0;
		            bIsScientific = false;
		            break;
		        default:
		            WasdiLog.warnLog("HDFProductReader.getFileForPublishBand. Band " + sBand + " not supported.");
		            return null;
	        }
			
			
			String sLSTEProductPath = m_oProductFile.getAbsolutePath();
			
			// first of all, we need to find the dedicated L1_GEO file
			String sProductInfo = extractProductInfo(sProductName);
			
			String sGEOProductNamePrefix = "ECOv002_L1B_GEO_" + sProductInfo;
			
			
			EcoStressRepository oRepo = new EcoStressRepository();
			
			WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. Looking for GEO product " + sGEOProductNamePrefix);
			
			EcoStressItemForReading oEcostressItem = oRepo.getEcoStressByFileNamePrefix(sGEOProductNamePrefix);
			
			if (oEcostressItem == null) {
				WasdiLog.errorLog("HDFProductReader.getFileForPublishBand. No GEO product found for file " + sProductName + "and prefix " + sGEOProductNamePrefix);
				return null;
			}
			
			String sGeoFileName = oEcostressItem.getFileName();
			
			String sWorkspaceDirPath = m_oProductFile.getParent();
			if (!sWorkspaceDirPath.endsWith("/"))
				sWorkspaceDirPath += File.separator;
						
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Download folder path " + sWorkspaceDirPath);
			
			bIsGeoFileInWorkspace = new File (sWorkspaceDirPath + sGeoFileName).exists();
			
			if (bIsGeoFileInWorkspace) {
				WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. Geo file already present in workspace");
				sGeoFilePath = sWorkspaceDirPath + sGeoFileName;
			}
			else {
				WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. Downloading file: " + sGeoFileName);
				
				String sFileUrl = oEcostressItem.getUrl()+ "," + sProductName + ",";
				
				WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. File url: "+ sFileUrl);

				CloudferroProviderAdapter oProvider = new CloudferroProviderAdapter();
				sGeoFilePath = oProvider.executeDownloadFile(sFileUrl, null, null, sWorkspaceDirPath, null, 0);
			}
			
			if (Utils.isNullOrEmpty(sGeoFilePath)) {
				WasdiLog.errorLog("HDFProductReader.getFileForPublishBand. No GEO products has been downloaded");
				return null;
			}
						
			WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. Geo file" + sGeoFilePath);
		 			
			WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. Workspace path " + sWorkspaceDirPath);			
			
			if(!sWorkspaceDirPath.endsWith(File.separator))
				sWorkspaceDirPath += File.separator;
			
			sVRTFilePath = sWorkspaceDirPath + sLayerId + "_vrt.vrt";
			
			sWarpedFilePath = sWorkspaceDirPath + sLayerId + "_warped.tif";
			
			String sFinalTIFPath = sWorkspaceDirPath + sLayerId + ".tif";
			
			
			// GDAL TRANSLATE
			
			ArrayList<String> asTranslateArgs = new ArrayList<>();
			
			WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. Executing gdal translate");
			
			String sGdalCommand = "gdal_translate";
			sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
			asTranslateArgs.add(sGdalCommand);
			asTranslateArgs.add("-unscale"); 
			asTranslateArgs.add("HDF5:\"" + sLSTEProductPath + "\":" + sHdfDataset);
			asTranslateArgs.add(sVRTFilePath);
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Command " + String.join(" ", asTranslateArgs));
			ShellExecReturn oTranslateReturn = RunTimeUtils.shellExec(asTranslateArgs, true, true, true, true); 
			String sGdalLogs = oTranslateReturn.getOperationLogs();
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. [gdal-translate]: " + sGdalLogs);
			
			if (sGdalLogs.contains("ERROR")) {
				WasdiLog.errorLog("HDFProductReader.getFileForPublishBand. Some error occured in the gdal_traslate. Does not make sense to proceed");
				return null;
			}
 
			// attach georeferencing
			fixEcostressVrt(sVRTFilePath, sGeoFilePath, dScaleFactor);
			
			// GDAL WARP
			WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. Executing gdal warp");
			ArrayList<String> asWarpArgs = new ArrayList<>();
			sGdalCommand = "gdalwarp";
			sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
			asWarpArgs.add(sGdalCommand);
			asWarpArgs.add("-dstnodata");
			asWarpArgs.add("0");
			asWarpArgs.add("-geoloc");
			asWarpArgs.add("-t_srs");
			asWarpArgs.add("EPSG:4326");
			asWarpArgs.add("-r");
			asWarpArgs.add(bIsScientific ? "bilinear" : "near"); // we use near for the qa band
			asWarpArgs.add("-wo");
			asWarpArgs.add("SAMPLE_STEPS=100"); 
			asWarpArgs.add(sVRTFilePath);
			asWarpArgs.add(sWarpedFilePath);
			ShellExecReturn oWarpReturn = RunTimeUtils.shellExec(asWarpArgs, true, true, true, true); 
			sGdalLogs = oWarpReturn.getOperationLogs();
			WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. [gdal-warp]: " + sGdalLogs);
			
			if (sGdalLogs.contains("ERROR")) {
				WasdiLog.errorLog("HDFProductReader.getFileForPublishBand. Some error occured in the gdalwarp. Does not make sense to proceed");
				return null;
			}
			
			// gdal_fillnodata.py
			if (bIsScientific) {
				WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. Executing gdal_fillnodata.py");
				ArrayList<String> asFillArgs = new ArrayList<>();
				sGdalCommand = "gdal_fillnodata.py";
				sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
				asFillArgs.add(sGdalCommand);
				asFillArgs.add("-md");
				asFillArgs.add("15");
				asFillArgs.add(sWarpedFilePath);
				asFillArgs.add(sFinalTIFPath);
				ShellExecReturn oFillNoDataReturn = RunTimeUtils.shellExec(asFillArgs, true, true, true, true);
				sGdalLogs = oFillNoDataReturn.getOperationLogs();
				WasdiLog.debugLog("HDFProductReader.getFileForPublishBand. [gdal-fillnodata]: " + sGdalLogs);
				if (sGdalLogs.contains("ERROR")) {
					WasdiLog.errorLog("HDFProductReader.getFileForPublishBand. Some error occured in the gdal_fillnodata. Does not make sense to proceed");
					deleteTempFiles(bIsGeoFileInWorkspace, sGeoFilePath, sVRTFilePath, sWarpedFilePath);
					return null;
				}
			}
			else {
				// In the QA band, we simply rename the warped file 
	            Files.move(Paths.get(sWarpedFilePath), Paths.get(sFinalTIFPath), StandardCopyOption.REPLACE_EXISTING);
			}
			
			return new File(sFinalTIFPath);
		} catch (Exception oE) {
			WasdiLog.errorLog("HDFProductReader.getFileForPublishBand. Error ", oE);
		}
		finally {
			deleteTempFiles(bIsGeoFileInWorkspace, sGeoFilePath, sVRTFilePath, sWarpedFilePath);
		}

		return null;
	}
	
	
	public void deleteTempFiles(boolean bIsGeoFileInWorkspace, String sGeoFilePath, String sVRTFilePath, String sWarpedFilePath) {
		// delete the files generated for publishing the band
		if (!bIsGeoFileInWorkspace && !Utils.isNullOrEmpty(sGeoFilePath) && new File(sGeoFilePath).exists()) {
			FileUtils.deleteQuietly(new File(sGeoFilePath));
		}
		
		if (!Utils.isNullOrEmpty(sVRTFilePath) && new File(sVRTFilePath).exists()) {
			FileUtils.deleteQuietly(new File(sVRTFilePath));
		}
		
		if (!Utils.isNullOrEmpty(sWarpedFilePath) && new File(sWarpedFilePath).exists()) {
			FileUtils.deleteQuietly(new File(sWarpedFilePath));
		}
	}
	
	public void fixEcostressVrt(String sVrtPath, String sGeoH5Path, double dScaleFactor) {
	    try {
	        Path oPath = Paths.get(sVrtPath);
	        String sXML = new String(Files.readAllBytes(oPath), StandardCharsets.UTF_8);

	        String sGeoBlock = "\n  <Metadata domain=\"GEOLOCATION\">\n" +
	                "    <MDI key=\"X_DATASET\">HDF5:\"" + sGeoH5Path + "\"://Geolocation/longitude</MDI>\n" +
	                "    <MDI key=\"Y_DATASET\">HDF5:\"" + sGeoH5Path + "\"://Geolocation/latitude</MDI>\n" +
	                "    <MDI key=\"X_BAND\">1</MDI>\n" +
	                "    <MDI key=\"Y_BAND\">1</MDI>\n" +
	                "    <MDI key=\"PIXEL_OFFSET\">0</MDI>\n" +
	                "    <MDI key=\"LINE_OFFSET\">0</MDI>\n" +
	                "    <MDI key=\"PIXEL_STEP\">1</MDI>\n" +
	                "    <MDI key=\"LINE_STEP\">1</MDI>\n" +
	                "  </Metadata>";

	        sXML = sXML.replaceFirst("(?i)<VRTDataset[^>]*>", "$0" + sGeoBlock);

	        sXML = sXML.replace("dataType=\"UInt16\"", "dataType=\"Float32\"");
	        
	        if (!sXML.contains("<ScaleRatio>")) {
	        	sXML = sXML.replace("<SourceBand>1</SourceBand>", 
	                    "<SourceBand>1</SourceBand>\n      <ScaleRatio>" + dScaleFactor + "</ScaleRatio>\n      <ScaleOffset>0</ScaleOffset>");
	        }

	        Files.write(oPath, sXML.getBytes(StandardCharsets.UTF_8));
	    }
	    catch (Exception oE) {
	        WasdiLog.errorLog("HDFProductReader.fixEcostressVrt. Error ", oE);
	    }
	}
	
	private String extractProductInfo(String sFileName) {
        String sRegex = "(\\d{5}_\\d{3}_\\d{8}T\\d{6})";
        Pattern oPattern = Pattern.compile(sRegex);
        Matcher oMatcher = oPattern.matcher(sFileName);

        if (oMatcher.find()) 
            return oMatcher.group(1);
        
        return null;
	}
	


	public String getFilePathByPrefix(String sKnownFilePath, String sPrefix) {
	    Path pKnownFile = Paths.get(sKnownFilePath);
	    Path pParentDir = pKnownFile.getParent();

	    if (pParentDir == null) return null;

	    try (Stream<Path> sFiles = Files.find(pParentDir, 1, (path, attrs) -> 
	            path.getFileName().toString().startsWith(sPrefix))) {
	        
	        Optional<Path> oFoundFile = sFiles.findFirst();
	        
	        return oFoundFile.map(Path::toString).orElse(null);
	        
	    } catch (Exception oE) {
	        WasdiLog.errorLog("HDFProductReader.getFilePathByPrefix. Error extracting the file path ", oE);
	    }
	    return null;
	}
	
}
