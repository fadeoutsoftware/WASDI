import java.io.BufferedReader;
import java.io.FileReader;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import wasdi.shared.business.modis11a2.ModisItem;
import wasdi.shared.utils.S3BucketUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.data.modis11a2.ModisRepository;

public class TryMODIS {
	
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
	private static final String s_sLatitude = "PointLongitude";
	private static final String s_sLongitude = "PointLatitude";
	private static final String s_sPlatform = "PlatformShortName";
	
	// KEY TO ACCESS THE PARAMETERS MAP
	private static final String s_sBoundingBox = "BoundingBox"; // the corners of the boudning box will be stored in the following order
	private static final String s_sUrl = "url";
	
	
//	System.out.println("Local Granule ID (0): " + asMetadata[0]);
//	System.out.println("Entity id (1): " + asMetadata[1]);
//	System.out.println("Acquisition Start date (2): " + sStartDate);
//	System.out.println("Acquisition End date (3): " + asMetadata[3]);
//	System.out.println("NW corner lat (28): " + asMetadata[28]);
//	System.out.println("NW corner long (29): " + asMetadata[29]);
//	System.out.println("NE corner lat (30): " + asMetadata[30]);
//	System.out.println("NE corner long (31): " + asMetadata[31]);
//	System.out.println("SE corner lat (32): " + asMetadata[32]);
//	System.out.println("SE corner long (33): " + asMetadata[33]);
//	System.out.println("SW corner lat (34): " + asMetadata[34]);
//	System.out.println("SW corner long (35): " + asMetadata[35]);

    /*
     * Prefix used to identify redirects to URS for the purpose of
     * adding authentication headers. For test, this should be:
     * https://uat.urs.earthdata.nasa.gov for test
     */
    static String URS = "https://urs.earthdata.nasa.gov";
  
 
    /*
     * Simple test.
     */
    public static void main( String[] args ) throws Exception {
    	// tryDownload();
    	tryReadMetadata();
    	
    }
    
    public static void tryReadMetadata() throws Exception {
    	final String sCSVFilePath = "C:/Users/valentina.leone/Desktop/WORK/MODIS_MODA/modis_mod11a2_v61_64f584ce5c8845f6/modis_mod11a2_v61_64f584ce5c8845f6.csv";
    	String sBaseUrl = "https://e4ftl01.cr.usgs.gov/MOLT/MOD11A2.061/";
    	
    	BufferedReader oReader = null;
    	
    	ModisRepository oModisRepo = new ModisRepository();
    	
    	try {
    		oReader = new BufferedReader(new FileReader(sCSVFilePath));  
    		String sLine = "";
    		int iCont = 0;
    		while ((sLine = oReader.readLine()) != null && iCont < 2) {  
    			String[] asMetadata = sLine.split(","); 
    			String sGranuleId = asMetadata[0];
    			String sStartDate = asMetadata[2].replaceAll("/", ".");

    			iCont ++;
    			
    			String sDirectoryUrl = sBaseUrl + sStartDate;
    			String sProductFileUrl = sDirectoryUrl + "/" + sGranuleId;
    			String sXMLUrlPath = sProductFileUrl + ".xml";
    			System.out.println("XML file url: " + sXMLUrlPath + "\n");
    			
    			if (iCont - 1 > 0) {
    				
    				String sXMLMetadataString = readXmlFile(sXMLUrlPath); // TODO: handle Exception
    				if (!Utils.isNullOrEmpty(sXMLMetadataString)) {
    					Map<String, String> asProperties = parseXML(sXMLMetadataString, sProductFileUrl);
    					asProperties.entrySet().stream().forEach(oEntry -> System.out.println(oEntry.getKey() + ": " + oEntry.getValue()));
    					ModisItem oItem = buildModisItem(asProperties);
    					oModisRepo.insertModisItem(oItem);
    				}
    				//tryDownloadFile(sDirectoryUrl + "/" + sGranuleId);
    			}
    		}  
    		oReader.close();
    	} catch (IOException e) {  
    		e.printStackTrace();
    		if (oReader != null) {
    			oReader.close();
    		}
    	}  
    	
    }
    
    
    public static void tryDownloadFile(String sUrlFile) throws Exception {
    	String sUsername = "*";
        String sPassword = "*";
        System.out.println("Accessing resource " + sUrlFile);
        
            /*
             * Set up a cookie handler to maintain session cookies. A custom
             * CookiePolicy could be used to limit cookies to just the resource
             * server and URS.
             */
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        
            
           System.out.println("Try to download file: " + sUrlFile);
           
           InputStream inputStream = null;
         
        try {
        	inputStream = getResource(sUrlFile, sUsername, sPassword);
	
	        // Check if the save directory exists; if not, create it
	        String savingDir = "C:/Users/valentina.leone/Desktop/WORK/MODIS_MODA";
	        File oSaveDir = new File(savingDir);


            String saveName = oSaveDir + File.separator + sUrlFile.substring(sUrlFile.lastIndexOf('/') + 1).trim();
            Path outputPath = Paths.get(saveName);

            // Download the file using NIO
            Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Downloaded file: " + saveName);
            
            inputStream.close();
       } catch (IOException e) {
        e.printStackTrace();
        if (inputStream != null)
        	inputStream.close();
       }
    }
    
    
    public static String readXmlFile(String sXMLUrlFile) throws IOException {
        String sUsername = "*";						// TODO: put this in the config file
        String sPassword = "*";				// TODO: put this in the config file
        System.out.println("Accessing resource " + sXMLUrlFile);
        
        InputStream oIn = null;
        BufferedReader oBin = null;
        String sXMLMetadata = "";
        try {
            /* Set up a cookie handler to maintain session cookies. */
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
 
            /* Retrieve a stream for the resource */
            oIn = getResource(sXMLUrlFile, sUsername, sPassword);
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
	
    
    private static ModisItem buildModisItem(Map<String, String> asProperties) {
    	ModisItem oItem = new ModisItem();
    	
    	oItem.setSFileName(asProperties.get(s_sFileName));
    	oItem.setLFileSize(Long.parseLong(asProperties.get(s_sFileSize)));
    	oItem.setSDayNightFlag(asProperties.get(s_sDayNightFlag));
    	oItem.setSInstrument(asProperties.get(s_sInstrument));
    	oItem.setSSensor(asProperties.get(s_sSensor));
    	oItem.setSBoundingBox(asProperties.get(s_sBoundingBox));
    	oItem.setSUrl(asProperties.get(s_sUrl));
    	oItem.setSPlatform(asProperties.get(s_sPlatform));
    	
    	//data
    	Long lStartDateTime = parseDate(asProperties.get(s_sStartDate), asProperties.get(s_sStartTime));
    	Long lEndDateTime = parseDate(asProperties.get(s_sEndDate), asProperties.get(s_sEndTime));
    	
    	double dStarDateTime = 0.0;
    	double dEndDateTime = 0.0;
    	
    	if (lStartDateTime != null) dStarDateTime = lStartDateTime.doubleValue();
    	else WasdiLog.errorLog("Starting time for product is null");
    	
    	if (lEndDateTime != null) dEndDateTime = lEndDateTime.doubleValue();
    	else WasdiLog.errorLog("Ending time for product is null");
    	
    	oItem.setDStartDate(dStarDateTime);
    	oItem.setDEndDate(dEndDateTime);
    	
    	return oItem;
    }
    
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
		
		List<String> asCoordinates = new ArrayList<>();
		// read longitude and latitude for each point in the xml string
		for (String sLine : asLines) {
			sLine = sLine.trim();
			if ( sLine.startsWith(sLongitudeOpenTag) && sLine.endsWith(sLongitueCloseTag)) 
				asCoordinates.add(sLine.replace(sLongitudeOpenTag, "").replace(sLongitueCloseTag, ""));
			else if (sLine.startsWith(sLatitudeOpenTag) && sLine.endsWith(sLatitudeCloseTag)) 
				asCoordinates.add(sLine.replace(sLatitudeOpenTag, "").replace(sLatitudeCloseTag, ""));
		}
		
		// repeat the latitude and the longitude of the last point, to close the bounding box
		if (asCoordinates.size() > 1) {
	    	asCoordinates.add(asCoordinates.get(0));
	    	asCoordinates.add(asCoordinates.get(1));
		} else {
			WasdiLog.debugLog("Coodinate list does not contain all the points. Cannot close the bounding box");
		}
		
		return String.join(",", asCoordinates);
	}
	
    public static void tryDownload() {
        String resource = "https://e4ftl01.cr.usgs.gov/MOLT/MOD11A2.061/2000.05.16/MOD11A2.A2000137.h34v10.061.2020045132244.hdf.xml";
        String username = "*";
        String password = "*";
        System.out.println("Accessing resource " + resource);
        try
        {
            /*
             * Set up a cookie handler to maintain session cookies. A custom
             * CookiePolicy could be used to limit cookies to just the resource
             * server and URS.
             */
            CookieHandler.setDefault(
                new CookieManager(null, CookiePolicy.ACCEPT_ALL));
 
            /* Retreve a stream for the resource */
            InputStream in = getResource(resource, username, password);
            /* Dump the resource out (not a good idea for binary data) */
            BufferedReader bin = new BufferedReader(
                new InputStreamReader(in));
            String line;
            while( (line = bin.readLine()) != null)
            {
                System.out.println(line);
            }
            bin.close();
        }
        catch( Exception t)
        {
            System.out.println("ERROR: Failed to retrieve resource");
            System.out.println(t.getMessage());
            t.printStackTrace();
        }
    }
  
 
    /*
     * Returns an input stream for a designated resource on a URS
     * authenticated remote server.
     *
     */
    public static InputStream getResource(String sResource, String sUsername, String sPassword) throws Exception {
        int iRedirects = 0;
        while( iRedirects < 10 ) {
            ++iRedirects;
            /*
             * Configure a connection to the resource server and submit the  request for our resource.
             */
            URL oUrl = new URL(sResource);
            HttpURLConnection oConnection = (HttpURLConnection) oUrl.openConnection();
            oConnection.setRequestMethod("GET");
            oConnection.setInstanceFollowRedirects(false);
            oConnection.setUseCaches(false);
            oConnection.setDoInput(true);
 
            /*
             * If this is the URS server, add in the authentication header.
             */
            if( sResource.startsWith(URS) )
            {
            	oConnection.setRequestProperty(
                    "Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(
                        (sUsername + ":" + sPassword).getBytes()));
            }
 

            int status = oConnection.getResponseCode();
            if( status == 200 )
                return oConnection.getInputStream();
 
            /*
             * Any value other than 302 (a redirect) will need some custom
             * handling. A 401 from URS means that the credentials
             * are invalid, while a 403 means that the user has not authorized
             * the application.
             */
            if( status != 302 ) {
                throw new Exception( "Invalid response from server - status " + status);
            }
 
            /*
             * Get the redirection location and continue. This should really
             * have a null check, just in case.
             */
            sResource = oConnection.getHeaderField("Location");
        }
 
        /*
         * If we get here, we exceeded our redirect limit. This is most likely
         * a configuration problem somewhere in the remote server.
         */
        throw new Exception("Redirection limit exceeded");
    }

}
