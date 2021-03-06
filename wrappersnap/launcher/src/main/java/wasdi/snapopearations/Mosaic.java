package wasdi.snapopearations;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import wasdi.LauncherMain;
import wasdi.ProcessWorkspaceLogger;
import wasdi.shared.parameters.MosaicParameter;
import wasdi.shared.parameters.MosaicSetting;
import wasdi.shared.utils.LoggerWrapper;
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
	private String m_sOutputFileFormat = "GeoTIFF";
	
	/**
	 * Output file name
	 */
	private String m_sOuptutFile;
		
	/**
	 * Reference to the process workspace Logger
	 */
	protected ProcessWorkspaceLogger m_oProcessWorkspaceLogger = null;
	
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
	
	
	public Boolean runGDALMosaic() {
		
		// Check parameter
		if (m_oMosaicSetting == null) {
			processWorkspaceLog("Error on parameters");
			m_oLogger.error("Mosaic.runGDALMosaic: parameter is null, return false");
			return false;
		}
		
		if (m_oMosaicSetting.getSources() == null) {
			processWorkspaceLog("Error: sources not available");
			m_oLogger.error("Mosaic.runGDALMosaic: sources are null, return false");
			return false;
		}
		
		if (m_oMosaicSetting.getSources().size() <= 0) {
			processWorkspaceLog("Error: sources not available");
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
			
			processWorkspaceLog("Setting output file " + m_sOuptutFile);
			
			// Output format
			asArgs.add("-of");
			asArgs.add(LauncherMain.snapFormat2GDALFormat(m_sOutputFileFormat));
			
			if (LauncherMain.snapFormat2GDALFormat(m_sOutputFileFormat).equals("GTiff")) {
				
				processWorkspaceLog("Adding Tiff format options");
				
				asArgs.add("-co");
				asArgs.add("COMPRESS=LZW");
				
				asArgs.add("-co");
				asArgs.add("BIGTIFF=YES");
			}
			
			// Set No Data for input 
			if (m_oMosaicSetting.getInputIgnoreValue()!= null) {
				processWorkspaceLog("Adding Ignore input value option" + m_oMosaicSetting.getInputIgnoreValue().toString());
				
				asArgs.add("-n");
				asArgs.add(""+m_oMosaicSetting.getInputIgnoreValue());				
			}

			if (m_oMosaicSetting.getNoDataValue() != null) {
				processWorkspaceLog("Adding no data option " + m_oMosaicSetting.getNoDataValue().toString());
				
				asArgs.add("-a_nodata");
				asArgs.add(""+m_oMosaicSetting.getNoDataValue());				

				asArgs.add("-init");
				asArgs.add(""+m_oMosaicSetting.getNoDataValue());				

			}
			
			// Pixel Size
			if (m_oMosaicSetting.getPixelSizeX()>0.0 && m_oMosaicSetting.getPixelSizeY()>0.0) {
				processWorkspaceLog("Adding pixel size option x = "+ m_oMosaicSetting.getPixelSizeX() + " - y = " + m_oMosaicSetting.getPixelSizeY());
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
				
				processWorkspaceLog("Adding input product " + m_oMosaicSetting.getSources().get(iProducts));
				
				asArgs.add(sProductFile);
			}
			
			// Execute the process
			ProcessBuilder oProcessBuidler = new ProcessBuilder(asArgs.toArray(new String[0]));
			Process oProcess;
		
			String sCommand = "";
			for (String sArg : asArgs) {
				sCommand += sArg + " ";
			}
			
			processWorkspaceLog("Start real mosaic");
			
			m_oLogger.debug("Mosaic.runGDALMosaic: Command = " + sCommand);
			
			oProcessBuidler.redirectErrorStream(true);
			oProcess = oProcessBuidler.start();
			
			BufferedReader oReader = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
			String sLine;
			while ((sLine = oReader.readLine()) != null)
				m_oLogger.debug("[gdal]: " + sLine);
			
			oProcess.waitFor();
			
			if (new File(sWorkspacePath+m_sOuptutFile).exists()) {
				
				processWorkspaceLog("Mosaic done");
				
				// Done
				m_oLogger.debug("Mosaic.runGDALMosaic: created GDAL file = " + m_sOuptutFile);				
			}
			else {
				processWorkspaceLog("Error creating output file");
				// Error
				m_oLogger.debug("Mosaic.runGDALMosaic: error creating mosaic = " + m_sOuptutFile);
				return false;
			}
			
		} 
        catch (Throwable e) {
        	processWorkspaceLog("There was an exception...");
			m_oLogger.error("Mosaic.runGDALMosaic: Exception generating output Product " + LauncherMain.getWorspacePath(m_oMosaicParameter) + m_sOuptutFile);
			m_oLogger.error("Mosaic.runGDALMosaic: " + e.toString());
			return false;
		}

		return true;		
	}
	
	
	/**
	 * Get the workspace logger
	 * @return
	 */
	public ProcessWorkspaceLogger geProcessWorkspaceLogger() {
		return m_oProcessWorkspaceLogger;
	}

	/**
	 * Set the workspace logger
	 * @param oProcessWorkspaceLogger
	 */
	public void setProcessWorkspaceLogger(ProcessWorkspaceLogger oProcessWorkspaceLogger) {
		this.m_oProcessWorkspaceLogger = oProcessWorkspaceLogger;
	}
	
	/**
	 * Safe Processo
	 * @param sLog
	 */
	protected void processWorkspaceLog(String sLog) {
		if (m_oProcessWorkspaceLogger!=null) {
			m_oProcessWorkspaceLogger.log(sLog);
		}
	}
	
	
	
}
