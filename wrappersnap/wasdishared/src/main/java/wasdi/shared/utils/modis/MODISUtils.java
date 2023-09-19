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
	private static final String s_sBoundingBox = "BoundingBox"; // the corners of the bounding box will be stored in the following order
	private static final String s_sUrl = "url";
	

    /*
     * Prefix used to identify redirects to URS for the purpose of adding authentication headers. For test, this should be:
     * https://uat.urs.earthdata.nasa.gov for test
     */
    private static final String s_URSUrl = "https://urs.earthdata.nasa.gov";
    
	private static final String s_sMODISBaseUrl = "https://e4ftl01.cr.usgs.gov/MOLT/MOD11A2.061/";

  
 

    public static void main( String[] args ) throws Exception {
    	final String sCSVFilePath = "C:/Users/valentina.leone/Desktop/WORK/MODIS_MODA/modis_mod11a2_v61_64f584ce5c8845f6/modis_mod11a2_v61_64f584ce5c8845f6.csv";
    	insertProducts();
    }
    
    /**
     * Populate the collection on Mongo
     * @param sCSVFilePath the path to the CSV file containing the list of product names
     * @return
     * @throws Exception
     */
    public static void insertProducts() throws Exception {
    	BufferedReader oReader = null;
    	ModisRepository oModisRepo = new ModisRepository();
    	int iProdCounts = 0;
    	
    	try {
    		oReader = new BufferedReader(new FileReader(s_sCSVFilePath));  
    		String sLine = "";
    		int iCont = 0;
    		
    		while ((sLine = oReader.readLine()) != null) {
    			iCont ++;
    			
    			String[] asMetadata = sLine.split(","); 
    			String sGranuleId = asMetadata[0];
    			String sStartDate = asMetadata[2].replaceAll("/", ".");
    			
    			String sProductDirectoryUrl = s_sMODISBaseUrl + sStartDate;
    			String sProductFileUrl = sProductDirectoryUrl + "/" + sGranuleId;
    			String sXMLUrlPath = sProductFileUrl + ".xml";
    			WasdiLog.debugLog("MODISUtils.insertProducts: tring to read XML metadata file at URL: " + sXMLUrlPath);
    			
    			
    			try {
	    			if (iCont - 1 > 0) {
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
    			oReader.close();
    		WasdiLog.debugLog("MODISUtils.insertProducts. Number of products added to the db: " + iProdCounts);
    	}	
    }
    
    
//    public static void tryDownloadFile(String sUrlFile) throws Exception {
//    	String sUsername = "*";
//        String sPassword = "*";
//        System.out.println("Accessing resource " + sUrlFile);
//        
//            /*
//             * Set up a cookie handler to maintain session cookies. A custom
//             * CookiePolicy could be used to limit cookies to just the resource
//             * server and URS.
//             */
//            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
//        
//            
//           System.out.println("Try to download file: " + sUrlFile);
//           
//           InputStream inputStream = null;
//         
//        try {
//        	inputStream = getResource(sUrlFile, sUsername, sPassword);
//	
//	        // Check if the save directory exists; if not, create it
//	        String savingDir = "C:/Users/valentina.leone/Desktop/WORK/MODIS_MODA";
//	        File oSaveDir = new File(savingDir);
//
//
//            String saveName = oSaveDir + File.separator + sUrlFile.substring(sUrlFile.lastIndexOf('/') + 1).trim();
//            Path outputPath = Paths.get(saveName);
//
//            // Download the file using NIO
//            Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
//
//            System.out.println("Downloaded file: " + saveName);
//            
//            inputStream.close();
//       } catch (IOException e) {
//        e.printStackTrace();
//        if (inputStream != null)
//        	inputStream.close();
//       }
//    }
    
    
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
        	WasdiLog.errorLog("Error while trying to read the metadata file: " + sXMLUrlFile + ". " + oEx.getMessage());
        } finally {
        	if (oIn != null)
        		oIn.close();
        	if (oBin != null) 
        		oBin.close();
        }
    	return sXMLMetadata;     
    }
    
    
	private static Long parseDate(String sDate, String sTime) {
		String sComposedDate = sDate + "T" + sTime.substring(0, 12) + "Z";
		return TimeEpochUtils.fromDateStringToEpoch(sComposedDate);
	}
	
    
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
     * Parse the XML string containing the metadata and populate a map
     * @param sXMLString
     * @param sProductFileUrl
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
    
	private static String readBoundingBox(String sXMLString) {
		String sBoundingBoxProperty =  readProperty(sXMLString, s_sBoundary);
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

    
    /*
     * Returns an input stream for a designated resource on a URSauthenticated remote server.
     */
    public static InputStream getResource(String sResource, String sUsername, String sPassword) throws Exception {
        int iRedirects = 0;
        while( iRedirects < 10 ) {
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
                WasdiLog.debugLog("MODISUtils.getResource. Successfully connected, opening the input stream");
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
