package wasdi.snapopearations;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.common.MosaicOp.Variable;
import org.esa.snap.dataio.geotiff.GeoTiffProductWriterPlugIn;

import wasdi.LauncherMain;
import wasdi.LoggerWrapper;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.MosaicParameter;
import wasdi.shared.parameters.MosaicSetting;
import wasdi.shared.utils.Utils;

public class Mosaic {
	
	/**
	 * Mosaic Settings
	 */
	MosaicSetting m_oMosaicSetting;
	
	/**
	 * Mosaic Parameter
	 */
	MosaicParameter m_oMosaicParameter;	
	
	/**
	 * Logger
	 */
	private LoggerWrapper m_oLogger = LauncherMain.s_oLogger;
	
	/**
	 * Local WASDI base path
	 */
	private String m_sBasePath = "";
	
	/**
	 * Output file format
	 */
	private String m_sOutputFileFormat = GeoTiffProductWriterPlugIn.GEOTIFF_FORMAT_NAME;
	
	/**
	 * Output file name
	 */
	private String m_sOuptutFile;
	
	/**
	 * Map of the source Products
	 */
	Map<String, Product> m_aoSourceMap = new HashMap<>();

	/**
	 * Process Workspace Repo
	 */
	private ProcessWorkspaceRepository m_oProcessRepository;

	/**
	 * Process Id
	 */
	private ProcessWorkspace m_oProcess;
	
    protected static final String PROPERTY_MAX_VALUE = "maxValue";
    protected static final String PROPERTY_MIN_VALUE = "minValue";	
	
	public Mosaic(MosaicParameter oParameter, String sBasePath) {
		m_oMosaicSetting = (MosaicSetting) oParameter.getSettings();
		m_oMosaicParameter = oParameter;
		m_sBasePath = sBasePath;
		m_sOuptutFile = oParameter.getDestinationProductName();
		
		if (!Utils.isNullOrEmpty(m_oMosaicSetting.getOutputFormat())) {
			m_sOutputFileFormat = m_oMosaicSetting.getOutputFormat();
		}
		
		m_oProcessRepository = new ProcessWorkspaceRepository();
		m_oProcess = m_oProcessRepository.getProcessByProcessObjId(oParameter.getProcessObjId());
	}
	
	/**
	 * Get Base Path
	 * @return
	 */
	public String getBasePath() {
		return m_sBasePath;
	}

	/** 
	 * Set Base Path
	 * @param sBasePath
	 */
	public void setBasePath(String sBasePath) {
		this.m_sBasePath = sBasePath;
	}

	/**
	 * Get Output File Format
	 * @return
	 */
	public String getOutputFileFormat() {
		return m_sOutputFileFormat;
	}

	/**
	 * Set Output File Format
	 * @param sOutputFileFormat
	 */
	public void setOutputFileFormat(String sOutputFileFormat) {
		this.m_sOutputFileFormat = sOutputFileFormat;
	}
	
	/**
	 * Init internal product map
	 */
	protected void initializeProductMap() {
		
		// Get Base Path
		String sWorkspacePath = LauncherMain.getWorspacePath(m_oMosaicParameter);
		
		// for each product
		for (int iProducts = 0; iProducts<m_oMosaicSetting.getSources().size(); iProducts ++) {
			
			// Get full path
			String sProductFile = sWorkspacePath+m_oMosaicSetting.getSources().get(iProducts);
			m_oLogger.debug("Mosaic.initializeProductMap: Product [" + iProducts +"] = " + sProductFile);
			
			File oFileProduct = new File(sProductFile);
			
			try {
				// Read and add to the map
				Product oProduct = ProductIO.readProduct(oFileProduct);
				m_aoSourceMap.put("sourceProduct"+(iProducts+1), oProduct);
				
			} catch (IOException e) {
				//e.printStackTrace();
				m_oLogger.error("Mosaic.initializeProductMap: Exception reding Product [" + iProducts +"] = " + sProductFile);
				m_oLogger.error("Mosaic.initializeProductMap: " + e.toString());
			}
		}
	}
	
	/**
	 * Get the latitude of a product
	 * @param oProduct Product
	 * @param sLevel minValue or maxValue
     * @return the min or max latitude of the product
	 */
    private double computeLatitude(Product oProduct, String sLevel){
    	
        final GeoCoding oSceneGeoCoding = oProduct.getSceneGeoCoding();
        Double[] adLatitudePoints = {
                            oSceneGeoCoding.getGeoPos(new PixelPos(0, 0), null).getLat(),
                            oSceneGeoCoding.getGeoPos(new PixelPos(0, oProduct.getSceneRasterHeight()), null).getLat(),
                            oSceneGeoCoding.getGeoPos(new PixelPos(oProduct.getSceneRasterWidth(), 0), null).getLat(),
                            oSceneGeoCoding.getGeoPos(new PixelPos(oProduct.getSceneRasterWidth(), oProduct.getSceneRasterHeight()), null).getLat()
        };

        switch(sLevel) {
            case PROPERTY_MIN_VALUE :
                return Collections.min(Arrays.asList(adLatitudePoints));
            case PROPERTY_MAX_VALUE :
                return Collections.max(Arrays.asList(adLatitudePoints));
            default :
                return Double.MAX_VALUE;
        }

    }

    /**
     * Get Longitute value of a Product
     * @param oProduct Product
     * @param sLevel minValue or maxValue
     * @return the min or max longitude of the product
     */
    private double computeLongitude(Product oProduct, String sLevel){
        final GeoCoding oSceneGeoCoding = oProduct.getSceneGeoCoding();
        Double[] longitudePoints = {
                oSceneGeoCoding.getGeoPos(new PixelPos(0, 0), null).getLon(),
                oSceneGeoCoding.getGeoPos(new PixelPos(0, oProduct.getSceneRasterHeight()), null).getLon(),
                oSceneGeoCoding.getGeoPos(new PixelPos(oProduct.getSceneRasterWidth(), 0), null).getLon(),
                oSceneGeoCoding.getGeoPos(new PixelPos(oProduct.getSceneRasterWidth(), oProduct.getSceneRasterHeight()), null).getLon()
        };

        switch(sLevel) {
            case PROPERTY_MIN_VALUE :
                return Collections.min(Arrays.asList(longitudePoints));
            case PROPERTY_MAX_VALUE :
                return Collections.max(Arrays.asList(longitudePoints));
            default :
                return Double.MAX_VALUE;
        }
    }
	
    /**
     * If needed, auto fine bounding box containing all the available sources
     */
	protected void findBoundingBox() {
		
		if (m_oMosaicSetting.getSouthBound() == -1.0 && m_oMosaicSetting.getNorthBound() == -1.0 && m_oMosaicSetting.getEastBound() == -1.0 && m_oMosaicSetting.getWestBound() == -1.0) {
			// Convert the Map in an array
			Collection<Product> aoProductCollection = m_aoSourceMap.values();
			Product aoProducts [] = new Product[aoProductCollection.size()];
			
			aoProducts = aoProductCollection.toArray(aoProducts);
			
			if (aoProducts.length<=0) return;
			
	        // set default values in case aoProducts.length == 0
	        double dSouthBoundVal = 35.0;
	        double dNorthBoundVal = 75.0;
	        double dWestBoundVal = -15.0;
	        double dEastBoundVal = 30.0;

	        if ( (aoProducts.length >= 1) && (aoProducts[0]) != null) {
	            dSouthBoundVal = computeLatitude(aoProducts[0], PROPERTY_MIN_VALUE);
	            dNorthBoundVal = computeLatitude(aoProducts[0], PROPERTY_MAX_VALUE);
	            dWestBoundVal = computeLongitude(aoProducts[0], PROPERTY_MIN_VALUE);
	            dEastBoundVal = computeLongitude(aoProducts[0], PROPERTY_MAX_VALUE);
	        }

	        for (int i = 1; i < aoProducts.length; i++) {
	            if (aoProducts[i] != null) {
	                double dSouthBoundValTemp = computeLatitude(aoProducts[i], PROPERTY_MIN_VALUE);
	                double dNorthBoundValTemp = computeLatitude(aoProducts[i], PROPERTY_MAX_VALUE);
	                double dWestBoundValTemp = computeLongitude(aoProducts[i], PROPERTY_MIN_VALUE);
	                double dEastBoundValTemp = computeLongitude(aoProducts[i], PROPERTY_MAX_VALUE);

	                if (dSouthBoundValTemp < dSouthBoundVal) dSouthBoundVal = dSouthBoundValTemp;
	                if (dNorthBoundValTemp > dNorthBoundVal) dNorthBoundVal = dNorthBoundValTemp;
	                if (dWestBoundValTemp < dWestBoundVal) dWestBoundVal = dWestBoundValTemp;
	                if (dEastBoundValTemp > dEastBoundVal) dEastBoundVal = dEastBoundValTemp;
	            }
	        }

	        m_oMosaicSetting.setNorthBound(dNorthBoundVal);
	        m_oMosaicSetting.setSouthBound(dSouthBoundVal);
	        m_oMosaicSetting.setEastBound(dEastBoundVal);
	        m_oMosaicSetting.setWestBound(dWestBoundVal);

		}

		// TODO
		if (m_oMosaicSetting.getPixelSizeX() == -1.0 && m_oMosaicSetting.getPixelSizeY() == -1.0) {
			m_oMosaicSetting.setPixelSizeX(0.05);
			m_oMosaicSetting.setPixelSizeY(0.05);
		}
		
	}
	
	/**
	 * Check all the products to find all the bands with the same name to auto-create variables
	 * @return true if found at least one common band, false otherwise
	 */
	protected Boolean findVariable() {
		
		// Convert the Map in an array
		Collection<Product> aoProductCollection = m_aoSourceMap.values();
		Product aoProducts [] = new Product[aoProductCollection.size()];
		
		aoProducts = aoProductCollection.toArray(aoProducts);
		
		if (aoProducts.length<=0) return false;
		
		
		// Find the product with the longest list of band names
		Product oReferenceProduct = null;
		int iBandCount = 0;
		
		for (int iFindRef = 0; iFindRef < aoProducts.length; iFindRef++) {
			if (aoProducts[iFindRef].getBandNames().length > iBandCount) {
				oReferenceProduct = aoProducts[iFindRef];
				iBandCount = aoProducts[iFindRef].getBandNames().length;
			}
		}
		
		// Get the list of all the band names
		String [] asBandNames = oReferenceProduct.getBandNames();
				
		
		for (int iVar = 0; iVar < asBandNames.length; iVar++) {
			
			// Get the band name
			String sBandName = asBandNames[iVar];
			
			// It is common to all products?
			Boolean bAddVariable = true;
			
			// For each product
			for (int iProducts = 0; iProducts<aoProducts.length; iProducts++) {
				
				Product oTestProduct = aoProducts[iProducts];
				
				// This band is in?
				if (oTestProduct.getBand(sBandName)== null) {
					// No, stop
					bAddVariable = false;
					break;
				}
			}
			
			// If the band is available to all products, add it
			if (bAddVariable) {
				m_oMosaicSetting.getVariableNames().add(sBandName);
				m_oMosaicSetting.getVariableExpressions().add(sBandName);
			}
		}
		
		if (m_oMosaicSetting.getVariableNames().size()>0) return true;
		else return false;
	}
	
	public Boolean runGDALMosaic() {
		
		// Check parameter
		if (m_oMosaicSetting == null) {
			m_oLogger.error("Mosaic.runGDALMosaic: parameter is null, return false");
			return false;
		}
		
		if (m_oMosaicSetting.getSources() == null) {
			m_oLogger.error("Mosaic.runGDALMosaic: sources are null, return false");
			return false;
		}
		
		if (m_oMosaicSetting.getSources().size() <= 0) {
			m_oLogger.error("Mosaic.runGDALMosaic: sources are empty, return false");
			return false;
		}
		
		
		try {
			String sGdalMergeCommand = "gdal_merge.py";
			
			ArrayList<String> asArgs = new ArrayList<String>();
			asArgs.add(sGdalMergeCommand);
			
			// Output file
			asArgs.add("-o");
			asArgs.add(LauncherMain.getWorspacePath(m_oMosaicParameter) + m_sOuptutFile);
			
			// Output format
			asArgs.add("-of");
			asArgs.add(LauncherMain.snapFormat2GDALFormat(m_sOutputFileFormat));
			
			if (LauncherMain.snapFormat2GDALFormat(m_sOutputFileFormat).equals("GTiff")) {
				asArgs.add("-co");
				asArgs.add("COMPRESS=LZW");
				
				asArgs.add("-co");
				asArgs.add("BIGTIFF=YES");
			}
			
			// Set No Data for input 
			if (m_oMosaicSetting.getInputIgnoreValue()!= null) {
				asArgs.add("-n");
				asArgs.add(""+m_oMosaicSetting.getInputIgnoreValue());				
			}

			if (m_oMosaicSetting.getNoDataValue() != null) {
				asArgs.add("-a_nodata");
				asArgs.add(""+m_oMosaicSetting.getNoDataValue());				

				asArgs.add("-init");
				asArgs.add(""+m_oMosaicSetting.getNoDataValue());				

			}
			
			// Pixel Size
			if (m_oMosaicSetting.getPixelSizeX()>0.0 && m_oMosaicSetting.getPixelSizeY()>0.0) {
				asArgs.add("-ps");
				asArgs.add(""+ m_oMosaicSetting.getPixelSizeX());
				asArgs.add("" + m_oMosaicSetting.getPixelSizeY());
			}
			
			// Get Base Path
			String sWorkspacePath = LauncherMain.getWorspacePath(m_oMosaicParameter);
			
			// for each product
			for (int iProducts = 0; iProducts<m_oMosaicSetting.getSources().size(); iProducts ++) {
				
				// Get full path
				String sProductFile = sWorkspacePath+m_oMosaicSetting.getSources().get(iProducts);
				m_oLogger.debug("Mosaic.runGDALMosaic: Product [" + iProducts +"] = " + sProductFile);
				
				asArgs.add(sProductFile);
			}
			
			// Execute the process
			ProcessBuilder oProcessBuidler = new ProcessBuilder(asArgs.toArray(new String[0]));
			Process oProcess;
		
			String sCommand = "";
			for (String sArg : asArgs) {
				sCommand += sArg + " ";
			}
			
			m_oLogger.debug("Mosaic.runGDALMosaic: Command = " + sCommand);
			
			oProcessBuidler.redirectErrorStream(true);
			oProcess = oProcessBuidler.start();
			
			BufferedReader oReader = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
			String sLine;
			while ((sLine = oReader.readLine()) != null)
				m_oLogger.debug("[gdal]: " + sLine);
			
			oProcess.waitFor();
			
			if (new File(sWorkspacePath+m_sOuptutFile).exists()) {
				// Done
				m_oLogger.debug("Mosaic.runGDALMosaic: created GDAL file = " + m_sOuptutFile);				
			}
			else {
				// Error
				m_oLogger.debug("Mosaic.runGDALMosaic: error creating mosaic = " + m_sOuptutFile);
				return false;
			}
			
		} 
        catch (Throwable e) {
			m_oLogger.error("Mosaic.runGDALMosaic: Exception generating output Product " + LauncherMain.getWorspacePath(m_oMosaicParameter) + m_sOuptutFile);
			m_oLogger.error("Mosaic.runGDALMosaic: " + e.toString());
			return false;
		}

		return true;		
	}
	
	/**
	 * Run the Mosaic Operation
	 * @return
	 */
	public Boolean runMosaic() {
		
		// Check parameter
		if (m_oMosaicSetting == null) {
			m_oLogger.error("Mosaic.runMosaic: parameter is null, return false");
			return false;
		}
		
		if (m_oMosaicSetting.getSources() == null) {
			m_oLogger.error("Mosaic.runMosaic: sources are null, return false");
			return false;
		}
		
		if (m_oMosaicSetting.getSources().size() <= 0) {
			m_oLogger.error("Mosaic.runMosaic: sources are empty, return false");
			return false;
		}
		
		// Init the product Map
		initializeProductMap();
		
		// Check if we need auto bounding box
		findBoundingBox();
		
		// The user gave us the variables?
		if (m_oMosaicSetting.getVariableNames().size()<=0) {
			
			// try auto find
			if (!findVariable()) {
				m_oLogger.error("Mosaic.runMosaic: variable not available, return false");
				return false;				
			}
		}
		
		// Check Variables
		Variable [] aoVarialbes = new Variable[m_oMosaicSetting.getVariableNames().size()];
		
		for (int iVar = 0; iVar < aoVarialbes.length; iVar ++) {
			aoVarialbes[iVar] = new Variable();
			
			aoVarialbes[iVar].setName(m_oMosaicSetting.getVariableNames().get(iVar));
			
			if (iVar<m_oMosaicSetting.getVariableExpressions().size()) {
				aoVarialbes[iVar].setExpression(m_oMosaicSetting.getVariableExpressions().get(iVar));
			}
			else {
				aoVarialbes[iVar].setExpression(m_oMosaicSetting.getVariableNames().get(iVar));
			}
		}
		
		// Fill Paramters Map
		
		Map<String, Object> aoParameterMap = new HashMap<>();
		
        aoParameterMap.put("variables", aoVarialbes);
        
        
        aoParameterMap.put("crs", m_oMosaicSetting.getCrs());
        aoParameterMap.put("southBound", m_oMosaicSetting.getSouthBound());
        aoParameterMap.put("northBound", m_oMosaicSetting.getNorthBound());
        aoParameterMap.put("eastBound", m_oMosaicSetting.getEastBound());
        aoParameterMap.put("westBound", m_oMosaicSetting.getWestBound());

        aoParameterMap.put("pixelSizeX", m_oMosaicSetting.getPixelSizeX());
        aoParameterMap.put("pixelSizeY", m_oMosaicSetting.getPixelSizeY());
        
        aoParameterMap.put("overlappingMethod", m_oMosaicSetting.getOverlappingMethod());        
        aoParameterMap.put("showSourceProducts", m_oMosaicSetting.getShowSourceProducts());
        aoParameterMap.put("elevationModelName", m_oMosaicSetting.getElevationModelName());
        
        aoParameterMap.put("resamplingName", m_oMosaicSetting.getResamplingName());
        aoParameterMap.put("updateMode", m_oMosaicSetting.getUpdateMode());
        
        aoParameterMap.put("nativeResolution", m_oMosaicSetting.getNativeResolution());
        aoParameterMap.put("combine" , m_oMosaicSetting.getCombine());
        
        // Check if we need multi size or single
        Boolean bMultiSize = false;
        
        for(Product oProduct: m_aoSourceMap.values() ) {
            if(oProduct.isMultiSize()){
            	bMultiSize = true;
                break;
            }        	
        }
        
        // Execute Operator
        Product oOutputProduct = null;
        
        if(!bMultiSize) {
        	m_oLogger.debug("Mosaic.runMosaic: executing Mosaic Operator");
        	oOutputProduct = GPF.createProduct("Mosaic", aoParameterMap, m_aoSourceMap);
        }
        else {
        	m_oLogger.debug("Mosaic.runMosaic: executing Multi-size Mosaic Operator");
        	oOutputProduct = GPF.createProduct("Multi-size Mosaic", aoParameterMap, m_aoSourceMap);
        }
        
        if (oOutputProduct == null) {
        	m_oLogger.error("Mosaic.runMosaic: Output file is null, return false");
        	return false;
        }
        
        try {
        	
        	/*
        	 * This can be a speed up: do not save the _count band. 
        	 * But how then the processor consider non valid pixels? 
        	 * How does it work "normally"?


        	// Remove (not requested) _count bands if in place
        	for (int iBands = 0; iBands<m_oMosaicSetting.getVariableNames().size(); iBands++) {
        		String sName = m_oMosaicSetting.getVariableNames().get(iBands);
        		
        		if (Utils.isNullOrEmpty(sName)) continue;
        		
        		Band oBand = oOutputProduct.getBand(sName);
        		Band oCountBand = oOutputProduct.getBand(sName+"_count");
        		
        		if (oCountBand != null && oBand != null) {
        			oBand.setValidPixelExpression("");
        			oOutputProduct.removeBand(oCountBand);
        		}
        	}
        	*/
        	// Save output
			ProductIO.writeProduct(oOutputProduct, LauncherMain.getWorspacePath(m_oMosaicParameter) + m_sOuptutFile, m_sOutputFileFormat, new WasdiProgreeMonitor(m_oProcessRepository, m_oProcess));
		} 
        catch (IOException e) {
			m_oLogger.error("Mosaic.runMosaic: Exception writing output Product " + m_sOuptutFile);
			m_oLogger.error("Mosaic.runMosaic: " + e.toString());
			return false;
		}

		return true;
	}
	
	
}
