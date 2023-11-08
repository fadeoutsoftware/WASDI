package wasdi.shared.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.AwsHostNameUtils;

//import wasdi.shared.business.ecostress.EcoStressItem;
import wasdi.shared.business.ecostress.EcoStressItemForWriting;
//import wasdi.shared.business.ecostress.EcoStressLocation;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ecostress.EcoStressRepository;
import wasdi.shared.utils.log.WasdiLog;

public final class S3BucketUtils {

	private static final AmazonS3 m_oConn;

	static {
		AWSCredentials oCredentials = new BasicAWSCredentials(WasdiConfig.Current.s3Bucket.accessKey,
				WasdiConfig.Current.s3Bucket.secretKey);

		String sEndpoint = WasdiConfig.Current.s3Bucket.endpoint;

		m_oConn = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(oCredentials))
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(sEndpoint,
						AwsHostNameUtils.parseRegion(sEndpoint, AmazonS3Client.S3_SERVICE_NAME)))
				.build();
	}

	private S3BucketUtils() {
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
				EcoStressItemForWriting oItem = parseEntry(sBucketName, oS3ObjectSummary.getKey());
				
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

	private static EcoStressItemForWriting parseEntry(String sBucketName, String sEntryKey) {
		String[] asTokens = sEntryKey.split("/");

		if (asTokens.length < 3) {
			return null;
		}

		String sDirectoryName = "";
		String sFileName = "";
		
		if (asTokens.length>=4) {
			sDirectoryName = asTokens[0] + "/" + asTokens[1];
			sFileName = asTokens[3].substring(0, asTokens[3].length() - 4);
			
		}
		else {
			sDirectoryName = asTokens[0];
			sFileName = asTokens[2].substring(0, asTokens[2].length() - 4);			
		}

		String sXmlFilePath = sDirectoryName + "/xml/" + sFileName + ".xml";
		String sH5FilePath = sDirectoryName + "/" + sFileName;

		String sUrl = WasdiConfig.Current.s3Bucket.endpoint + sBucketName + "/"+ sDirectoryName + "/" + sFileName;

		String sXml = readFile(sBucketName, sXmlFilePath);

		Map<String, String> asProperties = parseXml(sXml);

		EcoStressItemForWriting oItem = buildEcoStressItem(asProperties, sFileName, sH5FilePath, sUrl);

		return oItem;
	}

	private static EcoStressItemForWriting buildEcoStressItem(Map<String, String> asProperties, String sFileName, String sS3Path, String sUrl) {
		EcoStressItemForWriting oItem = new EcoStressItemForWriting();

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

//		EcoStressLocation oImageFootPrint = extractFootprint(asProperties);
//		oItem.setLocation(oImageFootPrint);

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

			String sCoordinates = "[[ [" +dWest + ", " + dNorth + "], [" + dWest +", " + dSouth + "], [" + dEast + ", " + dSouth + "] , [" +  dEast + ", " + dNorth + "], [" +dWest + ", " + dNorth + "] ]]"; 

			String sLocationJson = "{\"type\": \"Polygon\", \"coordinates\": " + sCoordinates +"}";

			return sLocationJson;
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3BucketUtils.extractFootprint: error", oEx);
		}

		return null;
	}

//	private static EcoStressLocation extractFootprint(Map<String, String> asProperties) {
//		try {
//			String sWest = asProperties.get("WestBoundingCoordinate");
//			String sNorth = asProperties.get("NorthBoundingCoordinate");
//			String sEast = asProperties.get("EastBoundingCoordinate");
//			String sSouth = asProperties.get("SouthBoundingCoordinate");
//
//			if (sWest == null || sNorth == null || sEast == null || sSouth == null) {
//				return null;
//			}
//
//			double dWest = Double.parseDouble(sWest);
//			double dNorth = Double.parseDouble(sNorth);
//			double dEast = Double.parseDouble(sEast);
//			double dSouth = Double.parseDouble(sSouth);
//
////			String sCoordinates = "[[ [" +dWest + ", " + dNorth + "], [" + dWest +", " + dSouth + "], [" + dEast + ", " + dSouth + "] , [" +  dEast + ", " + dNorth + "], [" +dWest + ", " + dNorth + "] ]]"; 
////
////			String sLocationJson = "{\"type\": \"Polygon\", \"coordinates\": " + sCoordinates +"}";
//
//			EcoStressLocation oEcoStressLocation = new EcoStressLocation();
//			oEcoStressLocation.setType("Polygon");
//
//			List<Double> aoPoint_1 = Arrays.asList(dWest, dNorth);
//			List<Double> aoPoint_2 = Arrays.asList(dWest, dSouth);
//			List<Double> aoPoint_3 = Arrays.asList(dEast, dSouth);
//			List<Double> aoPoint_4 = Arrays.asList(dEast, dNorth);
//			List<Double> aoPoint_5 = Arrays.asList(dWest, dNorth);
//
//			List<List<Double>> aoPolygon = new ArrayList<>();
//			aoPolygon.add(aoPoint_1);
//			aoPolygon.add(aoPoint_2);
//			aoPolygon.add(aoPoint_3);
//			aoPolygon.add(aoPoint_4);
//			aoPolygon.add(aoPoint_5);
//
//			List<List<List<Double>>> aoCoordinates = new ArrayList<>();
//			aoCoordinates.add(aoPolygon);
//
//			oEcoStressLocation.setCoordinates(aoCoordinates);
//
//			return oEcoStressLocation;
//		} catch (Exception oEx) {
//		}
//
//		return null;
//	}

	private static String readFile(String sBucketName, String sFilePath) {
		S3Object oS3Object = m_oConn.getObject(
				new GetObjectRequest(sBucketName, sFilePath)
		);

		S3ObjectInputStream oS3ObjectInputStream = oS3Object.getObjectContent();
		String sXml = s3ObjectInputStreamToString(oS3ObjectInputStream);

		return sXml;
	}

	public static String downloadFile(String sBucketName, String sFilePath, String sDownloadPath) {
		String sFileName = sFilePath.substring(sFilePath.lastIndexOf("/") + 1);
		m_oConn.getObject(
				new GetObjectRequest(sBucketName, sFilePath),
				new File(sDownloadPath + sFileName)
		);

		return sDownloadPath + sFileName;
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
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3BucketUtils.s3ObjectInputStreamToString: error", oEx);
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
			WasdiLog.errorLog("S3BucketUtils.fileToString: error", oEx);
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
		catch (Exception oEx) {
			WasdiLog.errorLog("S3BucketUtils.fileToString: error", oEx);
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
