package wasdi.shared.geoserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Logger;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.Purge;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.HTTPUtils;
import it.geosolutions.geoserver.rest.decoder.RESTBoundingBox;
import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.decoder.RESTLayer.Type;
import it.geosolutions.geoserver.rest.decoder.RESTLayerList;
import it.geosolutions.geoserver.rest.decoder.RESTResource;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSImageMosaicEncoder;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * GeoServerManager: utility class to add layers to geoserver
 * that can be later shown as WxS
 * 
 * Created by s.adamo on 24/05/2016.
 * Refactoring by p.campanella on 01/03/2022.
 */
public class GeoServerManager {
	
	/**
	 * Geoserver Workspace 
	 */
	private final String m_sWorkspace = "wasdi";
	
	/**
	 * URL of the GeoServer API 
	 */
    String m_sRestUrl;
    /**
     * Geoserver User 
     */
    String m_sRestUser;
    /**
     * Geoserver Password
     */
    String m_sRestPassword;
    
    /**
     * Gesolutions REST Publisher: utility class that already implements many REST API of Geoserver.
     * Since it is not updated, we cannot fully rely on this. But still we use it for what works.
     */
    GeoServerRESTPublisher m_oGsPublisher;
    
    /**
     * Gesolutions REST Publisher: utility class that already implements many REST API of Geoserver.
     * Since it is not updated, we cannot fully rely on this. But still we use it for what works. 
     */
    GeoServerRESTReader m_oGsReader;
    
    /**
     * Initializes the manager
     * @param sRestUrl API Url
     * @param sUser User 
     * @param sPassword Password
     * @throws MalformedURLException
     */
    public GeoServerManager(String sRestUrl, String sUser, String sPassword) throws MalformedURLException {
        m_sRestUrl = sRestUrl;
        m_sRestUser = sUser;
        m_sRestPassword = sPassword;
        m_oGsReader = new GeoServerRESTReader(m_sRestUrl, m_sRestUser, m_sRestPassword);
        m_oGsPublisher = new GeoServerRESTPublisher(m_sRestUrl, m_sRestUser, m_sRestPassword);
        
        // We check and create our workspace if needed
    	if (!m_oGsReader.existsWorkspace(m_sWorkspace)) {
    		m_oGsPublisher.createWorkspace(m_sWorkspace);
    	}
    }
    
    /**
     * Automatic constructor that uses WasdiConfig to initialize
     * @throws MalformedURLException
     */
    public GeoServerManager() throws MalformedURLException {
    	this(WasdiConfig.Current.geoserver.address, WasdiConfig.Current.geoserver.user, WasdiConfig.Current.geoserver.password);
    }
    
    /**
     * Get the bbox as detected by Geoserver as a String
     * @param sLayerId Layer id to query
     * @return bbox string in format "{\"miny\":%f,\"minx\":%f,\"crs\":\"%s\",\"maxy\":%f,\"maxx\":%f}"
     */
    public String getLayerBBox(String sLayerId) {
    	
    	try {
        	RESTBoundingBox oBbox = getLayerRESTBBox(sLayerId);
        	
        	if (oBbox != null) {
            	String sRet = String.format("{\"miny\":%f,\"minx\":%f,\"crs\":\"%s\",\"maxy\":%f,\"maxx\":%f}", 
            			oBbox.getMinY(), oBbox.getMinX(), oBbox.getCRS().replace("\"", "\\\\\\\""), oBbox.getMaxY(), oBbox.getMaxX());
            	
            	return sRet;        		
        	}
        	else {
        		return "";
        	}
        	
    	}
    	catch (Exception oEx) {
    		String sError = ExceptionUtils.getMessage(oEx);
    		WasdiLog.debugLog("GeoServerManager.getLayerBBox: ERROR " + sError);
    		return "";
		}
    }
    
    public List<String> getLayers() {
     	try {
     		RESTLayerList aoList = m_oGsReader.getLayers();
     		List<String> asNames = aoList.getNames();
          	return asNames;    		
    	}
    	catch (Exception oEx) {
    		String sError = ExceptionUtils.getMessage(oEx);
    		WasdiLog.debugLog("GeoServerManager.getLayers: ERROR " + sError);
    		return null;
		}        	 
    }
    
    /**
     * Get the bbox as detected by Geoserver as a RESTBoundingBox object
     * @param sLayerId Layer id to query
     * @return bbox string in format "{\"miny\":%f,\"minx\":%f,\"crs\":\"%s\",\"maxy\":%f,\"maxx\":%f}"
     */    
    public RESTBoundingBox getLayerRESTBBox(String sLayerId) {
    	try {
        	RESTLayer oLayer = m_oGsReader.getLayer(m_sWorkspace, sLayerId);
        	RESTResource oRes = m_oGsReader.getResource(oLayer);
        	RESTBoundingBox oBbox = oRes.getLatLonBoundingBox();
          	return oBbox;    		
    	}
    	catch (Exception oEx) {
    		String sError = ExceptionUtils.getMessage(oEx);
    		WasdiLog.debugLog("GeoServerManager.getLayerBBox: ERROR " + sError);
    		return null;
		}    	
    }
    
    /**
     * Get the name of the default style of a layer
     * @param sLayerId Geoserver Layer Id
     * @return
     */
    public String getLayerStyle(String sLayerId) {
    	try {
        	RESTLayer oLayer = m_oGsReader.getLayer(m_sWorkspace, sLayerId);
        	
          	return oLayer.getDefaultStyle();    		
    	}
    	catch (Exception oEx) {
    		String sError = ExceptionUtils.getMessage(oEx);
    		WasdiLog.debugLog("GeoServerManager.getLayerStyle: ERROR " + sError);
    		return null;
		}    	
    }

    /**
     * Removes an existing layer and relative data store
     * @param sLayerId layer id to remove
     * @return
     */
    public boolean removeLayer(String sLayerId) {

    	RESTLayer oLayer = m_oGsReader.getLayer(m_sWorkspace, sLayerId);
    	
    	if (oLayer == null) return false;
    	
    	Type oLayerType = oLayer.getType();    	
    	RESTResource oRes = m_oGsReader.getResource(oLayer);
    	
    	String sStoreName = oRes.getStoreName();
    	String[] asToks = sStoreName.split(":");
    	if (asToks.length>1) sStoreName = asToks[1];
    	
		switch (oLayerType) {
		case VECTOR:
			return m_oGsPublisher.removeDatastore(m_sWorkspace, sStoreName, true, Purge.ALL);			
		case RASTER:
			return m_oGsPublisher.removeCoverageStore(m_sWorkspace, sStoreName, true, Purge.ALL);
		default:
			WasdiLog.debugLog("GeoServerManager.removeLayer: unknown layer type for " + sLayerId);
			break;
		}

    	return false;
    }
    
    
    /**
     * Checks if a layer exists
     * @param sLayerId layer id to check
     * @return true if exists, false if it does not exists
     */
    public boolean layerExists(String sLayerId) {

    	RESTLayer oLayer = m_oGsReader.getLayer(m_sWorkspace, sLayerId);
    	
    	
    	if (oLayer == null) return false;
    	return true;
    }

    
    /**
     * Checks if a coverage store exists
     * @param sLayerId layer id to check
     * @return true if exists, false if it does not exists
     */
    public boolean coverageStoreExists(String sCoverageId) {

    	RESTCoverageStore oCoverageStore = m_oGsReader.getCoverageStore(m_sWorkspace, sCoverageId);
    	
    	if (oCoverageStore == null) return false;
    	return true;
    }

    
    /**
     * Publish a raster that had Pyramidization
     * @param sStoreName Name of the store and layer
     * @param sStyle Style
     * @param sEpsg Projection 
     * @param oBaseDir Pyramid base dir
     * @return
     * @throws FileNotFoundException
     */
    public boolean publishImagePyramid(String sStoreName, String sStyle, String sEpsg, File oBaseDir)
            throws FileNotFoundException {
    	
    	RESTLayer oLayer = m_oGsReader.getLayer(m_sWorkspace, sStoreName);
    	if (oLayer != null) removeLayer(sStoreName);
    	
    	//layer encoder
    	final GSLayerEncoder oLayerEnc = new GSLayerEncoder();
    	if (sStyle==null || sStyle.isEmpty()) sStyle="raster";
    	oLayerEnc.setDefaultStyle(sStyle);
    	
    	//coverage encoder
    	final GSImageMosaicEncoder oCoverageEnc=new GSImageMosaicEncoder();
    	oCoverageEnc.setName(sStoreName);
    	oCoverageEnc.setTitle(sStoreName);
    	if (sEpsg!=null) oCoverageEnc.setSRS(sEpsg);
    	oCoverageEnc.setMaxAllowedTiles(Integer.MAX_VALUE); 
    	
    	//publish
    	boolean bRes = m_oGsPublisher.publishExternalMosaic(m_sWorkspace, sStoreName, oBaseDir, oCoverageEnc, oLayerEnc);
    	
    	//configure coverage
        if (bRes && m_oGsReader.existsCoveragestore(m_sWorkspace, sStoreName) && m_oGsReader.existsCoverage(m_sWorkspace, sStoreName, sStoreName)) {
        	GSCoverageEncoder oCe = new GSCoverageEncoder();
            oCe.setEnabled(true); //abilito il coverage
            oCe.setSRS(sEpsg);
        	m_oGsPublisher.configureCoverage(oCe, m_sWorkspace, sStoreName);
        }

    	
		return bRes;
    	
    }
    
    /**
     * Publish a standard geotiff
     * @param sStoreName Name of store and layer
     * @param oGeotiffFile Geotiff file to publish
     * @param sEpsg Projection
     * @param sStyle Style to apply
     * @param oLogger Logger
     * @return
     * @throws FileNotFoundException
     */
    public boolean publishStandardGeoTiff(String sStoreName, File oGeotiffFile, String sEpsg, String sStyle, Logger oLogger)
            throws FileNotFoundException {
    	
    	RESTLayer oLayer = m_oGsReader.getLayer(m_sWorkspace, sStoreName);
    	if (oLayer != null) removeLayer(sStoreName);

    	if (sStyle == null || sStyle.isEmpty()) sStyle = "raster";
    	    	
    	if (sStoreName == null) {
    		oLogger.error("GeoServerManager.publishStandardGeoTiff: Store Name is null");
    	}
    	
    	if (oGeotiffFile == null) {
    		oLogger.error("GeoServerManager.publishStandardGeoTiff: oGeoTiffFile is null");
    	}
        
    	if (sEpsg == null) {
    		oLogger.error("GeoServerManager.publishStandardGeoTiff: sEpsg is null");
    	}
    	
        boolean bRes = m_oGsPublisher.publishExternalGeoTIFF(m_sWorkspace,sStoreName,oGeotiffFile, sStoreName, sEpsg, GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, sStyle);
        boolean bExistsCoverageStore = m_oGsReader.existsCoveragestore(m_sWorkspace, sStoreName);
        boolean bExistsCoverage= m_oGsReader.existsCoverage(m_sWorkspace, sStoreName, sStoreName);
                
        if (bRes && bExistsCoverageStore && bExistsCoverage) {
        	GSCoverageEncoder oCe = new GSCoverageEncoder();
            oCe.setEnabled(true); //abilito il coverage
            oCe.setSRS(sEpsg);
        	m_oGsPublisher.configureCoverage(oCe, m_sWorkspace, sStoreName);
        }
        
		return bRes;
    }
    
    /**
     * Adds a new style to geoserver
     * @param sStyleFile Path of the sld file
     * @return
     */
    public boolean publishStyle(String sStyleFile) {
    	File oFile = new File(sStyleFile);
    	
    	if (oFile.exists()) {
    		String sStyleName = WasdiFileUtils.getFileNameWithoutLastExtension(oFile.getName());
    		return m_oGsPublisher.publishStyle(oFile, sStyleName);
    	}
    	else {
    		return false;
    	}
    }
    
    /**
     * Removes a style to geoserver
     * @param sStyleFile Path of the sld file
     * @return
     */
    public boolean removeStyle(String sStyleName) {
    	return m_oGsPublisher.removeStyle(sStyleName);
    }
    
    /**
     * Updates a already published SLD style
     * @param sStyleFile
     * @return
     */
    public boolean updateStyle(String sName, String sStyleFile) {
    	
    	try {
    		String sBody = WasdiFileUtils.fileToText(sStyleFile);
    		
    		String sUrl = m_sRestUrl + "/rest/styles/" + sName + "?raw=true";
    		final String sResult = HTTPUtils.put(sUrl, sBody, "application/vnd.ogc.sld+xml", m_sRestUser, m_sRestPassword);
    		return sResult != null;    		
    	}
    	catch (Exception oEx) {
			WasdiLog.errorLog("GeoServerManager.updateStyle: exception ", oEx);
		}
    	
    	return false;
    }
    
    /**
     * Check if a style exists
     * @param sStyle
     * @return
     */
    public boolean styleExists(String sStyle) {
    	String sStyles = HTTPUtils.get(m_sRestUrl+"/rest/styles", m_sRestUser, m_sRestPassword);
    	
    	if (Utils.isNullOrEmpty(sStyles) == false) {
    		String sResearchKey = "\"name\":\"" + sStyle + "\"";
    		
    		if (sStyles.contains(sResearchKey)) return true;
    	}
    	
    	return false;
    }
    
	/**
	 * Add a shape file layer
	 * @param sStoreName layer and store name
	 * @param oShpFile Shape File Zipped
	 * @param sEpsg Projection
	 * @param sStyle Style
	 * @param oLogger Logger
	 * @throws DataException 
	 */
    public boolean publishShapeFile(String sStoreName, File oShpFile, String sEpsg, String sStyle, Logger oLogger) throws Exception {    	
		
    	// If the layer is there we clean it
    	RESTLayer oLayer = m_oGsReader.getLayer(m_sWorkspace, sStoreName);
    	if (oLayer != null) removeLayer(sStoreName);
    	
    	// Default style for shape is polygon
    	if (sStyle == null || sStyle.isEmpty()) sStyle = "polygon";
    	
    	
    	if (sStoreName == null) {
    		oLogger.error("GeoServerManager.publishShapeFile: Store Name is null");
    		return false;
    	}
    	
    	if (oShpFile == null) {
    		oLogger.error("GeoServerManager.publishShapeFile: oGeoTiffFile is null");
    		return false;
    	}
        
    	if (sEpsg == null) {
    		oLogger.error("GeoServerManager.publishShapeFile: sEpsg is null");
    		return false;
    	}
    	
    	boolean bRes = createShapeDataStore(oShpFile.getPath(), sStoreName);
    	
    	if (bRes) {
        	if (!configureLayerStyle(sStoreName, sStyle)) {
        		oLogger.error("GeoServerManager.publishShapeFile: there was an error configuring the style");
        	}    		
    	}
    	
		return bRes;
	}
    
    /**
     * Creates a datastore for shape files
     * @param sShapeFilePath
     * @param sStoreName
     * @return
     */
    private boolean createShapeDataStore(String sShapeFilePath, String sStoreName)  {
    	
        StringBuilder sUrl = new StringBuilder(m_sRestUrl).append("/rest/workspaces/")
                .append(m_sWorkspace).append("/datastores/").append(sStoreName).append("/file.shp");//?charset=ISO-8859-1
        
        final File oFile = new File(sShapeFilePath);
        
        String sResult = HTTPUtils.put(sUrl.toString(), oFile, "application/zip", m_sRestUser, m_sRestPassword);
        
        if (sResult != null) {
        	WasdiLog.debugLog(sResult);
        } 

        return sResult != null;
    }        
    
    
	/**
	 * Add a shape file layer
	 * @param sStoreName layer and store name
	 * @param oGeoPackageFile Shape File Zipped
	 * @param sEpsg Projection
	 * @param sStyle Style
	 * @param oLogger Logger
	 * @throws DataException 
	 */
    public boolean publishGeoPackageFile(String sStoreName, File oGeoPackageFile, String sBandName, String sStyle) throws Exception {    	
		
    	// If the layer is there we clean it
    	RESTLayer oLayer = m_oGsReader.getLayer(m_sWorkspace, sStoreName);
    	
    	if (oLayer != null) {
    		WasdiLog.debugLog("GeoServerManager.publishGeoPackageFile: found layer " + sStoreName + " deleting it");
    		removeLayer(sStoreName);
    	}
    	
    	// Default style for geopackages is polygon
    	if (sStyle == null || sStyle.isEmpty()) sStyle = "polygon";
    	
    	
    	if (sStoreName == null) {
    		WasdiLog.errorLog("GeoServerManager.publishGeoPackageFile: Store Name is null");
    		return false;
    	}
    	
    	if (oGeoPackageFile == null) {
    		WasdiLog.errorLog("GeoServerManager.publishGeoPackageFile: oGeoTiffFile is null");
    		return false;
    	}
    	
    	WasdiLog.debugLog("GeoServerManager.publishGeoPackageFile: calling create geo package store for " + sStoreName + " Band: " + sBandName);
    	
    	boolean bRes = createGeoPckgDataStore(oGeoPackageFile.getPath(), sStoreName);
    	
    	if (bRes) {
    		
    		WasdiLog.debugLog("GeoServerManager.publishGeoPackageFile: data store created, going to publish the layer");
    		
    		String sUrl = m_sRestUrl + "/rest/workspaces/" + m_sWorkspace + "/datastores/" + sStoreName + "/featuretypes";

	    	String sXml = "<featureType>" + "  <name>" + sBandName + "</name>" + "</featureType>";

	    	WasdiLog.debugLog("GeoServerManager.publishGeoPackageFile: publishing the layer with url " + sUrl);
	    	
	    	String sResult = HTTPUtils.post(sUrl, sXml, "application/xml", m_sRestUser, m_sRestPassword);
	    	
	    	if (sResult!=null) {
	    		WasdiLog.debugLog("GeoServerManager.publishGeoPackageFile: got response [" + sResult + "], now configure the style");
	        	if (!configureLayerStyle(sBandName, sStyle)) {
	        		WasdiLog.errorLog("GeoServerManager.publishGeoPackageFile: there was an error configuring the style");
	        	}    			    		
	    	}
	    	else {
	    		WasdiLog.warnLog("GeoServerManager.publishGeoPackageFile: got a Null response, there is a problem!");
	    		bRes = false;
	    	}
    		
    	}
    	else {
    		WasdiLog.debugLog("GeoServerManager.publishGeoPackageFile: createGeoPckgDataStore returned false");
    	}
    	
		return bRes;
	}    
    
    
    /**
     * Creates a datastore for geopkg files
     * @param sFilePath
     * @param sStoreName
     * @return
     */
    private boolean createGeoPckgDataStore(String sFilePath, String sStoreName)  {

        String sUrl = m_sRestUrl + "/rest/workspaces/" + m_sWorkspace + "/datastores";

        String sXml =
            "<dataStore>" +
            "  <name>" + sStoreName + "</name>" +
            "  <connectionParameters>" +
            "    <entry key=\"dbtype\">geopkg</entry>" +
            "    <entry key=\"database\">file:" + sFilePath + "</entry>" +
            "  </connectionParameters>" +
            "</dataStore>";

        WasdiLog.debugLog("GeoServerManager.createGeoPckgDataStore calling: " + sUrl + " with body " + sXml);
        
        String sResult = HTTPUtils.postXml(sUrl, sXml, m_sRestUser, m_sRestPassword);

        return sResult != null;
    }
    
    public boolean configureLayer(final String workspace, final String resourceName,
            final GSLayerEncoder layer) throws IllegalArgumentException {

        if (workspace == null || resourceName == null || layer == null) {
            throw new IllegalArgumentException("Null argument");
        }
        // TODO: check this usecase, layer should always be defined
        if (workspace.isEmpty() || resourceName.isEmpty() || layer.isEmpty()) {
            throw new IllegalArgumentException("Empty argument");
        }

        final String fqLayerName = workspace + ":" + resourceName;

        final String url = m_sRestUrl + "/rest/layers/" + fqLayerName;

        String layerXml = layer.toString();
        String sendResult = HTTPUtils.putXml(url, layerXml, m_sRestUser, m_sRestPassword);

        return sendResult != null;
    }    
    
    /**
     * Configure a new style for a layer 
     * @param sLayerId
     * @param sStyle
     * @return
     */
    public boolean configureLayerStyle(String sLayerId, String sStyle) {
    	
    	try {
        	boolean bRes = m_oGsPublisher.configureLayer(m_sWorkspace, sLayerId, configureDefaultStyle(sStyle));
        	
        	return bRes;		
    	}
    	catch (Exception oEx) {
			WasdiLog.debugLog("GeoServerManager.configureLayerStyle: exception " + oEx.toString());
		}
    	
    	return false;
    }    
    
    /**
     * Taken from geotools lib since it is private and we need it for configureLayer
     * @param sDefaultStyle Style to apply
     * @return
     */
    private GSLayerEncoder configureDefaultStyle(String sDefaultStyle) {
        final GSLayerEncoder layerEncoder = new GSLayerEncoder();
        if (sDefaultStyle != null && !sDefaultStyle.isEmpty()) {
            if(sDefaultStyle.indexOf(":") != -1) {
                String[] wsAndName = sDefaultStyle.split(":");
                layerEncoder.setDefaultStyle(wsAndName[0], wsAndName[1]);
            } else {
                layerEncoder.setDefaultStyle(sDefaultStyle);
            }
        }
        return layerEncoder;
    }

    /**
     * Get the featureType xml that is the body to send to geoserver to create the shapefile layer
     * @param sShapeFile Full path of the shapefile NOT zipped
     * @param sEPSG
     * @return
     */
    private String getFeatureTypeFromShapeFile(String sShapeFile, String sStoreName, String sEPSG) {
    	String sXML = "<featureType>\n";
    	
    	try {
    		
    		// This code needs the real shp, not the zipped. It is in the same folder so no problem
    		if (sShapeFile.endsWith(".zip")) {
    			sShapeFile = sShapeFile.replace(".zip", ".shp");
    		}
    		
    		File oShapeFile = new File(sShapeFile);
    		
    		// Try to read the shape
            ShapefileDataStore oShapefileDataStore = new ShapefileDataStore(oShapeFile.toURI().toURL());
            
			// Check the coordinate system
            SimpleFeatureCollection oFeatColl = oShapefileDataStore.getFeatureSource().getFeatures();
			CoordinateReferenceSystem oCrs = oFeatColl.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();
			
		    if (oCrs == null) {
		        oCrs = DefaultGeographicCRS.WGS84;
		    }
		    
		    // Get the envelope
			ReferencedEnvelope oBbox = oFeatColl.getBounds();
			double dMinY = oBbox.getMinY();
			double dMinX = oBbox.getMinX();
			double dMaxY = oBbox.getMaxY();
			double dMaxX = oBbox.getMaxX();            
            
            SimpleFeatureType oSimpleFeatureType = oShapefileDataStore.getSchema();
                        
            sXML +="<name>" + sStoreName + "</name>\r\n<nativeName>" + sStoreName + "</nativeName>\r\n";
            sXML += "<namespace>\r\n" + "<name>wasdi</name>\r\n";
            sXML += "<atom:link xmlns:atom=\"http://www.w3.org/2005/Atom\" rel=\"alternate\" href=\"" + WasdiConfig.Current.geoserver.address + "/rest/namespaces/wasdi.xml\" type=\"application/xml\"/>\r\n";
            sXML += "</namespace>\r\n";
            sXML += "<title>"+ sStoreName + "</title>\r\n";
            sXML += "<keywords></keywords>\r\n";
            sXML += "<projectionPolicy>FORCE_DECLARED</projectionPolicy>\r\n";
            sXML += "<enabled>true</enabled>\r\n";
            sXML += "<store class=\"dataStore\">\r\n"
            		+ "<name>wasdi:" + sStoreName + "</name>\r\n"
            		+ "<atom:link xmlns:atom=\"http://www.w3.org/2005/Atom\" rel=\"alternate\" href=\"" + WasdiConfig.Current.geoserver.address + "/rest/workspaces/wasdi/datastores/" + sStoreName + ".xml\" type=\"application/xml\"/>\r\n"
            		+ "</store>";
            
            sXML += "<srs>" + sEPSG + "</srs>";
            
            String sBbox = "<minx>" + dMinX + "</minx>" + "<maxx>" + dMaxX + "</maxx>" + "<miny>" + dMinY + "</miny>" + "<maxy>" + dMaxY + "</maxy>";
            sBbox += "<crs>" + sEPSG + "</crs>";
            
            sXML += "<nativeBoundingBox>" + sBbox + "</nativeBoundingBox>";
            sXML += "<latLonBoundingBox>" + sBbox + "</latLonBoundingBox>";
            //sXML += "<nativeCRS>" +  StringEscapeUtils.escapeXml(oCrs.toWKT()) + "</nativeCRS>";
            sXML += "<nativeCRS>" +  "GEOGCS[\"WGS 84\", DATUM[\"World Geodetic System 1984\", SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], AUTHORITY[\"EPSG\",\"6326\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4326\"]]" + "</nativeCRS>";
            
            sXML += "<simpleConversionEnabled>false</simpleConversionEnabled>";
            sXML += "<attributes>";
            for (int iAttributes=0; iAttributes<oSimpleFeatureType.getAttributeCount(); iAttributes++) 
            {
            	sXML += "<attribute>";
            	AttributeDescriptor oDescriptor = oSimpleFeatureType.getDescriptor(iAttributes);
            	String sAttributeName = (String) oDescriptor.getLocalName();
            	AttributeType oType = oDescriptor.getType();
            	
            	String sType = oType.getBinding().getName();
            	sXML += "<name>" + sAttributeName + "</name>";
            	sXML += "<binding>" + sType + "</binding>";
            	sXML += "<nillable>" + oDescriptor.isNillable() + "</nillable>";
            	sXML += "<minOccurs>" + oDescriptor.getMinOccurs() + "</minOccurs>";
            	sXML += "<maxOccurs>" + oDescriptor.getMaxOccurs() + "</maxOccurs>";
            	sXML += "</attribute>";
            }
            
            sXML += "</attributes>";
            sXML += "</featureType>";
            
            // Clean
            oShapefileDataStore.dispose();        		
    	}
    	catch (Exception oEx) {
    		WasdiLog.debugLog("GeoServerManager.getFeatureTypeFromShapeFile: " + oEx.toString());
		}
    	return sXML;
    }
}
