package wasdi.shared.geoserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.Purge;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.HTTPUtils;
import it.geosolutions.geoserver.rest.decoder.RESTBoundingBox;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.decoder.RESTLayer.Type;
import it.geosolutions.geoserver.rest.decoder.RESTResource;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSImageMosaicEncoder;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class GeoServerManager {
	
	
	private final String m_sWorkspace = "wasdi";

    String m_sRestUrl;
    String m_sRestUser;
    String m_sRestPassword;
    GeoServerRESTPublisher m_oGsPublisher;
    GeoServerRESTReader m_oGsReader;

    public GeoServerManager(String sRestUrl, String sUser, String sPassword) throws MalformedURLException {
        m_sRestUrl = sRestUrl;
        m_sRestUser = sUser;
        m_sRestPassword = sPassword;
        m_oGsReader = new GeoServerRESTReader(m_sRestUrl, m_sRestUser, m_sRestPassword);
        m_oGsPublisher = new GeoServerRESTPublisher(m_sRestUrl, m_sRestUser, m_sRestPassword);

    	if (!m_oGsReader.existsWorkspace(m_sWorkspace)) {
    		m_oGsPublisher.createWorkspace(m_sWorkspace);
    	}
    }
    
    public GeoServerManager() throws MalformedURLException {
    	this(WasdiConfig.Current.geoserver.address, WasdiConfig.Current.geoserver.user, WasdiConfig.Current.geoserver.password);
    }
    
    public String getLayerBBox(String sLayerId) {
    	
    	try {
        	RESTLayer oLayer = m_oGsReader.getLayer(m_sWorkspace, sLayerId);
        	RESTResource oRes = m_oGsReader.getResource(oLayer);
        	RESTBoundingBox oBbox = oRes.getLatLonBoundingBox();
        	
        	String sRet = String.format("{\"miny\":%f,\"minx\":%f,\"crs\":\"%s\",\"maxy\":%f,\"maxx\":%f}", 
        			oBbox.getMinY(), oBbox.getMinX(), oBbox.getCRS().replace("\"", "\\\\\\\""), oBbox.getMaxY(), oBbox.getMaxX());
        	
        	return sRet;    		
    	}
    	catch (Exception oEx) {
    		String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
    		Utils.debugLog("GeoServerManager.getLayerBBox: ERROR " + sError);
    		return "";
		}
    	
    }

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
			Utils.debugLog("GeoServerManager.removeLayer: unknown layer type for " + sLayerId);
			break;
		}

    	return false;
    }
    

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
    
    public boolean publishStyle(String sStyleFile) {
    	File oFile = new File(sStyleFile);
    	
    	if (oFile.exists()) {
    		String sStyleName = Utils.getFileNameWithoutLastExtension(oFile.getName());
    		return m_oGsPublisher.publishStyle(oFile, sStyleName);
    	}
    	else {
    		return false;
    	}
    }
    
    public boolean styleExists(String sStyle) {
    	String sStyles = HTTPUtils.get(m_sRestUrl+"/rest/styles", m_sRestUser, m_sRestPassword);
    	
    	if (Utils.isNullOrEmpty(sStyles) == false) {
    		String sResearchKey = "\"name\":\"" + sStyle + "\"";
    		
    		if (sStyles.contains(sResearchKey)) return true;
    	}
    	
    	return false;
    }
    
	/**
	 * aggiunge un layer da uno shapefile
	 * @param layerName
	 * @param shp
	 * @param style
	 * @throws DataException 
	 */
/*
    public String publishShapeFile(String layerName, File shp, String style) throws Exception {
    	
		//cerco lo stile
		//StyleInfo si = searchStyleInfo(style);
		
		//creo il data store
		ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
		CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace(workspace);
        DataStoreInfo dsi = builder.buildDataStore(layerName);
		dsi.setType(factory.getDisplayName());
		Map connectionParameters = dsi.getConnectionParameters();
		connectionParameters.put(ShapefileDataStoreFactory.URLP.key, DataUtilities.fileToURL(shp));
		builder.setStore(dsi);

		//creo il feature type info
		try {
			ShapefileDataStore store = (ShapefileDataStore)factory.createDataStore(connectionParameters);
			FeatureTypeInfo ftinfo = builder.buildFeatureType(store.getNames().get(0));
			ftinfo.setName(layerName);
			builder.lookupSRS(ftinfo, true);
	        builder.setupBounds(ftinfo);
	        catalog.add(dsi);
	        catalog.add(ftinfo);  		
			
			//creo il layer
	        LayerInfo li = builder.buildLayer(ftinfo);
	        updateLayerInfo(li, si);
    		catalog.add(li);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DataException("errore reading shapefile: " + e.getMessage());
		}

//			synchronized (storesTimestampsSync) {
		storesTimestamps.put(dsi.getName(), new Date());
//			}
		
	}
*/
}
