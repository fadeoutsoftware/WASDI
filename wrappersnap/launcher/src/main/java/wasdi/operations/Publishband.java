package wasdi.operations;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import wasdi.LauncherMain;
import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.geoserver.Publisher;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.payloads.PublishBandPayload;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.PublishBandResultViewModel;

public class Publishband extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		WasdiLog.infoLog("Publishband.executeOperation");
		
        String sLayerId = "";
        
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}

        try {
        	
        	PublishBandParameter oParameter = (PublishBandParameter) oParam;

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 0);

            // Read File Name
            String sInputFile = oParameter.getFileName();
            
            // Check integrity
            if (Utils.isNullOrEmpty(sInputFile)) {
            	
                // File not good!!
                WasdiLog.warnLog("Publishband.executeOperation: file is null or empty");
                
                String sError = "Input File path is null";
                m_oProcessWorkspaceLogger.log(sError);

                // Send KO to Rabbit
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());

                return false;
            }            

            // Generate full path name of the input file
            sInputFile = PathsConfig.getWorkspacePath(oParameter) + sInputFile;
            
            // Debug Utils: to debug publish band in local, we can force the path of the file to be published
            String sBackup = sInputFile;
            
            if (WasdiConfig.Current.geoserver.localDebugPublisBand) {
            	// Force it as it was on the standard server path
            	sInputFile = "/data/wasdi/" + oParam.getWorkspaceOwnerId() + "/" + oParam.getWorkspace() + "/" + oParameter.getFileName();
            }
            
            // Try to get the file
            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sInputFile);

            if (oDownloadedFile == null) {
            	oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(WasdiFileUtils.fixPathSeparator(sInputFile));
            }

            if (oDownloadedFile == null) {
                WasdiLog.errorLog("Publishband.executeOperation: Downloaded file is null!! Return empyt layer id for [" + sInputFile + "]");
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), "Cannot find product to publish", oParameter.getExchange());
                return false;
            }
            
            // it was only for backup, restore the real local path
            if (WasdiConfig.Current.geoserver.localDebugPublisBand) {
            	sInputFile = sBackup;
            }

            // Get the product name
            String sProductName = oDownloadedFile.getProductViewModel().getName();

            m_oProcessWorkspaceLogger.log("Publish Band " + sProductName + " - " + oParameter.getBandName());
            WasdiLog.debugLog("Publishband.executeOperation: " + sProductName + " - " + oParameter.getBandName());

            // Create input file object
            File oInputFile = new File(sInputFile);

            // set file size
            setFileSizeToProcess(oInputFile, oProcessWorkspace);

            // Generate a random Layer Id
            sLayerId = Utils.getRandomName();

            // Is already published?
            PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
            PublishedBand oAlreadyPublished = oPublishedBandsRepository.getPublishedBand(sInputFile, oParameter.getBandName());

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 10);

            if (oAlreadyPublished != null) {
                // Yes !!
                WasdiLog.debugLog("Publishband.executeOperation:  Band already published. Return result");

                m_oProcessWorkspaceLogger.log("Band already published.");

                // Generate the View Model
                PublishBandResultViewModel oVM = new PublishBandResultViewModel();
                oVM.setBandName(oParameter.getBandName());
                oVM.setProductName(sProductName);
                oVM.setLayerId(sLayerId);

                m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), oVM, oParameter.getExchange());
                
                return true;
            }

            // Default Style: can be changed in the following lines depending by the product
            String sStyle = getStyleByFileName(sInputFile);
                        
            // Finally, if specified, we set the style specified by the product
            if (Utils.isNullOrEmpty(oParameter.getStyle()) == false) {
                sStyle = oParameter.getStyle();
            }
            
            WasdiLog.debugLog("Publishband.executeOperation:  Generating Band Image with style " + sStyle);
            m_oProcessWorkspaceLogger.log("Generate Band Image with style " + sStyle);

            // Create the Product Reader
			WasdiProductReader oReadProduct = WasdiProductReaderFactory.getProductReader(oInputFile);
			
			String sPlatform = oDownloadedFile.getPlatform();
			
			if (Utils.isNullOrEmpty(sPlatform)) {
				sPlatform = MissionUtils.getPlatformFromSatelliteImageFileName(sPlatform);
			}
			
			// Ask to obtain the file to send to geoserver
			File oFileToCopy = oReadProduct.getFileForPublishBand(oParameter.getBandName(), sLayerId, sPlatform);
			
			if (oFileToCopy == null) {
                WasdiLog.debugLog("Publishband.executeOperation:  File for geoserver is null, return");
                m_oProcessWorkspaceLogger.log("Sorry, but we do not know how to show this file on the map");
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), "Looks we cannot show\nthis file on map", oParameter.getExchange());
                return false;
			}
			            
            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 20);

            // write the data directly to GeoServer Data Dir
            String sTargetDir = WasdiConfig.Current.paths.geoserverDataDir;

            if (!(sTargetDir.endsWith("/") || sTargetDir.endsWith("\\"))) sTargetDir += "/";
            sTargetDir += sLayerId + "/";

            File oTargetDir = new File(sTargetDir);
            
            if (!oTargetDir.exists()) {
            	oTargetDir.mkdirs();
            }

            // List of the files copied
            ArrayList<String> asCopiedFiles = new ArrayList<String>();
            
            // Output file Path
            String sOutputFilePath = sTargetDir + oFileToCopy.getName();

            // Output File
            File oOutputFile = new File(sOutputFilePath);

            WasdiLog.debugLog("Publishband.executeOperation: copy geoserver ready file to " + sOutputFilePath + " [LayerId] = " + sLayerId);
            
            FileUtils.copyFile(oFileToCopy, oOutputFile);
            asCopiedFiles.add(oOutputFile.getPath());
            
            WasdiLog.debugLog("Publishband.executeOperation: search for other files to copy (same name, different extension)");
            
			ArrayList<String> asFilesToCopy = new ArrayList<String>();
			File oWorkspaceFolder = new File(oFileToCopy.getParent());
			
			// Take all the files in the folder
			File[] aoWorkspaceFiles = oWorkspaceFolder.listFiles();
			
			// We are searching for files with the same name but different extension
			String sBaseFileNameFilter = oFileToCopy.getName();
			sBaseFileNameFilter = WasdiFileUtils.getFileNameWithoutLastExtension(sBaseFileNameFilter);
			sBaseFileNameFilter += ".";
			
			if (aoWorkspaceFiles != null) {
				for (File oChild : aoWorkspaceFiles) {
					// Is it me?
					if (oChild.getName().equals(oFileToCopy.getName())) continue;
					
					// Does it match?
					if (oChild.getName().startsWith(sBaseFileNameFilter))  {
						if (!oChild.getName().toLowerCase().endsWith(".zip")) {
							asFilesToCopy.add(oChild.getPath());
							WasdiLog.debugLog("Publishband.executeOperation: found other file to copy " + oChild.getName());							
						}
					}
				}
			}
            
            if (asFilesToCopy.size()>0) {
    			for (String sFileToCopy : asFilesToCopy) {
    				String sOtherOutputFile = oOutputFile.getPath();
    				String sOtherOutputExtension = WasdiFileUtils.getFileNameExtension(sOtherOutputFile);
    				String sNewExtension = WasdiFileUtils.getFileNameExtension(sFileToCopy);
    				
    				sOtherOutputFile = sOtherOutputFile.replace(sOtherOutputExtension, sNewExtension);
    				
    				WasdiLog.debugLog("Publishband.executeOperation: copy also " + sFileToCopy + " to " + sOtherOutputFile);
    				
    				FileUtils.copyFile(new File(sFileToCopy), new File(sOtherOutputFile));
    				asCopiedFiles.add(sOtherOutputFile);
    			}            	
            }
            

            m_oProcessWorkspaceLogger.log("Publish on geoserver");

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 50);

            // Create the geoserver Manager
            GeoServerManager oGeoServerManager = new GeoServerManager();
            
            // Check or Get the style from WASDI
            sStyle = checkOrGetStyle(oGeoServerManager, sStyle, oParam);
            
            //Ok publish
            Publisher oPublisher = new Publisher();
            
            WasdiProductReader oFileToPublishReader = WasdiProductReaderFactory.getProductReader(oOutputFile);
            
            String sEPSG = oFileToPublishReader.getEPSG();
            
            if (Utils.isNullOrEmpty(sEPSG)) {
            	WasdiLog.errorLog("Publishband.executeOperation: EPSG is still null. Try to recover with default EPSG:4326");
            	sEPSG = "EPSG:4326";
            }
            
            if (sOutputFilePath.toLowerCase().endsWith(".shp")) {
                WasdiLog.debugLog("Publishband.executeOperation: Call publish shapefile sOutputFilePath = " + sOutputFilePath + " , sLayerId = " + sLayerId + " Style = " + sStyle);
                sLayerId = oPublisher.publishShapeFile(sOutputFilePath, asCopiedFiles, sLayerId, sEPSG, sStyle, oGeoServerManager);
            }
            else if (MissionUtils.isGeoPackageFile(new File(sOutputFilePath))) {
                WasdiLog.debugLog("Publishband.executeOperation: Call publish GeoPackage sOutputFilePath = " + sOutputFilePath + " , sLayerId = " + sLayerId + " Style = " + sStyle);
                sLayerId = oPublisher.publishGeoPackageFile(sOutputFilePath, sLayerId, oParameter.getBandName(), sStyle, oGeoServerManager);            	
            }
            else {
                WasdiLog.debugLog("Publishband.executeOperation: Call publish geotiff sOutputFilePath = " + sOutputFilePath + " , sLayerId = " + sLayerId + " Style = " + sStyle);
                sLayerId = oPublisher.publishGeoTiff(sOutputFilePath, sLayerId, sEPSG, sStyle, oGeoServerManager);            	
            }

            WasdiLog.debugLog("Publishband.executeOperation: Obtained sLayerId = " + sLayerId);

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 90);

            boolean bResultPublishBand = true;

            if (sLayerId == null) {
                m_oProcessWorkspaceLogger.log("Error publishing in Geoserver... :(");
                bResultPublishBand = false;
                WasdiLog.errorLog("Publishband.executeOperation: Image not published . ");
                throw new Exception("Layer Id is null. Image not published");
            } else {

                m_oProcessWorkspaceLogger.log("Ok got layer id " + sLayerId);

                WasdiLog.debugLog("Publishband.executeOperation: Image published.");

                // get bounding box from data base
                String sBBox = oDownloadedFile.getBoundingBox();
                String sGeoserverBBox = oGeoServerManager.getLayerBBox(sLayerId);

                WasdiLog.debugLog("Publishband.executeOperation: Bounding Box: " + sBBox);
                WasdiLog.debugLog("Publishband.executeOperation: Geoserver Bounding Box: " + sGeoserverBBox + " for Layer Id " + sLayerId);

                // Create Entity
                PublishedBand oPublishedBand = new PublishedBand();
                oPublishedBand.setLayerId(sLayerId);
                oPublishedBand.setProductName(oDownloadedFile.getFilePath());
                oPublishedBand.setBandName(oParameter.getBandName());
                oPublishedBand.setUserId(oParameter.getUserId());
                oPublishedBand.setWorkspaceId(oParameter.getWorkspace());
                oPublishedBand.setBoundingBox(sBBox);
                oPublishedBand.setGeoserverBoundingBox(sGeoserverBBox);

                Node oNode = LauncherMain.getWorkspaceNode(oParameter.getWorkspace());

                if (oNode != null) {
                    WasdiLog.debugLog("Publishband.executeOperation: node code: " + oNode.getNodeCode());
                    oPublishedBand.setGeoserverUrl(oNode.getNodeGeoserverAddress());
                }

                // Add it the the db
                oPublishedBandsRepository.insertPublishedBand(oPublishedBand);

                WasdiLog.debugLog("Publishband.executeOperation: Band instered in db");

                // Create the View Model
                PublishBandResultViewModel oVM = new PublishBandResultViewModel();
                oVM.setBandName(oParameter.getBandName());
                oVM.setProductName(sProductName);
                oVM.setLayerId(sLayerId);
                oVM.setBoundingBox(sBBox);
                oVM.setGeoserverBoundingBox(sGeoserverBBox);
                oVM.setGeoserverUrl(oPublishedBand.getGeoserverUrl());

                // P.Campanella 2019/05/02: Wait a little bit to make GeoServer "finish" the process
                try {
                	Thread.sleep(1000);
                } catch (InterruptedException oEx) {
                	Thread.currentThread().interrupt();
                	WasdiLog.errorLog("PunlishBand.executeOperation: thread was interrupted");
                }

                m_oSendToRabbit.SendRabbitMessage(bResultPublishBand, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), oVM, oParameter.getExchange());
                
                m_oProcessWorkspaceLogger.log("Band published " + new EndMessageProvider().getGood());

                PublishBandPayload oPayload = new PublishBandPayload();

                oPayload.setBand(oParameter.getBandName());
                oPayload.setProduct(sProductName);
                oPayload.setLayerId(sLayerId);
                
                setPayload(oProcessWorkspace, oPayload);

                oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
            }
            
            updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);
            
            return true;
            
        } catch (Exception oEx) {

            m_oProcessWorkspaceLogger.log("Exception " + oEx.toString());

            WasdiLog.errorLog("Publishband.executeOperation: Exception " + oEx.toString() + " " + ExceptionUtils.getStackTrace(oEx));

            String sError = ExceptionUtils.getMessage(oEx);

            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.PUBLISHBAND.name(), oParam.getWorkspace(), sError, oParam.getExchange());            
        } 

		return false;
	}

	protected String getStyleByFileName(String sFile) {
        // Default Style: can be changed in the following lines depending by the product		
		
        String sStyle = "raster";

        // Hard Coded set Flood Style - STYLES HAS TO BE MANAGED
        if (sFile.toUpperCase().contains("FLOOD")) {
            sStyle = "DDS_FLOODED_AREAS";
        }
        // Hard Coded set NDVI Style - STYLES HAS TO BE MANAGED
        if (sFile.toUpperCase().contains("NDVI")) {
            sStyle = "NDVI";
        }
        // Hard Coded set Burned Areas Style - STYLES HAS TO BE MANAGED
        if (sFile.toUpperCase().contains("BURNEDAREA")) {
            sStyle = "burned_areas";
        }
        // Hard Coded set Flood Risk Style - STYLES HAS TO BE MANAGED
        if (sFile.toUpperCase().contains("FRISK")) {
            sStyle = "frisk";
        }
        // Hard Coded set rgb Style - STYLES HAS TO BE MANAGED
        if (sFile.toUpperCase().contains("_RGB")) {
            sStyle = "wasdi_s2_rgb";
        }
        
        if (sFile.toUpperCase().contains("S5P") && sFile.toUpperCase().contains("_CH4_")) {
            sStyle = "s5p_ch4";
        }
        
        if (sFile.toUpperCase().contains("S5P") && sFile.toUpperCase().contains("_CO_")) {
            sStyle = "s5p_co";
        }
        
        if (sFile.toUpperCase().contains("S5P") && sFile.toUpperCase().contains("_HCHO_")) {
            sStyle = "s5p_hcho";
        }
        
        if (sFile.toUpperCase().contains("S5P") && sFile.toUpperCase().contains("_NO2_")) {
            sStyle = "s5p_no2";
        }
        
        if (sFile.toUpperCase().contains("S5P") && sFile.toUpperCase().contains("_O3_")) {
            sStyle = "s5p_o3";
        }
        
        if (sFile.toUpperCase().contains("S5P") && sFile.toUpperCase().contains("_SO2_")) {
            sStyle = "s5p_so2";
        }
        
        if (sFile.contains("MeteOcean"))  {
        	sStyle = "MeteOcean";
        }
        
        if (WasdiFileUtils.isShapeFile(sFile)) {
        	sStyle = "polygon";
        }
        
        if (MissionUtils.isGeoPackageFile(new File(sFile))) {
        	sStyle = "polygon";
        }
                
        return sStyle;
	}
	
	/**
	 * Checks if the requested style is on geoserver. If it is not
	 * it downloads it
	 * @param oGeoServerManager GeoServer Manager 
	 * @param sStyle Style to check
	 * @param oParameter Base Parameter, to use the session to download the style if needed
	 * @return
	 */
	protected String checkOrGetStyle(GeoServerManager oGeoServerManager, String sStyle, BaseParameter oParameter) {
        // Do we have the style in this Geoserver?
        if (!oGeoServerManager.styleExists(sStyle)) {

            // Not yet: obtain styles root path
            String sStylePath =PathsConfig.getStylesPath();

            // Set the style file
            sStylePath += sStyle + ".sld";

            File oStyleFile = new File(sStylePath);

            // Do we have the file?
            if (!oStyleFile.exists()) {
                // No, Download style
                WasdiLog.infoLog("Publishband.executeOperation: download style " + sStyle + " from main node");
                String sRet = downloadStyle(sStyle, oParameter.getSessionID(), sStylePath);

                // Check download result
                if (!sRet.equals(sStylePath)) {
                    // Not good...
                    WasdiLog.errorLog("Publishband.executeOperation: error downloading style " + sStyle);
                }
            }

            // Publish the style
            if (oGeoServerManager.publishStyle(sStylePath)) {
                WasdiLog.infoLog("Publishband.executeOperation: published style " + sStyle + " on local geoserver");
            } else {
                WasdiLog.errorLog("Publishband.executeOperation: error publishing style " + sStyle + " reset on raster");
                sStyle = "raster";
            }
        }
        
        return sStyle;
	}
	
    /**
     * Download a Style on the local PC
     *
     * @param sSessionId
     * @return
     */
    protected String downloadStyle(String sStyle, String sSessionId, String sDestinationFileFullPath) {
        try {

            if (sStyle == null) {
                WasdiLog.errorLog("Publishband.downloadStyle: sStyle must not be null");
                return "";
            }

            if (sStyle.equals("")) {
                WasdiLog.errorLog("Publishband.downloadStylesStyle must not be empty");
                return "";
            }

            String sBaseUrl = WasdiConfig.Current.baseUrl;
            if (!sBaseUrl.endsWith("/")) sBaseUrl += "/";

            String sUrl = sBaseUrl + "styles/downloadbyname?style=" + sStyle;

            Map<String, String> asHeaders = HttpUtils.getStandardHeaders(sSessionId);
            return HttpUtils.downloadFile(sUrl, asHeaders, sDestinationFileFullPath);

        } catch (Exception oEx) {
        	WasdiLog.errorLog("PublishBand.downloadStyle: error", oEx);
            return "";
        }
    }

}
