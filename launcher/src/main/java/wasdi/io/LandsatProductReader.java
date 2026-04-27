package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.json.JSONObject;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.gis.GdalInfoResult;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * Product reader class for Landsat-5, Landsat-7 products and Landsat-8 L2 products
 * @author valentina.leone
 *
 */
public class LandsatProductReader extends WasdiProductReader {

	public LandsatProductReader(File oProductFile) {
		super(oProductFile);	
	}
	
	@Override
	public ProductViewModel getProductViewModel() {
		if (m_oProductFile == null) {
			WasdiLog.warnLog("LandsatProductReader.getProductViewModel: product file is null");
			return null;
		}

		WasdiLog.debugLog("LandsatProductReader.getProductViewModel. Product file path " + m_oProductFile.getAbsolutePath());

		ProductViewModel oViewModel = new ProductViewModel();
		oViewModel.setFileName(m_oProductFile.getName());
		oViewModel.setName(m_oProductFile.getName());

		NodeGroupViewModel oBandsGroup = new NodeGroupViewModel("Bands");
		oBandsGroup.setBands(new ArrayList<BandViewModel>());

		List<File> aoBandFiles = getLandsatBandFiles();
		LinkedHashSet<String> aoSeenBands = new LinkedHashSet<>();

		for (File oBandFile : aoBandFiles) {
			String sBandName = inferBandNameFromFileName(oBandFile.getName());
			if (Utils.isNullOrEmpty(sBandName) || aoSeenBands.contains(sBandName)) {
				continue;
			}

			BandViewModel oBand = new BandViewModel(sBandName);
			try {
				GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(oBandFile);
				if (oInfo != null && oInfo.size != null && oInfo.size.size() >= 2) {
					oBand.setWidth(oInfo.size.get(0));
					oBand.setHeight(oInfo.size.get(1));
				}
			}
			catch (Exception oEx) {
				WasdiLog.warnLog("LandsatProductReader.getProductViewModel: cannot read raster size for " + oBandFile.getName());
			}

			oBandsGroup.getBands().add(oBand);
			aoSeenBands.add(sBandName);
		}

		oViewModel.setBandsGroups(oBandsGroup);

		WasdiLog.debugLog("LandsatProductReader.getProductViewModel: done");
		return oViewModel;
	}

	private List<File> getLandsatBandFiles() {
		List<File> aoBandFiles = new ArrayList<>();

		if (m_oProductFile == null) return aoBandFiles;

		if (m_oProductFile.isFile()) {
			if (isTiffFile(m_oProductFile)) {
				aoBandFiles.add(m_oProductFile);
			}
		}
		else if (m_oProductFile.isDirectory()) {
			File[] aoChildren = m_oProductFile.listFiles();
			if (aoChildren != null) {
				for (File oChild : aoChildren) {
					if (oChild.isFile() && isTiffFile(oChild)) {
						aoBandFiles.add(oChild);
					}
					else if (oChild.isDirectory() && oChild.getName().toUpperCase().endsWith(".TIFF")) {
						File[] aoTiffFolderFiles = oChild.listFiles();
						if (aoTiffFolderFiles != null) {
							for (File oFile : aoTiffFolderFiles) {
								if (oFile.isFile() && isTiffFile(oFile)) {
									aoBandFiles.add(oFile);
								}
							}
						}
					}
				}
			}
		}

		Collections.sort(aoBandFiles, Comparator.comparing(File::getName));
		return aoBandFiles;
	}

	private boolean isTiffFile(File oFile) {
		String sName = oFile.getName().toLowerCase();
		return sName.endsWith(".tif") || sName.endsWith(".tiff");
	}

	private String inferBandNameFromFileName(String sFileName) {
		if (Utils.isNullOrEmpty(sFileName)) return null;

		String sBaseName = sFileName;
		int iDot = sBaseName.lastIndexOf('.');
		if (iDot > 0) sBaseName = sBaseName.substring(0, iDot);

		Pattern oPattern = Pattern.compile("(?:^|_)(SR_B\\d+|ST_B\\d+|B\\d+|B\\d+_VCID_\\d+|BQA|QA_PIXEL|QA_RADSAT|QA_AEROSOL|AEROSOL)$", Pattern.CASE_INSENSITIVE);
		Matcher oMatcher = oPattern.matcher(sBaseName);
		if (oMatcher.find()) {
			return oMatcher.group(1).toUpperCase();
		}

		int iUnderscore = sBaseName.lastIndexOf('_');
		if (iUnderscore >= 0 && iUnderscore < sBaseName.length() - 1) {
			return sBaseName.substring(iUnderscore + 1).toUpperCase();
		}

		return sBaseName;
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
			
			File oFirstBandFile = getFirstBandFile();
			if (oFirstBandFile != null) {
				GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(oFirstBandFile);
				if (oInfo != null && oInfo.wgs84East != 0.0) {
					return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f",
							(float) oInfo.wgs84South, (float) oInfo.wgs84West,
							(float) oInfo.wgs84South, (float) oInfo.wgs84East,
							(float) oInfo.wgs84North, (float) oInfo.wgs84East,
							(float) oInfo.wgs84North, (float) oInfo.wgs84West,
							(float) oInfo.wgs84South, (float) oInfo.wgs84West);
				}
			}
		
		} catch (IOException oEx) {
			WasdiLog.errorLog("LandsatProductReader.getProductBoundingBox. Exception when trying to read the bounding box ", oEx);
		} catch (Exception oEx) {
			WasdiLog.errorLog("LandsatProductReader.getProductBoundingBox. Unexpected exception ", oEx);
		}
		
		return ""; 
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
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}
	
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
		try {
			if (Utils.isNullOrEmpty(sBand)) {
				WasdiLog.warnLog("LandsatProductReader.getFileForPublishBand: band is null/empty");
				return null;
			}

			File oBandFile = findBandFileByBandName(sBand);
			if (oBandFile == null) {
				WasdiLog.warnLog("LandsatProductReader.getFileForPublishBand: band not found: " + sBand);
				return null;
			}

			if (isFileAlready4326(oBandFile)) {
				return oBandFile;
			}

			String sOutputPath = m_oProductFile.getParentFile().getAbsolutePath() + File.separator + sLayerId + ".tif";
			String sGdalWarp = GdalUtils.adjustGdalFolder("gdalwarp");
			ArrayList<String> asWarpArgs = new ArrayList<>();
			asWarpArgs.add(sGdalWarp);
			asWarpArgs.add("-t_srs");
			asWarpArgs.add("EPSG:4326");
			asWarpArgs.add("-of");
			asWarpArgs.add("GTiff");
			asWarpArgs.add(oBandFile.getAbsolutePath());
			asWarpArgs.add(sOutputPath);

			RunTimeUtils.shellExec(asWarpArgs, true, true, true, true);

			File oOutput = new File(sOutputPath);
			if (oOutput.exists()) {
				return oOutput;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LandsatProductReader.getFileForPublishBand: error ", oEx);
		}

		return null;
	}

	@Override
	public String getEPSG() {
		try {
			File oFirstBandFile = getFirstBandFile();
			if (oFirstBandFile == null) return null;

			GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(oFirstBandFile);
			if (oInfo != null && !Utils.isNullOrEmpty(oInfo.coordinateSystemWKT)) {
				CoordinateReferenceSystem oCRS = CRS.parseWKT(oInfo.coordinateSystemWKT);
				return CRS.lookupIdentifier(oCRS, true);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LandsatProductReader.getEPSG: exception ", oEx);
		}
		return null;
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

	private File findBandFileByBandName(String sBand) {
		List<File> aoBandFiles = getLandsatBandFiles();
		String sRequestedBand = sBand.toUpperCase();

		for (File oBandFile : aoBandFiles) {
			String sCandidate = inferBandNameFromFileName(oBandFile.getName());
			if (!Utils.isNullOrEmpty(sCandidate) && sCandidate.equalsIgnoreCase(sRequestedBand)) {
				return oBandFile;
			}
		}

		for (File oBandFile : aoBandFiles) {
			if (oBandFile.getName().toUpperCase().contains("_" + sRequestedBand + ".")) {
				return oBandFile;
			}
		}

		return null;
	}

	private File getFirstBandFile() {
		List<File> aoBandFiles = getLandsatBandFiles();
		if (aoBandFiles.isEmpty()) return null;
		return aoBandFiles.get(0);
	}

	private boolean isFileAlready4326(File oBandFile) {
		try {
			GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(oBandFile);
			if (oInfo == null || Utils.isNullOrEmpty(oInfo.coordinateSystemWKT)) return false;

			CoordinateReferenceSystem oCRS = CRS.parseWKT(oInfo.coordinateSystemWKT);
			String sEpsg = CRS.lookupIdentifier(oCRS, true);
			return !Utils.isNullOrEmpty(sEpsg) && sEpsg.contains("4326");
		}
		catch (Exception oEx) {
			WasdiLog.warnLog("LandsatProductReader.isFileAlready4326: cannot detect EPSG for " + oBandFile.getName());
			return false;
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
		
	}
}
