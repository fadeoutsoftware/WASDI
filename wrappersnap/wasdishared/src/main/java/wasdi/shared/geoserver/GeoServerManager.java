package wasdi.shared.geoserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.Purge;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTBoundingBox;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.decoder.RESTLayer.Type;
import it.geosolutions.geoserver.rest.decoder.RESTResource;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSImageMosaicEncoder;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class GeoServerManager {
	
	
	private final String workspace = "wasdi";

    String restUrl  = "http://localhost:8080/geoserver";
    String restUser = "admin";
    String restPassword   = "geoserver";
    GeoServerRESTPublisher gsPublisher;
    GeoServerRESTReader gsReader;

    public GeoServerManager(String sRestUrl, String sUser, String sPassword) throws MalformedURLException {
        restUrl = sRestUrl;
        restUser = sUser;
        restPassword = sPassword;
        gsReader = new GeoServerRESTReader(restUrl, restUser, restPassword);
        gsPublisher = new GeoServerRESTPublisher(restUrl, restUser, restPassword);

    	if (!gsReader.existsWorkspace(workspace)) {
    		gsPublisher.createWorkspace(workspace);
    	}
    }
    
    public String getLayerBBox(String layerId) {
    	
    	RESTLayer layer = gsReader.getLayer(workspace, layerId);
    	RESTResource res = gsReader.getResource(layer);
    	RESTBoundingBox bbox = res.getLatLonBoundingBox();
    	
    	String ret = String.format("{\"miny\":%f,\"minx\":%f,\"crs\":\"%s\",\"maxy\":%f,\"maxx\":%f}", 
    			bbox.getMinY(), bbox.getMinX(), bbox.getCRS().replace("\"", "\\\\\\\""), bbox.getMaxY(), bbox.getMaxX());
    	
    	return ret;
    	
    }

    public boolean removeLayer(String layerId) {

    	RESTLayer layer = gsReader.getLayer(workspace, layerId);
    	Type layerType = layer.getType();    	
    	RESTResource res = gsReader.getResource(layer);
    	
    	String storeName = res.getStoreName();
    	String[] toks = storeName.split(":");
    	if (toks.length>1) storeName = toks[1];
    	
		switch (layerType) {
		case VECTOR:
			return gsPublisher.removeDatastore(workspace, storeName, true, Purge.ALL);			
		case RASTER:
			return gsPublisher.removeCoverageStore(workspace, storeName, true, Purge.ALL);
		default:
			System.out.println("GeoServerManager.removeLayer: unknown layer type for " + layerId);
			break;
		}

    	return false;
    }
    

    public boolean publishImagePyramid(String storeName, String style, String epsg, File baseDir)
            throws FileNotFoundException {
    	
    	RESTLayer layer = gsReader.getLayer(workspace, storeName);
    	if (layer != null) removeLayer(storeName);
    	
    	//layer encoder
    	final GSLayerEncoder layerEnc = new GSLayerEncoder();
    	if (style==null || style.isEmpty()) style="raster";
    	layerEnc.setDefaultStyle(style);
    	
    	//coverage encoder
    	final GSImageMosaicEncoder coverageEnc=new GSImageMosaicEncoder();
    	coverageEnc.setName(storeName);
    	coverageEnc.setTitle(storeName);
    	if (epsg!=null) coverageEnc.setSRS(epsg);
    	coverageEnc.setMaxAllowedTiles(Integer.MAX_VALUE); 
    	
    	//publish
    	boolean res = gsPublisher.publishExternalMosaic(workspace, storeName, baseDir, coverageEnc, layerEnc);
    	
    	//configure coverage
        if (res && gsReader.existsCoveragestore(workspace, storeName) && gsReader.existsCoverage(workspace, storeName, storeName)) {
        	GSCoverageEncoder ce = new GSCoverageEncoder();
            ce.setEnabled(true); //abilito il coverage
            ce.setSRS(epsg);
        	gsPublisher.configureCoverage(ce, workspace, storeName);
        }

    	
		return res;
    	
    }


    public boolean publishStandardGeoTiff(String storeName, File geotiffFile, String epsg, String style)
            throws FileNotFoundException {

    	RESTLayer layer = gsReader.getLayer(workspace, storeName);
    	if (layer != null) removeLayer(storeName);

    	if (style == null || style.isEmpty()) style = "raster";
        
        boolean res = gsPublisher.publishExternalGeoTIFF(workspace,storeName,geotiffFile, storeName, epsg, GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED,style);
        
        if (res && gsReader.existsCoveragestore(workspace, storeName) && gsReader.existsCoverage(workspace, storeName, storeName)) {
        	GSCoverageEncoder ce = new GSCoverageEncoder();
            ce.setEnabled(true); //abilito il coverage
            ce.setSRS(epsg);
        	gsPublisher.configureCoverage(ce, workspace, storeName);
        }
        
		return res;
    }
}
