package wasdi.shared.geoserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;


/**
 * Created by s.adamo on 24/05/2016.	
 */
public class Publisher {

    // Define a static logger variable so that it references the
    // Logger instance named "MyApp".
    static Logger s_oLogger = LogManager.getLogger(Publisher.class);
    
    public long m_lMaxMbTiffPyramid = 50L;

    public Publisher() {
		 try {
	         //get jar directory
	        File oCurrentFile = new File(Publisher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			String sThisFilePath = oCurrentFile.getParentFile().getPath();
			WasdiFileUtils.loadLogConfigFile(sThisFilePath);
	     } catch(Exception oEx) {
	         //no log4j configuration
	     	WasdiLog.debugLog( "Publisher: Error loading log.  Reason: " + oEx.getMessage() );
	     }
    	 
        // Set the publisher 
        try {
            m_lMaxMbTiffPyramid = Long.parseLong(WasdiConfig.Current.geoserver.maxGeotiffDimensionPyramid);
        } catch (Exception e) {
            WasdiLog.debugLog("Publisher: wrong MAX_GEOTIFF_DIMENSION_PYRAMID, setting default to 1024");
            m_lMaxMbTiffPyramid = 1024L;
        }
        
    }

    private boolean launchImagePyramidCreation(String sInputFile, String sPathName) {

        String sTargetDir = sPathName;
        if (!sTargetDir.endsWith("/")) sTargetDir += "/";
        
        Path oTargetPath = Paths.get(sTargetDir);
        if (!Files.exists(oTargetPath))
        {
            try {
                Files.createDirectory(oTargetPath);
            } catch (IOException e) {
            	WasdiLog.errorLog("Publisher.launchImagePyramidCreation: error", e);
            }
        }

        try {
            //fix permission
            WasdiFileUtils.fixUpPermissions(oTargetPath);
            
            ArrayList<String> asCmds = new ArrayList<>();
            
            String sGdalRetile = "gdal_retile.py";
            sGdalRetile = GdalUtils.adjustGdalFolder(sGdalRetile);
            
            String sConfigString = WasdiConfig.Current.geoserver.gdalRetileCommand;
            if (sConfigString.startsWith("gdal_retile.py ")) {
            	sConfigString = sConfigString.substring("gdal_retile.py ".length());
            }
            
            sGdalRetile = sGdalRetile + " " + sConfigString;
            
            String [] asGdalRetilePart = sGdalRetile.split(" ");
            if (asGdalRetilePart != null) {
            	for (String sPart : asGdalRetilePart) {
					asCmds.add(sPart);
				}
            }
            
            asCmds.add("-targetDir");
            asCmds.add(sTargetDir);
            asCmds.add(sInputFile);
            //String sCmd = String.format("%s -targetDir %s %s", sGdalRetile, sTargetDir, sInputFile);
            
            String sCmd = "";
            
            for (String sPart : asCmds) {
				sCmd += sPart + " ";
			}

            WasdiLog.debugLog("Publisher.LaunchImagePyramidCreation: Command: " + sCmd);
            
            RunTimeUtils.shellExec(asCmds, true, false, false, false);
            
        } catch (Exception e) {
            WasdiLog.errorLog("Publisher.LaunchImagePyramidCreation: Error generating pyramid image: ",  e);
            return  false;
        }

        WasdiLog.debugLog("Publisher.LaunchImagePyramidCreation:  Return true");
        return  true;
    }

    private String publishImagePyramidOnGeoServer(String sFileName, String sStoreName, String sStyle, GeoServerManager oManager) throws Exception {

        File oFile = new File(sFileName);
        String sPath = oFile.getParent();

        // Create Pyramid
        if (!launchImagePyramidCreation(sFileName, sPath)) return null;

        WasdiLog.debugLog("Publisher.PublishImagePyramidOnGeoServer: Publish Image Pyramid With Geoserver Manager");

        //publish image pyramid
        try {

            // Storage Folder
            File oSourceDir = new File(sPath);

            //Pubblico il layer
            //TODO check the epsg specification
            if (!oManager.publishImagePyramid(sStoreName, sStyle, "EPSG:4326", oSourceDir)) {
            	WasdiLog.errorLog("Publisher.PublishImagePyramidOnGeoServer: unable to publish image mosaic " + sStoreName);
            	return null;
            }
            
            WasdiLog.infoLog("Publisher.PublishImagePyramidOnGeoServer: image mosaic published " + sStoreName);

        }catch (Exception oEx) {
        	WasdiLog.errorLog("Publisher.PublishImagePyramidOnGeoServer: unable to publish image mosaic " + sStoreName, oEx);
        	return null;
        }

        return sStoreName;

    }


    public String publishGeoTiffImage(String sFileName, String sStoreName, String sEPSG, String sStyle, GeoServerManager oManager) throws Exception {
        
        String sUrl = WasdiConfig.Current.geoserver.address;
        if (!sUrl.endsWith("/")) sUrl += "/";
        sUrl += "rest/workspaces/wasdi/coveragestores/" + sStoreName + "/external.geotiff?coverageName="+sStoreName;
        
        //publish image
        try {
        	boolean bCoverageDone = false;
        	
        	if (!oManager.coverageStoreExists(sStoreName)) {
                Map<String, String> asHeaders = HttpUtils.getBasicAuthorizationHeaders(WasdiConfig.Current.geoserver.user, WasdiConfig.Current.geoserver.password);
                asHeaders.put("Content-Type", "application/json");
                asHeaders.put("Accept", "application/json");
                String sResponse = HttpUtils.httpPut(sUrl, sFileName, asHeaders);
                
                WasdiLog.debugLog("Publisher.PublishGeoTiffImage: Create Coverage URL");
                WasdiLog.debugLog(sUrl);
                
                WasdiLog.infoLog("Publisher.PublishGeoTiffImage: Create Coverage got Response Code: " + sResponse);
                
                if (!Utils.isNullOrEmpty(sResponse)) {
                	bCoverageDone = true;
                }
        		
        	}
        	else {
        		bCoverageDone = true;
        	}
            
            if (bCoverageDone) {
            	WasdiLog.infoLog("Publisher.PublishGeoTiffImage: coverage created or already existing");
            	
            	sUrl = WasdiConfig.Current.geoserver.address;
                if (!sUrl.endsWith("/")) sUrl += "/";
                sUrl += "rest/layers/wasdi:"+sStoreName;
                Map<String, String> asHeaders = HttpUtils.getBasicAuthorizationHeaders(WasdiConfig.Current.geoserver.user, WasdiConfig.Current.geoserver.password);
                asHeaders.put("Content-Type", "application/json");
                String sPayload =  "{\"layer\": {\"name\": \""+ sStoreName +"\",\"defaultStyle\": {\"name\": \""+ sStyle +"\"}}}";
                
                String sResponse = HttpUtils.httpPut(sUrl, sPayload, asHeaders);
                
                WasdiLog.debugLog("Publisher.PublishGeoTiffImage: Create Layer payload");
                WasdiLog.debugLog(sPayload);
                WasdiLog.debugLog("Publisher.PublishGeoTiffImage: Create Layer URL");
                WasdiLog.debugLog(sUrl);
                
                WasdiLog.infoLog("Publisher.PublishGeoTiffImage: Create Layer created " + sResponse);
            }
            else {
            	WasdiLog.warnLog("Publisher.PublishGeoTiffImage: impossible to find or create the Coverage Store");
            }

        } catch (Exception oEx) {
        	WasdiLog.errorLog("Publisher.PublishGeoTiffImage Exception: unable to publish geotiff " + sStoreName, oEx);
        	return null;
        }

        return sStoreName;

    }

    public String publishGeoTiff(String sFileName, String sStore, String sEPSG, String sStyle, GeoServerManager oManager) throws Exception {
        // Domain Check

        if (Utils.isNullOrEmpty(sFileName)) return  "";
        if (Utils.isNullOrEmpty(sStore)) return  "";

        File oFile = new File(sFileName);
        if (oFile.exists()==false) return "";

        long lFileLenght = oFile.length();
        long lMaxSize = m_lMaxMbTiffPyramid*1024L*1024L;

        // More than Gb => Pyramid, otherwise normal geotiff
        if (lFileLenght> lMaxSize) return this.publishImagePyramidOnGeoServer(sFileName, sStore, sStyle, oManager);
        else  return this.publishGeoTiffImage(sFileName, sStore, sEPSG, sStyle, oManager);
    }
    
    /**
     * Publish a Shape file in geoserver
     * @param sFileName Full Name and Path of the .shp 
     * @param asShapeFiles List of the different Full name and Path of the shape (all same names, different extension)
     * @param sStore name of the store and layer to create 
     * @param sEPSG Projection
     * @param sStyle Style 
     * @param oManager Geoserver Manager
     * @return Name of the created store/layer if ok, null otherwise
     * @throws Exception
     */
    public String publishShapeFile(String sFileName, ArrayList<String> asShapeFiles, String sStore, String sEPSG, String sStyle, GeoServerManager oManager) throws Exception {

        // Domain Check
        if (Utils.isNullOrEmpty(sFileName)) return  "";
        if (Utils.isNullOrEmpty(sStore)) return  "";
        if (asShapeFiles == null) return "";
        
        String sBaseName = WasdiFileUtils.getFileNameWithoutLastExtension(sFileName);
        
        ArrayList<String> asRenamedShapeFiles = new ArrayList<>();
        
        for (String sShapeFile : asShapeFiles) {
        	
        	String sExtension = WasdiFileUtils.getFileNameExtension(sShapeFile);
        	String sNewName = sShapeFile.replace(sBaseName+"."+sExtension, sStore+"."+sExtension);
        	File oNewFile = new File(sNewName);
        	
        	WasdiFileUtils.renameFile(sShapeFile, oNewFile.getName());
        	asRenamedShapeFiles.add(sNewName);
		}
        
        String sExtension = WasdiFileUtils.getFileNameExtension(sFileName);
        sFileName = sFileName.replace(sBaseName+"."+sExtension, sStore+"."+sExtension);
        
        String sZipFile = sFileName.replace(".shp", ".zip");
        sZipFile = sZipFile.replace(".SHP", ".zip");

        File oZippedShapeFile = new File(sZipFile);
        
        ZipFileUtils.zipFiles(asRenamedShapeFiles, oZippedShapeFile.getPath());
        
        //Publish Shape File
        try {
            if (!oManager.publishShapeFile(sStore, oZippedShapeFile, sEPSG, sStyle, s_oLogger)) {
            	WasdiLog.errorLog("Publisher.publishShapeFile: unable to publish shapefile " + sStore);
            	return null;
            }
            WasdiLog.infoLog("Publisher.publishShapeFile: shapefile published " + sStore);

        } catch (Exception oEx) {
        	WasdiLog.errorLog("Publisher.publishShapeFile Exception: unable to publish shapefile " + sStore, oEx);
        	return null;
        }

        return sStore;
    }

    
    /**
     * Publish a Shape file in geoserver
     * @param sFileName Full Name and Path of the .shp 
     * @param asShapeFiles List of the different Full name and Path of the shape (all same names, different extension)
     * @param sStore name of the store and layer to create 
     * @param sEPSG Projection
     * @param sStyle Style 
     * @param oManager Geoserver Manager
     * @return Name of the created store/layer if ok, null otherwise
     * @throws Exception
     */
    public String publishGeoPackageFile(String sFileName, String sLayerId, String sBandName, String sStyle, GeoServerManager oManager) throws Exception {

        // Domain Check
        if (Utils.isNullOrEmpty(sFileName)) return  "";
        
        //Publish GeoPackage File
        try {
            if (!oManager.publishGeoPackageFile(sLayerId, new File(sFileName), sBandName, sStyle)) {
            	WasdiLog.errorLog("Publisher.publishGeoPackageFile: unable to publish  " + sFileName);
            	return null;
            }
            WasdiLog.infoLog("Publisher.publishGeoPackageFile: GeoPackage File published " + sFileName);

        } 
        catch (Exception oEx) {
        	WasdiLog.errorLog("Publisher.publishGeoPackageFile Exception: unable to publish " + sFileName, oEx);
        	return null;
        }

        return sBandName;
    }
    
}
