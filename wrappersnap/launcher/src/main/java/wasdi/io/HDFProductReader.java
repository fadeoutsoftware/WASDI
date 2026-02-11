package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import wasdi.dataproviders.CloudferroProviderAdapter;
import wasdi.shared.business.ecostress.EcoStressItemForReading;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ecostress.EcoStressRepository;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

public class HDFProductReader extends SnapProductReader {

	public HDFProductReader(File oProductFile) {
		super(oProductFile);
	}
	
	@Override
	public String getProductBoundingBox() {
		
		String sBoundingBox = "";
		
		try { 
			Product oProduct = getSnapProduct();
			
			if (oProduct == null) {
				WasdiLog.infoLog("HDFProductReader.getProductBoundingBox: snap product is null");
				return sBoundingBox;
			}
			
			// y is the latitude, x is the longitude
			double dMinY = Double.NaN;
			double dMaxY = Double.NaN;
			double dMinX = Double.NaN;
			double dMaxX = Double.NaN;
			
			MetadataElement oMetadataRoot = oProduct.getMetadataRoot();
	        MetadataElement[] aoElements = oMetadataRoot.getElements();
	        
	        for (MetadataElement oMetadataElement : aoElements) {
	        	
	        	// maxX : EAST
	        	if (!Double.isNaN(oMetadataElement.getAttributeDouble("EastBoundingCoord", Double.NaN))) 
	        		dMaxX = oMetadataElement.getAttributeDouble("EastBoundingCoord");
	        	
	        	// minX: WEST
	        	if (!Double.isNaN(oMetadataElement.getAttributeDouble("WestBoundingCoord", Double.NaN))) 
	        		dMinX = oMetadataElement.getAttributeDouble("WestBoundingCoord");
	        	
	          	// maxY: NORTH
	        	if (!Double.isNaN(oMetadataElement.getAttributeDouble("NorthBoundingCoord", Double.NaN))) 
	        		dMaxY = oMetadataElement.getAttributeDouble("NorthBoundingCoord");
	        		
	        	// minY: SOUTH
	        	if (!Double.isNaN(oMetadataElement.getAttributeDouble("SouthBoundingCoord", Double.NaN))) 
	        		dMinY = oMetadataElement.getAttributeDouble("SouthBoundingCoord");
	        	
	        }
			
	        if (Double.isNaN(dMaxX) || Double.isNaN(dMinX) || Double.isNaN(dMaxY) || Double.isNaN(dMinY))
	        	return sBoundingBox;
	        
	        sBoundingBox = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", 
					(float) dMinY, (float) dMinX, (float) dMinY, (float) dMaxX, (float) dMaxY, (float) dMaxX, (float) dMaxY, (float) dMinX, (float) dMinY, (float) dMinX);	
		
		} catch (Exception oEx) {
			WasdiLog.errorLog("HDFProductReader.getProductBoundingBox. Error", oEx);
		}
        
		return sBoundingBox;
	}
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
		
		
		WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Band: " + sBand + ", layer id: " + sLayerId + ", platform: " + sPlatform + " v1");
		
		WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Absolute file of the product" + m_oProductFile.getAbsolutePath());
		
		
		try {
			String sProductName = m_oProductFile.getName();		
			
			// TODO: con la getpath, posso vedere se c'e' gia' un file GEO
			
			if (!sProductName.toUpperCase().startsWith("EEH2TES_L2_LSTE")) {
				return super.getFileForPublishBand(sBand, sLayerId, sPlatform);
			}
			
			if (!sBand.equals("LST")) {
				WasdiLog.warnLog("HDFProductReader.getFileForPublishBand. Band is not LST. Conversion not supported");
				return null;
			}
			
			// we need to publish the LST bands of LSTE ECOSTRESS products
			
			String sLSTEProductPath = m_oProductFile.getAbsolutePath();
			
			// first of all, we need to find the dedicated L1_GEO file
			String sProductInfo = extractProductInfo(sProductName);
			
			String sGEOProductNamePrefix = "ECOv002_L1B_GEO_" + sProductInfo;
			
			/*
			EcoStressRepository oRepo = new EcoStressRepository();
			
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Looking for GEO product " + sGEOProductNamePrefix);
			
			EcoStressItemForReading oEcostressItem = oRepo.getEcoStressByFileNamePrefix(sGEOProductNamePrefix);
			
			if (oEcostressItem == null) {
				WasdiLog.errorLog("HDFProductReader.getFileForPublishBand. No GEO product found for file " + sProductName + "and prefix " + sGEOProductNamePrefix);
				return null;
			}
			*/
			
			// from here I can download the GEO product in  the temp folder
			// WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Downloading file: " + oEcostressItem.getFileName());
			
			/*
			String sFileUrl = oEcostressItem.getS3Path() + "," + sProductName + ",";
			
			
			String sDownloadFolder = WasdiConfig.Current.paths.wasdiTempFolder;
			if (!sDownloadFolder.endsWith("/"))
				sDownloadFolder += File.separator;
			
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. File url: "+ sFileUrl);
			
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Download folder path " + sDownloadFolder);
			
			CloudferroProviderAdapter oProvider = new CloudferroProviderAdapter();
			
			String sGEOFilePath = oProvider.executeDownloadFile(sFileUrl, null, null, sDownloadFolder, null, 0);
			
			if (Utils.isNullOrEmpty(sGEOFilePath)) {
				WasdiLog.errorLog("HDFProductReader.getFileForPublishBand. No GEO products has been downloaded");
				return null;
			}
			*/
			
			String sGEOFilePath = this.getFilePathByPrefix(sLSTEProductPath, sGEOProductNamePrefix);
			
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. " + sGEOFilePath);

			 
			String sWorkspaceDirPath = m_oProductFile.getParent();
			
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Workspace path " + sWorkspaceDirPath);
			
			String sProductNameNoExtension = WasdiFileUtils.getFileNameWithoutExtensionsAndTrailingDots(sProductName);
			
			
			if(!sWorkspaceDirPath.endsWith(File.separator))
				sWorkspaceDirPath += File.separator;
			
			String sVRTFilePath = sWorkspaceDirPath + sProductNameNoExtension + "_vrt.vrt";
			
			String sWarpedFilePath = sWorkspaceDirPath + sProductNameNoExtension + "_warped.tif";
			
			String sFinalTIFPath = sWorkspaceDirPath + sLayerId + ".tif";
			
			
			// GDAL TRANSLATE
			
			ArrayList<String> asTranslateArgs = new ArrayList<>();
			
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Executing gdal translate");
			
			String sGdalCommand = "gdal_translate";
			sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
			asTranslateArgs.add(sGdalCommand);
			asTranslateArgs.add("-unscale"); 
			asTranslateArgs.add("HDF5:\"" + sLSTEProductPath + "\"://LST");
			asTranslateArgs.add(sVRTFilePath);
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Command " + String.join(" ", asTranslateArgs));
			ShellExecReturn oTranslateReturn = RunTimeUtils.shellExec(asTranslateArgs, true, true, true, true); 
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. [gdal-translate]: " + oTranslateReturn.getOperationLogs());
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. [gdal-translate-return-code]: " + oTranslateReturn.getOperationReturn());
 
			// attach georeferencing
			fixEcostressVrt(sVRTFilePath, sGEOFilePath);
			
			// GDAL WARP
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Executing gdal warp");
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
			asWarpArgs.add("cubicspline"); 
			asWarpArgs.add("-wo");
			asWarpArgs.add("SAMPLE_STEPS=1000"); 
			asWarpArgs.add(sVRTFilePath);
			asWarpArgs.add(sWarpedFilePath);
			ShellExecReturn oWarpReturn = RunTimeUtils.shellExec(asWarpArgs, true, true, true, true); 
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. [gdal-warp]: " + oWarpReturn.getOperationLogs());
			
			// gdal_fillnodata.py
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Executing gdal_fillnodata.py");
			ArrayList<String> asFillArgs = new ArrayList<>();
			sGdalCommand = "gdal_fillnodata.py";
			sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
			
			asFillArgs.add(sGdalCommand);
			asFillArgs.add("-md");
			asFillArgs.add("20"); // Raggio di ricerca per il riempimento
			asFillArgs.add(sWarpedFilePath);
			asFillArgs.add(sFinalTIFPath);
			ShellExecReturn oFillNoDataReturn = RunTimeUtils.shellExec(asFillArgs, true, true, true, true);
			WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. [gdal-fillnodata]: " + oFillNoDataReturn.getOperationLogs());
			
			// TODO: at the end, delete the GEO file from the temp folder
		
		} catch (Exception oE) {
			WasdiLog.errorLog("HDFProductReader.getFileForPublishBand. Error ", oE);
		}

		
		return null;
	}
	
	public void fixEcostressVrt(String vrtPath, String geoH5Path) {
		try {
		    Path oPath = Paths.get(vrtPath);
		    String sXML = new String(Files.readAllBytes(oPath), StandardCharsets.UTF_8);
	
		    // build the Geolocation using the driver HDF5
		    String geoBlock = "<Metadata domain=\"GEOLOCATION\">\n" +
		            "    <MDI key=\"X_DATASET\">HDF5:\"" + geoH5Path + "\"://Geolocation/longitude</MDI>\n" +
		            "    <MDI key=\"Y_DATASET\">HDF5:\"" + geoH5Path + "\"://Geolocation/latitude</MDI>\n" +
		            "    <MDI key=\"X_BAND\">1</MDI>\n" +
		            "    <MDI key=\"Y_BAND\">1</MDI>\n" +
		            "    <MDI key=\"PIXEL_OFFSET\">0</MDI>\n" +
		            "    <MDI key=\"LINE_OFFSET\">0</MDI>\n" +
		            "    <MDI key=\"PIXEL_STEP\">1</MDI>\n" +
		            "    <MDI key=\"LINE_STEP\">1</MDI>\n" +
		            "  </Metadata>\n  <Metadata>";
	
		    // insert the geoloc block, changing the type to fload and applying the scale factor 0.02
		    sXML = sXML.replaceFirst("<Metadata>", geoBlock);
		    sXML = sXML.replace("dataType=\"UInt16\"", "dataType=\"Float32\"");
		    
		    if (!sXML.contains("<ScaleRatio>")) {
		    	sXML = sXML.replace("<SourceBand>1</SourceBand>", 
		                "<SourceBand>1</SourceBand>\n      <ScaleRatio>0.02</ScaleRatio>\n      <ScaleOffset>0</ScaleOffset>");
		    }
	
		    Files.write(oPath, sXML.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception oE) {
			WasdiLog.errorLog("HDFProductReader.fixExostressVrt. Error ", oE);
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

	    // Use try-with-resources to ensure the stream is closed
	    try (Stream<Path> sFiles = Files.find(pParentDir, 1, (path, attrs) -> 
	            path.getFileName().toString().startsWith(sPrefix))) {
	        
	        Optional<Path> oFoundFile = sFiles.findFirst();
	        
	        return oFoundFile.map(Path::toString).orElse(null);
	        
	    } catch (IOException e) {
	        // Log the error (e.g., WasdiLog.errorLog)
	        return null;
	    }
	}
	
	
	public static void main(String[]args) throws Exception {
		
		WasdiConfig.readConfig("C:/temp/wasdi/wasdiLocalTESTConfig_develop.json");
		HDFProductReader oReader = new HDFProductReader(new File("C:/WASDI/datasets/ecostress/EEH2TES_L2_LSTE_37139_003_20250123T152424_0000_00.h5"));
		oReader.getFileForPublishBand("LST", "111", Platforms.ECOSTRESS);
		
		
	}
	
	
}
