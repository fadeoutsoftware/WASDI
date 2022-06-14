package wasdi.dbutils.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;

import wasdi.shared.business.ecostress.EcoStressItem;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ecostress.EcoStressRepository;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;

public final class S3BucketHelper {

	private static final AWSCredentials m_oCredentials;
	private static final AmazonS3 m_oConn;

	static {
		m_oCredentials = new BasicAWSCredentials(WasdiConfig.Current.s3Bucket.accessKey, WasdiConfig.Current.s3Bucket.secretKey);

		m_oConn = new AmazonS3Client(m_oCredentials);
		m_oConn.setEndpoint(WasdiConfig.Current.s3Bucket.endpoint);
	}

	private S3BucketHelper() {
		throw new java.lang.UnsupportedOperationException("This is a helper class and cannot be instantiated");
	}

	public static void parseS3Bucket() {
		String sBucketName = WasdiConfig.Current.s3Bucket.bucketName;
		String sFolders = WasdiConfig.Current.s3Bucket.folders;

		String[] asFolders = sFolders.split(", ");

		for (String sFolder : asFolders) {
			parseFolder(sBucketName, sFolder);
		}
	}

	public static void parseFolder(String sBucketName, String sFolderPath) {
		System.out.println("parseFolder | bucketName: " + sBucketName + " | folderPath: " + sFolderPath);

		EcoStressRepository oEcoStressRepository = new EcoStressRepository();

		Date oStartDate = new Date();

		ObjectListing oObjectListing = m_oConn.listObjects(sBucketName, sFolderPath);
		int iCounter = -1;

		do {
			for (S3ObjectSummary oS3ObjectSummary : oObjectListing.getObjectSummaries()) {
				EcoStressItem oItem = parseEntry(sBucketName, oS3ObjectSummary.getKey());
				
				if (oItem != null) {
					oEcoStressRepository.insertEcoStressItem(oItem);
					iCounter++;					
				}
			}
			oObjectListing = m_oConn.listNextBatchOfObjects(oObjectListing);
		} while (oObjectListing.getObjectSummaries().size() != 0);

		Date oEndDate = new Date();
		long lMillis = oEndDate.getTime() - oStartDate.getTime();
		System.out.println("seconds: " + (lMillis / 1_000) + " (millis: " + lMillis + ")");

		System.out.println("counter: " + iCounter);
	}

	private static EcoStressItem parseEntry(String sBucketName, String sEntryKey) {
		String[] asTokens = sEntryKey.split("/");

		if (asTokens.length < 4) {
			return null;
		}

		String sDirectoryName = asTokens[0] + "/" + asTokens[1];
		String sFileName = asTokens[3].substring(0, asTokens[3].length() - 4);

		String sXmlFilePath = sDirectoryName + "/xml/" + sFileName + ".xml";
		String sH5FilePath = sDirectoryName + "/" + sFileName;

		String sUrl = WasdiConfig.Current.s3Bucket.endpoint + sBucketName + "/"+ sDirectoryName + "/" + sFileName;

		String sXml = readFile(sBucketName, sXmlFilePath);

		Map<String, String> asProperties = parseXml(sXml);

		EcoStressItem oItem = buildEcoStressItem(asProperties, sFileName, sH5FilePath, sUrl);

		return oItem;
	}

	private static EcoStressItem buildEcoStressItem(Map<String, String> asProperties, String sFileName, String sS3Path, String sUrl) {
		EcoStressItem oItem = new EcoStressItem();

		oItem.setFileName(sFileName);

		oItem.setStartOrbitNumber(Integer.parseInt(asProperties.get("StartOrbitNumber")));
		oItem.setStopOrbitNumber(Integer.parseInt(asProperties.get("StopOrbitNumber")));

		oItem.setDayNightFlag(asProperties.get("DayNightFlag"));

		Long beginningDate = parseDate(asProperties.get("RangeBeginningDate"), asProperties.get("RangeBeginningTime"));
		if (beginningDate != null) {
			oItem.setBeginningDate(beginningDate.doubleValue());
		}

		Long endingDate = parseDate(asProperties.get("RangeEndingDate"), asProperties.get("RangeEndingTime"));
		if (endingDate != null) {
			oItem.setEndingDate(beginningDate.doubleValue());
		}

		String sImageFootPrint = extractFootprint(asProperties);
		oItem.setLocation(sImageFootPrint);

		oItem.setPlatform(asProperties.get("PlatformShortName"));
		oItem.setInstrument(asProperties.get("InstrumentShortName"));
		oItem.setSensor(asProperties.get("SensorShortName"));
		oItem.setParameterName(asProperties.get("ParameterName"));

		oItem.setS3Path(sS3Path);
		oItem.setUrl(sUrl);

		return oItem;
	}

	private static String extractFootprint(Map<String, String> asProperties) {
		try {
			String sWest = asProperties.get("WestBoundingCoordinate");
			String sNorth = asProperties.get("NorthBoundingCoordinate");
			String sEast = asProperties.get("EastBoundingCoordinate");
			String sSouth = asProperties.get("SouthBoundingCoordinate");

			if (sWest == null || sNorth == null || sEast == null || sSouth == null) {
				return null;
			}

			double dWest = Double.parseDouble(sWest);
			double dNorth = Double.parseDouble(sNorth);
			double dEast = Double.parseDouble(sEast);
			double dSouth = Double.parseDouble(sSouth);
			
			/*
			double adPoint1[] = { dWest, dNorth };
			double adPoint2[] = { dWest, dSouth };
			double adPoint3[] = { dEast, dSouth };
			double adPoint4[] = { dEast, dNorth };
			double adPoint5[] = { dWest, dNorth };
			
			JSONArray oPoint1 = new JSONArray(adPoint1);
			JSONArray oPoint2 = new JSONArray(adPoint2);
			JSONArray oPoint3 = new JSONArray(adPoint3);
			JSONArray oPoint4 = new JSONArray(adPoint4);
			JSONArray oPoint5 = new JSONArray(adPoint5);
			
			JSONArray oFirstArray = new JSONArray();
			oFirstArray.put(oPoint1);
			oFirstArray.put(oPoint2);
			oFirstArray.put(oPoint3);
			oFirstArray.put(oPoint4);
			oFirstArray.put(oPoint5);
			
			JSONArray oSecondArray = new JSONArray();
			oSecondArray.put(oFirstArray);
			
			
			JSONObject oLocation = new JSONObject();
			oLocation.put("type", "Polygon");
			oLocation.put("coordinates", oSecondArray);
			*/
			
			String sCoordinates = "[[ [" +dWest + ", " + dNorth + "], [" + dWest +", " + dSouth + "], [" + dEast + ", " + dSouth + "] , [" +  dEast + ", " + dNorth + "], [" +dWest + ", " + dNorth + "] ]]"; 
			
			String sLocationJson = "{\"type\": \"Polygon\", \"coordinates\": " + sCoordinates +"}";

			return sLocationJson;
			
			//return oLocation;
		} catch (Exception oEx) {
			System.out.println("asProperties: " + asProperties);
			oEx.printStackTrace();
		}

		return null;
	}

	private static String readFile(String sBucketName, String sFilePath) {
		S3Object oS3Object = m_oConn.getObject(
				new GetObjectRequest(sBucketName, sFilePath)
		);

		S3ObjectInputStream oS3ObjectInputStream = oS3Object.getObjectContent();
		String sXml = s3ObjectInputStreamToString(oS3ObjectInputStream);

		return sXml;
	}

	public static String downloadFile(String sBucketName, String sFilePath, String sDownloadPath) {
		m_oConn.getObject(
				new GetObjectRequest(sBucketName, sFilePath),
				new File(sDownloadPath)
		);

		return sDownloadPath + "/" + sFilePath;
	}

	public static long getFileSize(String sBucketName, String sFilePath) {
		ObjectMetadata objectMetadata = m_oConn.getObjectMetadata(
				new GetObjectMetadataRequest(sBucketName, sFilePath)
		);

		long lInstanceLength = objectMetadata.getInstanceLength();

		return lInstanceLength;
	}

	private static String s3ObjectInputStreamToString(S3ObjectInputStream s3ObjectInputStream) {
		try {
			String xml = inputStreamToString(s3ObjectInputStream);

			return xml;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Map<String, String> parseXmlFile(String sFilePath) {
		String sXml = fileToString(sFilePath);

		return parseXml(sXml);
	}

	public static Map<String, String> parseXml(String sXml) {
		Map<String, String> asProperties = new HashMap<>();

		List<String> asPropertyNames = Arrays.asList("PlatformShortName", "InstrumentShortName", "SensorShortName", "ParameterName",
				"WestBoundingCoordinate", "NorthBoundingCoordinate", "EastBoundingCoordinate", "SouthBoundingCoordinate",
				"RangeBeginningDate", "RangeBeginningTime", "RangeEndingDate", "RangeEndingTime",
				"StartOrbitNumber", "StopOrbitNumber",
				"DayNightFlag");

		asPropertyNames.forEach(s -> {
			asProperties.put(s, readProperty(sXml, s));
		});

		return asProperties;
	}

	private static String readProperty(String sXml, String sPropertyName) {
		String sValue = null;

		if (sXml.contains(sPropertyName)) {
			int iLengthofTagOpen = ("<" + sPropertyName + ">").length();
			int iIndexOfTagOpen = sXml.indexOf("<" + sPropertyName + ">");
			int iIndexOfTagClose = sXml.indexOf("</" + sPropertyName + ">");

			sValue = sXml.substring(iIndexOfTagOpen + iLengthofTagOpen, iIndexOfTagClose);
		}

		return sValue;
	}

	private static String fileToString(String sFilePath) {
		try {
			File oFile = new File(sFilePath);
			String sXml = inputStreamToString(new FileInputStream(oFile));

			return sXml;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return null;
	}

	public static String inputStreamToString(InputStream is) throws IOException {
		StringBuilder oSb = new StringBuilder();
		String sLine;
		BufferedReader oBr = new BufferedReader(new InputStreamReader(is));
		while ((sLine = oBr.readLine()) != null) {
			oSb.append(sLine);
		}
		oBr.close();
		return oSb.toString();
	}

	private static Long parseDate(String sDate, String sTime) {
		String sComposedDate = sDate + "T" + sTime.substring(0, 12) + "Z";

		return TimeEpochUtils.fromDateStringToEpoch(sComposedDate);
	}
	
	public static void importEcoStressFolder(String sFolder) {
		
		try {
			
			String [] asFiles;
			
			EcoStressRepository oEcoStressRepository = new EcoStressRepository();
			
			File oXmlFolder = new File(sFolder);
			
			asFiles = oXmlFolder.list();
			
			for (String sFile : asFiles) {
				
				File oActualFile = new File(sFolder+sFile);
				
				Map<String, String> asProperties = parseXmlFile(sFolder+sFile);
				//EcoStressItem oItem = buildEcoStressItem(asProperties, sFileName, sH5FilePath, sUrl);
			}
		} 
		catch (Exception e) {
	          e.printStackTrace();
	    }		
	}
	
	public static void importEcoStress(String sLocalFolder) {
		
		if (Utils.isNullOrEmpty(sLocalFolder)) {
			parseS3Bucket();
		}
		else {
			importEcoStressFolder(sLocalFolder);
		}
	}	

}
