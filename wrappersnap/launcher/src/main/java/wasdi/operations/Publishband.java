package wasdi.operations;

import java.awt.Dimension;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.geotools.referencing.CRS;

import wasdi.LauncherMain;
import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.io.WasdiProductWriter;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.geoserver.Publisher;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.payloads.PublishBandPayload;
import wasdi.shared.utils.BandImageManager;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.products.PublishBandResultViewModel;

public class Publishband extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		m_oLocalLogger.debug("Publishband.executeOperation");
		
        String sLayerId = "";
        
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

        try {
        	
        	PublishBandParameter oParameter = (PublishBandParameter) oParam;

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 0);
            }

            // Read File Name
            String sFile = oParameter.getFileName();
            
            // Check integrity
            if (Utils.isNullOrEmpty(sFile)) {
                // File not good!!
                m_oLocalLogger.debug("Publishband.executeOperation: file is null or empty");
                String sError = "Input File path is null";

                m_oProcessWorkspaceLogger.log("Input file is null...");

                // Send KO to Rabbit
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());

                return false;
            }            

            // Generate full path name
            sFile = LauncherMain.getWorkspacePath(oParameter) + sFile;

            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sFile);

            if (oDownloadedFile == null) {
                m_oLocalLogger.error("Publishband.executeOperation: Downloaded file is null!! Return empyt layer id for [" + sFile + "]");
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), "Cannot find product to publish", oParameter.getExchange());
                return false;
            }

            // Get the product name
            String sProductName = oDownloadedFile.getProductViewModel().getName();

            m_oProcessWorkspaceLogger.log("Publish Band " + sProductName + " - " + oParameter.getBandName());
            m_oLocalLogger.debug("Publishband.executeOperation:  File = " + sFile);

            // Create file object
            File oFile = new File(sFile);
            String sInputFileNameOnly = oFile.getName();

            // set file size
            setFileSizeToProcess(oFile, oProcessWorkspace);

            // Generate Layer Id
            sLayerId = sInputFileNameOnly;
            sLayerId = Utils.getFileNameWithoutLastExtension(sFile);
            sLayerId += "_" + oParameter.getBandName();

            // Is already published?
            PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
            PublishedBand oAlreadyPublished = oPublishedBandsRepository.getPublishedBand(sFile, oParameter.getBandName());

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 10);

            if (oAlreadyPublished != null) {
                // Yes !!
                m_oLocalLogger.debug("Publishband.executeOperation:  Band already published. Return result");

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
            String sStyle = "raster";
            
            sStyle = getStyleByFileName(sFile);
            
            if (Utils.isNullOrEmpty(oParameter.getStyle()) == false) {
                sStyle = oParameter.getStyle();
            }

            m_oProcessWorkspaceLogger.log("Using style " + sStyle);

            m_oLocalLogger.debug("Publishband.executeOperation:  Generating Band Image...");

            m_oProcessWorkspaceLogger.log("Generate Band Image");

            // Read the product
			WasdiProductReader oReadProduct = WasdiProductReaderFactory.getProductReader(oFile);
			
			Product oProduct = oReadProduct.getSnapProduct();
			
			String sEPSG = "EPSG:4326";

            if (oProduct == null) {
            	
            	boolean bContinue = false;
            	
    			if (sInputFileNameOnly.toUpperCase().startsWith("S5P")) {
    				
    				if (convertSentinel5PtoGeotiff(oFile.getAbsolutePath(), oParameter.getBandName() + ".tif", oParameter.getBandName())) {
    					String sNewPath = oFile.getParentFile().getPath();
    					if (!sNewPath.endsWith("/")) sNewPath += "/";
    					sNewPath += oParameter.getBandName() + ".tif";
    					sFile = sNewPath;
    					oFile = new File(sFile);
    					
    					bContinue = true;
    				}
    				
    			}
				
    			if (!bContinue) {
                    // TODO: HERE CHECK IF IT IS A SHAPE FILE!!!!!					
    				m_oProcessWorkspaceLogger.log("Impossible to read the input file sorry");
    				m_oLocalLogger.error("Publishband.executeOperation: Not a SNAP Product Return empyt layer id for [" + sFile + "]");
    				return false;    				
    			}
			}
			else {
				sEPSG = CRS.lookupIdentifier(oProduct.getSceneCRS(), true);
			}

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 20);

            // write the data directly to GeoServer Data Dir
            String sGeoServerDataDir = WasdiConfig.Current.paths.geoserverDataDir;
            String sTargetDir = sGeoServerDataDir;

            if (!(sTargetDir.endsWith("/") || sTargetDir.endsWith("\\"))) sTargetDir += "/";
            sTargetDir += sLayerId + "/";

            File oTargetDir = new File(sTargetDir);
            if (!oTargetDir.exists())
                oTargetDir.mkdirs();

            // Output file Path
            String sOutputFilePath = sTargetDir + sLayerId + ".tif";

            // Output File
            File oOutputFile = new File(sOutputFilePath);

            m_oLocalLogger.debug("Publishband.executeOperation: to " + sOutputFilePath + " [LayerId] = " + sLayerId);

            // Check if is already a .tif image
            if ((sFile.toLowerCase().endsWith(".tif") || sFile.toLowerCase().endsWith(".tiff")) == false) {

                // Check if it is a S2
                if (oProduct.getProductType().startsWith("S2")
                        && oProduct.getProductReader().getClass().getName().startsWith("org.esa.s2tbx")) {

                    m_oLocalLogger.debug("Publishband.executeOperation:  Managing S2 Product");
                    m_oLocalLogger.debug("Publishband.executeOperation:  Getting Band " + oParameter.getBandName());

                    Band oBand = oProduct.getBand(oParameter.getBandName());
                    Product oGeotiffProduct = new Product(oParameter.getBandName(), "GEOTIFF");
                    oGeotiffProduct.addBand(oBand);
                    sOutputFilePath = new WasdiProductWriter(m_oProcessWorkspaceRepository, oProcessWorkspace)
                            .WriteGeoTiff(oGeotiffProduct, sTargetDir, sLayerId);
                    oOutputFile = new File(sOutputFilePath);
                    m_oLocalLogger.debug("Publishband.executeOperation:  Geotiff File Created (EPSG=" + sEPSG + "): "
                            + sOutputFilePath);

                } else {

                    m_oLocalLogger.debug("Publishband.executeOperation:  Managing NON S2 Product");
                    m_oLocalLogger.debug("Publishband.executeOperation:  Getting Band " + oParameter.getBandName());

                    // Get the Band
                    Band oBand = oProduct.getBand(oParameter.getBandName());
                    // Get Image
                    // MultiLevelImage oBandImage = oBand.getSourceImage();
                    RenderedImage oBandImage = oBand.getSourceImage();

                    // Check if the Colour Model is present
                    ColorModel oColorModel = oBandImage.getColorModel();

                    // Tested for Copernicus Marine - netcdf files
                    if (oColorModel == null) {

                        // Colour Model not present: try a different way to get the Image
                        BandImageManager oImgManager = new BandImageManager(oProduct);

                        // Create full dimension and View port
                        Dimension oOutputImageSize = new Dimension(oBand.getRasterWidth(), oBand.getRasterHeight());

                        // Render the image
                        oBandImage = oImgManager.buildImageWithMasks(oBand, oOutputImageSize, null, false, true);
                    }

                    // Get TIFF Metadata
                    GeoTIFFMetadata oMetadata = ProductUtils.createGeoTIFFMetadata(oProduct);

                    m_oLocalLogger.debug("Publishband.executeOperation:  Output file: " + sOutputFilePath);
                    
                    GeoTIFF.writeImage(oBandImage, oOutputFile, oMetadata);
                }
            } else {
                // This is a geotiff, just copy
                FileUtils.copyFile(oFile, oOutputFile);
            }

            m_oProcessWorkspaceLogger.log("Publish on geoserver");

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 50);

            // Ok publish
            GeoServerManager oGeoServerManager = new GeoServerManager();

            // Do we have the style in this Geoserver?
            if (!oGeoServerManager.styleExists(sStyle)) {

                // Not yet: obtain styles root path
                String sStylePath = WasdiConfig.Current.paths.downloadRootPath;
                if (!sStylePath.endsWith(File.separator)) sStylePath += File.separator;
                sStylePath += "styles" + File.separator;

                // Set the style file
                sStylePath += sStyle + ".sld";

                File oStyleFile = new File(sStylePath);

                // Do we have the file?
                if (!oStyleFile.exists()) {
                    // No, Download style
                    m_oLocalLogger.info("Publishband.executeOperation: download style " + sStyle + " from main node");
                    String sRet = downloadStyle(sStyle, oParameter.getSessionID(), sStylePath);

                    // Check download result
                    if (!sRet.equals(sStylePath)) {
                        // Not good...
                        m_oLocalLogger.error("Publishband.executeOperation: error downloading style " + sStyle);
                    }
                }

                // Publish the style
                if (oGeoServerManager.publishStyle(sStylePath)) {
                    m_oLocalLogger.info("Publishband.executeOperation: published style " + sStyle + " on local geoserver");
                } else {
                    m_oLocalLogger.error("Publishband.executeOperation: error publishing style " + sStyle + " reset on raster");
                    sStyle = "raster";
                }
            }

            Publisher oPublisher = new Publisher();

            try {
                oPublisher.m_lMaxMbTiffPyramid = Long.parseLong(WasdiConfig.Current.geoserver.maxGeotiffDimensionPyramid);
            } catch (Exception e) {
                m_oLocalLogger.error("Publishband.executeOperation: wrong MAX_GEOTIFF_DIMENSION_PYRAMID, setting default to 1024");
                oPublisher.m_lMaxMbTiffPyramid = 1024L;
            }

            m_oLocalLogger.debug("Publishband.executeOperation: Call publish geotiff sOutputFilePath = " + sOutputFilePath + " , sLayerId = " + sLayerId + " Style = " + sStyle);
            sLayerId = oPublisher.publishGeoTiff(sOutputFilePath, sLayerId, sEPSG, sStyle, oGeoServerManager);

            m_oLocalLogger.debug("Publishband.executeOperation: Obtained sLayerId = " + sLayerId);

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 90);

            boolean bResultPublishBand = true;

            if (sLayerId == null) {
                m_oProcessWorkspaceLogger.log("Error publishing in Geoserver... :(");
                bResultPublishBand = false;
                m_oLocalLogger.debug("Publishband.executeOperation: Image not published . ");
                throw new Exception("Layer Id is null. Image not published");
            } else {

                m_oProcessWorkspaceLogger.log("Ok got layer id " + sLayerId);

                m_oLocalLogger.debug("Publishband.executeOperation: Image published.");

                // get bounding box from data base
                String sBBox = oDownloadedFile.getBoundingBox();
                String sGeoserverBBox = oGeoServerManager.getLayerBBox(sLayerId);

                m_oLocalLogger.debug("Publishband.executeOperation: Bounding Box: " + sBBox);
                m_oLocalLogger.debug("Publishband.executeOperation: Geoserver Bounding Box: " + sGeoserverBBox + " for Layer Id " + sLayerId);

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
                    m_oLocalLogger.debug("Publishband.executeOperation: node code: " + oNode.getNodeCode());
                    oPublishedBand.setGeoserverUrl(oNode.getNodeGeoserverAddress());
                }

                // Add it the the db
                oPublishedBandsRepository.insertPublishedBand(oPublishedBand);

                m_oLocalLogger.debug("Publishband.executeOperation: Index Updated");

                // Create the View Model
                PublishBandResultViewModel oVM = new PublishBandResultViewModel();
                oVM.setBandName(oParameter.getBandName());
                oVM.setProductName(sProductName);
                oVM.setLayerId(sLayerId);
                oVM.setBoundingBox(sBBox);
                oVM.setGeoserverBoundingBox(sGeoserverBBox);
                oVM.setGeoserverUrl(oPublishedBand.getGeoserverUrl());

                // P.Campanella 2019/05/02: Wait a little bit to make GeoServer "finish" the
                // process
                Thread.sleep(5000);

                m_oSendToRabbit.SendRabbitMessage(bResultPublishBand, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), oVM, oParameter.getExchange());
                
                m_oProcessWorkspaceLogger.log("Band published " + new EndMessageProvider().getGood());

                PublishBandPayload oPayload = new PublishBandPayload();

                oPayload.setBand(oParameter.getBandName());
                oPayload.setProduct(sProductName);
                oPayload.setLayerId(sLayerId);
                
                setPayload(oProcessWorkspace, oPayload);

                oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
            }
            
            return true;
            
        } catch (Exception oEx) {

            m_oProcessWorkspaceLogger.log("Exception");

            m_oLocalLogger.error("Publishband.executeOperation: Exception " + oEx.toString() + " " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);

            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.PUBLISHBAND.name(), oParam.getWorkspace(), sError, oParam.getExchange());            
        } 
        finally {
            BandImageManager.stopCacheThread();
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
        
        return sStyle;
	}
    
	protected boolean convertSentinel5PtoGeotiff(String sInputFile, String sOutputFile, String sBand) {
		try {
			
			if (Utils.isNullOrEmpty(sInputFile)) return false;
			if (Utils.isNullOrEmpty(sOutputFile)) return false;
			if (Utils.isNullOrEmpty(sBand)) return false;
			
			String sInputPath = "";
			File oFile = new File(sInputFile);
			sInputPath = oFile.getParentFile().getPath();
			if (!sInputPath.endsWith("/")) sInputPath += "/";
			
			String sGdalCommand = "gdal_translate";
			sGdalCommand = LauncherMain.adjustGdalFolder(sGdalCommand);
			
			ArrayList<String> asArgs = new ArrayList<String>();
			asArgs.add(sGdalCommand);
			
			asArgs.add("-co");
			asArgs.add("WRITE_BOTTOMUP=NO");
			
			asArgs.add("-of");
			asArgs.add("VRT");
			
			String sGdalInput = "NETCDF:\""+sInputFile+"\":/PRODUCT/"+sBand;
			
			asArgs.add(sGdalInput);
			asArgs.add(sInputPath + sBand + ".vrt");

			// Execute the process
			ProcessBuilder oProcessBuidler = new ProcessBuilder(asArgs.toArray(new String[0]));
			Process oProcess;
		
//			String sCommand = "";
//			for (String sArg : asArgs) {
//				sCommand += sArg + " ";
//			}
			
			oProcessBuidler.redirectErrorStream(true);
			oProcess = oProcessBuidler.start();
			
			BufferedReader oReader = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
			String sLine;
			while ((sLine = oReader.readLine()) != null)
				m_oLocalLogger.debug("Publishband.convertS5PtoGeotiff [gdal]: " + sLine);
			
			oProcess.waitFor();			
			
			asArgs = new ArrayList<String>();
			sGdalCommand = "gdalwarp";
			sGdalCommand = LauncherMain.adjustGdalFolder(sGdalCommand);
			
			asArgs.add(sGdalCommand);
			asArgs.add("-geoloc");
			asArgs.add("-t_srs");
			asArgs.add("EPSG:4326");
			asArgs.add("-overwrite");
			asArgs.add(sInputPath + sBand+ ".vrt");
			asArgs.add(sInputPath + sOutputFile);
			
			oProcessBuidler = new ProcessBuilder(asArgs.toArray(new String[0]));
		
//			sCommand = "";
//			for (String sArg : asArgs) {
//				sCommand += sArg + " ";
//			}
			
			oProcessBuidler.redirectErrorStream(true);
			oProcess = oProcessBuidler.start();
			
			oReader = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
			while ((sLine = oReader.readLine()) != null)
				m_oLocalLogger.debug("Publishband.convertSentine5PtoGeotiff [gdal]: " + sLine);
			
			oProcess.waitFor();
			
			if (new File(sInputPath + sOutputFile).exists()) return true;
			else return false;
		}
		catch (Exception oEx) {
			
			m_oLocalLogger.debug("Publishband.convertSentinel5PtoGeotiff: Exception = " + oEx.toString());
			
			return false;
		}
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
                m_oLocalLogger.error("Publishband.downloadStyle: sStyle must not be null");
                return "";
            }

            if (sStyle.equals("")) {
                m_oLocalLogger.error("Publishband.downloadStylesStyle must not be empty");
                return "";
            }

            String sBaseUrl = WasdiConfig.Current.baseUrl;

            String sUrl = sBaseUrl + "/filebuffer/downloadstyle?style=" + sStyle;

            Map<String, String> asHeaders = HttpUtils.getStandardHeaders(sSessionId);
            return HttpUtils.downloadFile(sUrl, asHeaders, sDestinationFileFullPath);

        } catch (Exception oEx) {
            oEx.printStackTrace();
            return "";
        }
    }

}
