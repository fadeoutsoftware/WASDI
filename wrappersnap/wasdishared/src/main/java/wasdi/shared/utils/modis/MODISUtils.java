package wasdi.shared.utils.modis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import wasdi.shared.business.modis11a2.ModisItemForWriting;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.data.modis11a2.ModisRepository;

public class MODISUtils {
	
	private static final String s_sUsername;
	private static final String s_sPassword;
	private static final String s_sCSVFilePath;
	
	static {
		DataProviderConfig oDataProvider = WasdiConfig.Current.getDataProviderConfig("LPDAAC");
		s_sUsername = oDataProvider.user;
		s_sPassword = oDataProvider.password;
		
		// this is the CSV file downloaded from https://earthexplorer.usgs.gov/ and containing the list of all MOD11A6 products, with some of their metadata
		s_sCSVFilePath = WasdiConfig.Current.paths.userHomePath + "modis_mod11a2_v61_64f584ce5c8845f6.csv"; 
	}
	
	// MODIS XML PARAMETERS
	private static final String s_sFileName = "DistributedFileName";
	private static final String s_sFileSize = "FileSize";
	private static final String s_sDayNightFlag = "DayNightFlag";
	private static final String s_sEndTime = "RangeEndingTime";
	private static final String s_sEndDate = "RangeEndingDate";
	private static final String s_sStartTime = "RangeBeginningTime";
	private static final String s_sStartDate = "RangeBeginningDate";
	private static final String s_sBoundary = "Boundary";
	private static final String s_sInstrument = "InstrumentShortName"; 
	private static final String s_sSensor = "SensorShortName";
	private static final String s_sLongitude = "PointLongitude";
	private static final String s_sLatitude = "PointLatitude";
	private static final String s_sPlatform = "PlatformShortName";
	
	// KEY TO ACCESS THE PARAMETERS MAP
	private static final String s_sBoundingBox = "BoundingBox";
	private static final String s_sUrl = "url";
	

    /*
     * Prefix used to identify redirects to URS url, where the authentication is performed before accessing a MODIS product. 
     */
    private static final String s_URSUrl = "https://urs.earthdata.nasa.gov";
    
    /**
     * Base URL where MOD11A2 products are available for download
     */
	private static final String s_sMODISBaseUrl = "https://e4ftl01.cr.usgs.gov/MOLT/MOD11A2.061/";

  
    /**
     * When no parameter is specified, all products in the CSV file are read
     * @throws Exception
     */
    public static void insertProducts() throws Exception {
    	insertProducts(-1, -1);
    }
    
    /**
     * Populate the MODIS catalogue on Mongo, importing the products specified in the CSV files, in the lines included between iStartLine and iEndLine.
     * To populate the db, the method will establish a connection to the data provider and try to read the metadata of the products from the XML files available ONLINE,
     * for each product. 
     * To avoid external connections to the data provider, then the method insertProductsFromCSV should be used. 
     * @param iStartLine line of the CSV file containing the first product to be imported
     * @param iEndLine line of the CSV file containing the last product to be imported
     * @return
     */
    public static void insertProducts(int iStartLine, int iEndLine) {
    	BufferedReader oReader = null;
    	ModisRepository oModisRepo = new ModisRepository();
    	int iProdCounts = 0;
    	
    	try {
    		oReader = new BufferedReader(new FileReader(s_sCSVFilePath));  
    		String sLine = "";
    		int iCurrentLine = 0; // the variable stores the number of the current line of the CSV file being read
    		boolean bImportAll = iStartLine == -1 && iEndLine == -1;
    		
    		while ((sLine = oReader.readLine()) != null) {
    			iCurrentLine ++;
    			
    			// if we are not importing all the products and we didn't reach yet the starting line, then we just more to the next line
    			if (!bImportAll && iCurrentLine < iStartLine) {
    				continue;
    			}
    			
    			// if we are not importing all the products and already passed the ending line, then we can stop reading the other lines
    			if (!bImportAll && iCurrentLine > iEndLine) {
    				break;
    			}
    			
    			String[] asMetadata = sLine.split(","); 
    			String sGranuleId = asMetadata[0];
    			String sStartDate = asMetadata[2].replaceAll("/", ".");
    			
    			String sProductDirectoryUrl = s_sMODISBaseUrl + sStartDate;
    			String sProductFileUrl = sProductDirectoryUrl + "/" + sGranuleId;
    			String sXMLUrlPath = sProductFileUrl + ".xml";
    			WasdiLog.debugLog("MODISUtils.insertProducts: trying to read XML metadata file at URL: " + sXMLUrlPath);
    			
    			try {
	    			if (iCurrentLine - 1 > 0) { // if the line is not the first (the one containing the name of the columns)
	    				String sXMLMetadataString = readXmlFile(sXMLUrlPath); 
	    				
	    				if (!Utils.isNullOrEmpty(sXMLMetadataString)) {
	    					Map<String, String> asProperties = parseXML(sXMLMetadataString, sProductFileUrl);
	    					ModisItemForWriting oItem = buildModisItem(asProperties);
	    					oModisRepo.insertModisItem(oItem);
	    					iProdCounts++;
	    					WasdiLog.debugLog("MODISUtils.insertProducts. product added to db: " + sProductFileUrl);
	    				} else {
	    					WasdiLog.debugLog("MODISUtils.insertProducts. Impossible to read metadata and add product to db: " + sProductFileUrl);
	    				}
	    			}
    			} catch(IOException oEx) {
    	    		WasdiLog.errorLog("MODISUtils.insertProducts. Exception while reading metadata file " + sXMLUrlPath + ". " + oEx.getMessage());
    			} 
    		}  // end while
    		oReader.close();
    	} catch (IOException oEx) {
    		WasdiLog.errorLog("MODISUtils.insertProducts. Error while populating the database with MODIS products " + oEx.getMessage());
    	} finally {
    		if (oReader != null)
				try {
					oReader.close();
				} catch (IOException oEx) {
		    		WasdiLog.errorLog("MODISUtils.insertProducts. Error while closing the buffer reader: " + oEx.getMessage());
				}
    		WasdiLog.debugLog("MODISUtils.insertProducts. Number of products added to the db: " + iProdCounts);
    	}	
    }
    
    /**
     * Populate the MODIS catalogue on Mongo, importing the products specified in the CSV files, in the lines included between iStartLine and iEndLine.
     * To populate the db, the method will ONLY RELY on the metadata stored in the CSV file (this should reduce the time required to populate the db, since no connection
     * to the external provider is needed)
     * @param iStartLine line of the CSV file containing the first product to be imported
     * @param iEndLine line of the CSV file containing the last product to be imported
     * @return
     */
    public static void insertProductsFromCsv(int iStartLine, int iEndLine) {
    	BufferedReader oReader = null;
    	ModisRepository oModisRepo = new ModisRepository();
    	int iProdCounts = 0;
    	
    	
    	
    	try {
    		oReader = new BufferedReader(new FileReader(s_sCSVFilePath));  
    		String sLine = "";
    		int iCurrentLine = 0; // the variable stores the number of the current line of the file being read
    		boolean bImportAll = iStartLine == -1 && iEndLine == -1;
    		
    		while ((sLine = oReader.readLine()) != null) {
    			iCurrentLine ++;
    			
    			// if we are not importing all the products and we didn't reach yet the starting line, then we just more to the next line
    			if (!bImportAll && iCurrentLine < iStartLine) {
    				continue;
    			}
    			
    			// if we are not importing all the products and already passed the ending line, then we can stop reading the other lines
    			if (!bImportAll && iCurrentLine > iEndLine) {
    				WasdiLog.debugLog("MODISUtils.insertProductsFromCsv. Lines have been read.");
    				break;
    			}
    			
    			WasdiLog.debugLog("MODISUtils.insertProductsFromCsv. Reading line: " + iCurrentLine);
    			
    			try {
	    			if (iCurrentLine - 1 > 0) { // if the line is not the first (the one containing the name of the columns)    				
	    				Map<String, String> asProperties = buildProperties(sLine);

	    				if (asProperties != null && !asProperties.isEmpty()) {
	    					ModisItemForWriting oItem = buildModisItem(asProperties);
	    					oModisRepo.insertModisItem(oItem);
	    					iProdCounts++;
	    					WasdiLog.debugLog("MODISUtils.insertProductsFromCsv. product added to db: " + asProperties.getOrDefault(s_sFileName, "null"));
	    				} else {
	    					WasdiLog.debugLog("MODISUtils.insertProductsFromCsv. Impossible to read metadata and add product to db for line: " + sLine);
	    				}
	    				
	    			}
    			} catch(Exception oEx) {
    	    		WasdiLog.errorLog("MODISUtils.insertProductsFromCsv. Exception while reading line " + sLine + ". " + oEx.getMessage());
    			} 
    		}  // end while
    		oReader.close();
    	} catch (IOException oEx) {
    		WasdiLog.errorLog("MODISUtils.insertProductsFromCsv. Error while populating the database with MODIS products " + oEx.getMessage());
    	} finally {
    		if (oReader != null)
				try {
					oReader.close();
				} catch (IOException oEx) {
		    		WasdiLog.errorLog("MODISUtils.insertProductsFromCsv. Error while closing the buffer reader: " + oEx.getMessage());
				}
    		WasdiLog.debugLog("MODISUtils.insertProductsFromCsv. Number of products added to the db: " + iProdCounts);
    	}	
    }
    
    public static void insertMissingProductsFromCsv() throws Exception {
    	WasdiLog.debugLog("insertMissingProductsFromCsv - read the whole CSV");
    	insertMissingProductsFromCsv(-1, -1);
    }
    
    /**
     * When importing the MODIS products reading the metadata from the XML files available in the data provider online, if could happen that the XML can not be accessed.
     * In that case, the product will not be added to the database. 
     * This method tries to repair the database, checking which products, among those listed in the CSV file, are missing in the database and adds those products
     * reading the metadata directly from the CSV file. In that case, only the size of the product will miss, since it is not stored in the CSV file. 
     * @param iStartLine first line of the csv file to read, and eventually start adding the corresponding product
     * @param iEndLine list last line of the csv file to read
     */
    public static void insertMissingProductsFromCsv(int iStartLine, int iEndLine){
    	BufferedReader oReader = null;
    	ModisRepository oModisRepo = new ModisRepository();
    	int iProdCounts = 0;
    	
    	WasdiLog.debugLog("insertMissingProductsFromCsv - method started");
    	
    	try {
    		oReader = new BufferedReader(new FileReader(s_sCSVFilePath));  
    		String sLine = "";
    		int iCurrentLine = 0; // the variable stores the number of the current line of the file being read
    		boolean bImportAll = iStartLine == -1 && iEndLine == -1;
    		
    		while ((sLine = oReader.readLine()) != null) {
    			iCurrentLine ++;
    			
    			if (!bImportAll && iCurrentLine < iStartLine) {
    		    	WasdiLog.debugLog("Skipping line: " + iCurrentLine);
    				continue;
    			}
    			
    			if (!bImportAll && iCurrentLine > iEndLine) {
    				WasdiLog.debugLog("MODISUtils.insertMissingProductsFromCsv. Lines have been read.");
    				break;
    			}
    			
		    	WasdiLog.debugLog("Reading line: " + iCurrentLine);
    			   			
    			try {
	    			if (iCurrentLine - 1 > 0) { // if the line is not the first (the one containing the name of the columns)    				
	    				Map<String, String> asProperties = buildProperties(sLine);

	    				if (asProperties != null && !asProperties.isEmpty()) {
	    					
	    					String sFileName = asProperties.get(s_sFileName);
	    					
	    					if (oModisRepo.countDocumentsMatchingFileName(sFileName) < 1L) {
		    					ModisItemForWriting oItem = buildModisItem(asProperties);
		    					oModisRepo.insertModisItem(oItem);
		    					iProdCounts++;
		    					WasdiLog.debugLog("MODISUtils.insertMissingProductsFromCsv. product added to db: " + asProperties.getOrDefault(s_sFileName, "null"));
	    					}
	    					
	    				} else {
	    					WasdiLog.debugLog("MODISUtils.insertMissingProductsFromCsv. Impossible to read metadata and add product to db for line: " + sLine);
	    				}
	    				
	    			}
    			} catch(Exception oEx) {
    	    		WasdiLog.errorLog("MODISUtils.insertMissingProductsFromCsv. Exception while reading line " + sLine + ". " + oEx.getMessage());
    			} 
    		}  // end while
    		oReader.close();
    	} catch (IOException oEx) {
    		WasdiLog.errorLog("MODISUtils.insertMissingProductsFromCsv. Error while populating the database with MODIS products " + oEx.getMessage());
    	} finally {
    		if (oReader != null)
				try {
					oReader.close();
				} catch (IOException oEx) {
		    		WasdiLog.errorLog("MODISUtils.insertMissingProductsFromCsv. Error while closing the buffer reader: " + oEx.getMessage());
				}
    		WasdiLog.debugLog("MODISUtils.insertMissingProductsFromCsv. Number of products added to the db: " + iProdCounts);
    	}	
    }
    
    /**
     * Takes a line from the CSV file, and builds the map that represents the metadata of a MODIS product 
     * @param sLine a line extracted from the CSV file and representing the metadata of a product
     * @return the map representing the metadata of a MODIS product
     */
    private static Map<String, String> buildProperties(String sLine) {
    	
    	if (Utils.isNullOrEmpty(sLine)) {
    		return null;
    	}
    	
    	String[] asMetadata = sLine.split(",");
    	
    	if (asMetadata.length != 38) {
    		return null;
    	}
    	
    	String sGranuleId = asMetadata[0];
    	String sStartDate = asMetadata[2];
    	
    	String sProductDirectoryUrl = s_sMODISBaseUrl + sStartDate.replaceAll("/", ".");
		String sProductFileUrl = sProductDirectoryUrl + "/" + sGranuleId;
		
		List<Double> adLatitude = new ArrayList<>();
		adLatitude.add(Double.parseDouble(asMetadata[28]));
		adLatitude.add(Double.parseDouble(asMetadata[30]));
		adLatitude.add(Double.parseDouble(asMetadata[32]));
		adLatitude.add(Double.parseDouble(asMetadata[34]));
		
		List<Double> adLongitude = new ArrayList<>();
		adLongitude.add(Double.parseDouble(asMetadata[29]));
		adLongitude.add(Double.parseDouble(asMetadata[31]));
		adLongitude.add(Double.parseDouble(asMetadata[33]));
		adLongitude.add(Double.parseDouble(asMetadata[35]));
		
		String sBoundingBox = getJsonFormatBoundingBox(adLatitude, adLongitude);	
    	
    	Map<String, String> asProperties = new HashMap<>();
    	asProperties.put(s_sFileName, sGranuleId);
		asProperties.put(s_sStartDate, sStartDate.replaceAll("/", "-"));
		asProperties.put(s_sStartTime, "00:00:00.000000");
		asProperties.put(s_sEndDate, asMetadata[3].replaceAll("/", "-"));
		asProperties.put(s_sEndTime, "23:59:59.000000");
		asProperties.put(s_sPlatform, "Terra");
		asProperties.put(s_sDayNightFlag, asMetadata[6]);
		asProperties.put(s_sSensor, "MODIS");
		asProperties.put(s_sInstrument, "MODIS");
		asProperties.put(s_sFileSize, "0");
		asProperties.put(s_sBoundingBox, sBoundingBox);
		asProperties.put(s_sUrl, sProductFileUrl);
		
		return asProperties;	
    }

    
    /**
     * Given the URL of a XML file containing the metadata of a MODIS products, access the XML online and returns a string representation of its content, 
     * preserving its original XML representation.
     * @param sXMLUrlFile the URL of the XML file containing the metadata of a XML product
     * @return the string representing the XML content of the file
     * @throws IOException
     */
    public static String readXmlFile(String sXMLUrlFile) throws IOException {
    	WasdiLog.debugLog("MODISUtils.readXmlFile. Accessing resource: " + sXMLUrlFile);
        
        InputStream oIn = null;
        BufferedReader oBin = null;
        String sXMLMetadata = null;
        try {
            /* Set up a cookie handler to maintain session cookies. */
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
 
            /* Retrieve a stream for the resource */
            oIn = getResource(sXMLUrlFile, s_sUsername, s_sPassword);
            oBin = new BufferedReader(new InputStreamReader(oIn));
            StringBuilder oStringBuilder = new StringBuilder();
            String sLine;
            while( (sLine = oBin.readLine()) != null) {
            	oStringBuilder.append(sLine + "\n");
            }
            
            sXMLMetadata = oStringBuilder.toString();
        }
        catch( Exception oEx) {
        	WasdiLog.errorLog("MODISUtils.readXmlFile. Error while trying to read the metadata file: " + sXMLUrlFile + ". " + oEx.getMessage());
        } finally {
        	if (oIn != null)
        		oIn.close();
        	if (oBin != null) 
        		oBin.close();
        }
    	return sXMLMetadata;     
    }
    
    /**
     * Returns the long representing the epoch, for a given date and time
     * @param sDate the date in the format yyyy-MM-dd
     * @param sTime the time in the format HH:mm:ss.SSSSSS
     * @return the long representing the epoch for the given date and time
     */
	private static Long parseDate(String sDate, String sTime) {
		String sComposedDate = sDate + "T" + sTime.substring(0, 12) + "Z";
		return TimeEpochUtils.fromDateStringToEpoch(sComposedDate);
	}
	
    
	/**
	 * Given the map storing the metadata for a MODIS product, builds the object that will be passed to MongoDB for creating and storing the corresponding document
	 * @param asProperties the map containing the metadata for a MODIS product
	 * @return the object to pass to MongoDB for creating the corresponding entry in the db 
	 */
    private static ModisItemForWriting buildModisItem(Map<String, String> asProperties) {
    	ModisItemForWriting oItem = new ModisItemForWriting();
    	
    	oItem.setSFileName(asProperties.get(s_sFileName));
    	oItem.setLFileSize(Long.parseLong(asProperties.get(s_sFileSize)));
    	oItem.setSDayNightFlag(asProperties.get(s_sDayNightFlag));
    	oItem.setSInstrument(asProperties.get(s_sInstrument));
    	oItem.setSSensor(asProperties.get(s_sSensor));
    	oItem.setSBoundingBox(asProperties.get(s_sBoundingBox));
    	oItem.setSUrl(asProperties.get(s_sUrl));
    	oItem.setSPlatform(asProperties.get(s_sPlatform));
    	
    	// set start and end dateTime
    	Long lStartDateTime = parseDate(asProperties.get(s_sStartDate), asProperties.get(s_sStartTime));
    	Long lEndDateTime = parseDate(asProperties.get(s_sEndDate), asProperties.get(s_sEndTime));
    	
    	double dStarDateTime = 0.0;
    	double dEndDateTime = 0.0;
    	
    	if (lStartDateTime != null) 
    		dStarDateTime = lStartDateTime.doubleValue();
    	else 
    		WasdiLog.debugLog("MODISUtils.buildModisItem. Starting time for product is null");
    	
    	if (lEndDateTime != null) 
    		dEndDateTime = lEndDateTime.doubleValue();
    	else 
    		WasdiLog.debugLog("MODISUtils.buildModisItem. Ending time for product is null");
    	
    	oItem.setDStartDate(dStarDateTime);
    	oItem.setDEndDate(dEndDateTime);
    	
    	return oItem;
    }
    
    /**
     * Parse the XML string representing the metadata of a product and populate a map with the metadata of interest
     * @param sXMLString the string representation of the XML file read from the data provider and containing the metadata of a product
     * @param sProductFileUrl the URL where the product is stored, on the data provider
     * @return
     */
    private static Map<String, String> parseXML(String sXMLString, String sProductFileUrl) {
    	Map<String, String> asProperties = new HashMap<>();
    	
    	List<String> asPropertyNames = Arrays.asList(s_sFileName, s_sFileSize, s_sDayNightFlag, s_sEndTime, s_sEndDate,
    			s_sStartTime, s_sStartDate, s_sInstrument, s_sSensor, s_sPlatform);
		
		// set the bounding box
		String sBoundingBox = readBoundingBox(sXMLString);
		asProperties.put(s_sBoundingBox, String.join(",", sBoundingBox));
		
		// set the product url
		asProperties.put(s_sUrl, sProductFileUrl);
		
    	// set all other properties
		asPropertyNames.forEach(sProperty -> {
			asProperties.put(sProperty, readProperty(sXMLString, sProperty));
		});
		
		return asProperties;
    }
    
    /**
     * Given a string representing the XML content of a MODIS metadata file, extracts the value associated to a specific tag
     * @param sXml the string representing the XML content
     * @param sPropertyName the name of a tag (without the opening and closure marks)
     * @return the value associated to the tag
     */
	public static String readProperty(String sXml, String sPropertyName) {
		String sValue = null;

		if (sXml.contains(sPropertyName)) {
			int iLengthofTagOpen = ("<" + sPropertyName + ">").length();
			int iIndexOfTagOpen = sXml.indexOf("<" + sPropertyName + ">");
			int iIndexOfTagClose = sXml.indexOf("</" + sPropertyName + ">");

			sValue = sXml.substring(iIndexOfTagOpen + iLengthofTagOpen, iIndexOfTagClose);
		}

		return sValue;
	}
    
	/**
	 * Given a string representing the XML content of a MODIS metadata file, extracts the the bounding box
	 * @param sXMLString the string representing the XML content
	 * @return the JSON string representing the bounding box, in the format required to store the information on Mongo
	 */
	private static String readBoundingBox(String sXMLString) {
		String sBoundingBoxProperty =  readProperty(sXMLString, s_sBoundary);
		
		if (sBoundingBoxProperty == null) 
			sBoundingBoxProperty = "";
		
		String[] asLines = sBoundingBoxProperty.split("\n");
		String sLongitudeOpenTag = "<" + s_sLongitude + ">";
		String sLongitueCloseTag = sLongitudeOpenTag.replace("<", "</");
		String sLatitudeOpenTag = "<" + s_sLatitude + ">";
		String sLatitudeCloseTag = sLatitudeOpenTag.replace("<", "</");
		
		List<Double> adLongitude = new ArrayList<>();
		List<Double> adLatitude = new ArrayList<>();
		// read longitude and latitude for each point in the xml string
		for (String sLine : asLines) {
			sLine = sLine.trim();
			if ( sLine.startsWith(sLongitudeOpenTag) && sLine.endsWith(sLongitueCloseTag))
				adLongitude.add(Double.parseDouble(sLine.replace(sLongitudeOpenTag, "").replace(sLongitueCloseTag, "")));
			else if (sLine.startsWith(sLatitudeOpenTag) && sLine.endsWith(sLatitudeCloseTag)) 
				adLatitude.add(Double.parseDouble(sLine.replace(sLatitudeOpenTag, "").replace(sLatitudeCloseTag, "")));
		}
		
		return getJsonFormatBoundingBox(adLatitude, adLongitude);
	}
	
	/**
	 * Given the arrays of latitude and longitude points, determine the bounding box of the points and returns the 
	 * JSON string to store in the db for the corresponding polygon
	 * @param adLatitude array of latitude points
	 * @param adLongitude array of longitude points
	 * @return the JSON string representing the bounding box of the points, to be stored in the db
	 */
	private static String getJsonFormatBoundingBox(List<Double> adLatitude, List<Double> adLongitude) {
		double dMaxLat = Collections.max(adLatitude); // north
		double dMinLat = Collections.min(adLatitude); // south
		double dMaxLong = Collections.max(adLongitude); // east
		double dMinLong = Collections.min(adLongitude); // west
		
		// coordinates: WN, EN, ES, WS, WN
		String sCoordinates = "[[ [" 
				+ dMinLong + ", " + dMaxLat + "], [" 
				+ dMaxLong +", " + dMaxLat + "], [" 
				+ dMaxLong + ", " + dMinLat + "] , [" 
				+ dMinLong + ", " + dMinLat + "], [" 
				+ dMinLong + ", " + dMaxLat + "] ]]"; 
		
		String sLocationJson = "{\"type\": \"Polygon\", \"coordinates\": " + sCoordinates +"}";
		
		return sLocationJson;
	}

    
    /**
     * Returns an input stream for a designated resource on a URS authenticated remote server.
     * This is where the access to the LPDAAC data provider is done.
     * @param sResource the URL to access
     * @param sUsername username for authentication on URS EARTHDATA by NASA
     * @param sPassword password for authentication on URS EARTHDATA by NASA
     * @return the input stream for the resource
     */
    public static InputStream getResource(String sResource, String sUsername, String sPassword) throws Exception {
        int iRedirects = 0;
        while( iRedirects < 20 ) {
            ++iRedirects;

            URL oUrl = new URL(sResource);
            HttpURLConnection oConnection = (HttpURLConnection) oUrl.openConnection();
            oConnection.setRequestMethod("GET");
            oConnection.setInstanceFollowRedirects(false);
            oConnection.setUseCaches(false);
            oConnection.setDoInput(true);
 
            // If this is the URS server, add in the authentication header.
            if( sResource.startsWith(s_URSUrl) ) {
            	oConnection.setRequestProperty(
                    "Authorization",
                    "Basic " + Base64.getEncoder().encodeToString((sUsername + ":" + sPassword).getBytes()));
            }
 

            int status = oConnection.getResponseCode();
            if( status == 200 ) {
                WasdiLog.debugLog("MODISUtils.getResource. Successfully connected, opening the input stream after " + iRedirects + " redirections");
            	return oConnection.getInputStream();
            }
 
            // status codes other than 302 (a redirect) are not accepted
            if( status != 302 ) {
                throw new Exception("MODISUtils.getResource. Invalid response from server - status " + status);
            }
 
            // Get the redirection location and continue. 
            sResource = oConnection.getHeaderField("Location");
            if (sResource == null) {
            	throw new Exception("MODISUtils.getResource. Redirection location is null. Impossible to continue");
            } else {
                WasdiLog.debugLog("MODISUtils.getResource. Got a redirection to: " + sResource);
            }
        }
 
        // If we get here, we exceeded our redirect limit
        throw new Exception("MODISUtils.getResource. Redirection limit exceeded");
    }
    


}
