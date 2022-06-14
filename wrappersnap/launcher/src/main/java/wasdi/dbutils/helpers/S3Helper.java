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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;

import wasdi.shared.business.ecostress.EcoStressItem;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ecostress.EcoStressRepository;
import wasdi.shared.utils.TimeEpochUtils;

public final class S3Helper {

	private static final AWSCredentials credentials;

	static {
		credentials = new BasicAWSCredentials(WasdiConfig.Current.s3Bucket.accessKey, WasdiConfig.Current.s3Bucket.secretKey);
	}

	private S3Helper() {
		throw new java.lang.UnsupportedOperationException("This is a helper class and cannot be instantiated");
	}

	public static void parseS3Bucket() {
		AmazonS3 oConn = new AmazonS3Client(credentials);
		oConn.setEndpoint(WasdiConfig.Current.s3Bucket.endpoint);

		String sBucketName = WasdiConfig.Current.s3Bucket.bucketName;
		String sFolders = WasdiConfig.Current.s3Bucket.folders;

		String[] asFolders = sFolders.split(", ");

		for (String sFolder : asFolders) {
			parseFolder(oConn, sBucketName, sFolder);
		}
	}

	public static void parseFolder(AmazonS3 oConn, String sBucketName, String sFolderPath) {
		System.out.println("parseFolder | bucketName: " + sBucketName + " | folderPath: " + sFolderPath);

		EcoStressRepository oEcoStressRepository = new EcoStressRepository();

		Date oStartDate = new Date();

		ObjectListing oObjectListing = oConn.listObjects(sBucketName, sFolderPath);
		int iCounter = -1;

		do {
			for (S3ObjectSummary oS3ObjectSummary : oObjectListing.getObjectSummaries()) {
				EcoStressItem oItem = parseEntry(oConn, sBucketName, oS3ObjectSummary.getKey());
				oEcoStressRepository.insertEcoStressItem(oItem);

				iCounter++;
			}
			oObjectListing = oConn.listNextBatchOfObjects(oObjectListing);
		} while (oObjectListing.getObjectSummaries().size() != 0);

		Date oEndDate = new Date();
		long lMillis = oEndDate.getTime() - oStartDate.getTime();
		System.out.println("seconds: " + (lMillis / 1_000) + " (millis: " + lMillis + ")");

		System.out.println("counter: " + iCounter);
	}

	private static EcoStressItem parseEntry(AmazonS3 oConn, String sBucketName, String sEntryKey) {
		String[] asTokens = sEntryKey.split("/");

		if (asTokens.length < 4) {
			return null;
		}

		String sDirectoryName = asTokens[0] + "/" + asTokens[1];
		String sFileName = asTokens[3].substring(0, asTokens[3].length() - 4);

		String sXmlFilePath = sDirectoryName + "/xml/" + sFileName + ".xml";
		String sH5FilePath = sDirectoryName + "/" + sFileName;

		String sUrl = WasdiConfig.Current.s3Bucket.endpoint + sBucketName + "/"+ sDirectoryName + "/" + sFileName;

		String sXml = readFile(oConn, sBucketName, sXmlFilePath);

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

		Polygon oImageFootPrint = extractFootprint(asProperties);
		oItem.setLocation(oImageFootPrint);

		oItem.setPlatform(asProperties.get("PlatformShortName"));
		oItem.setInstrument(asProperties.get("InstrumentShortName"));
		oItem.setSensor(asProperties.get("SensorShortName"));
		oItem.setParameterName(asProperties.get("ParameterName"));
		
		oItem.setS3Path(sS3Path);
		oItem.setUrl(sUrl);

		return oItem;
	}

	private static Polygon extractFootprint(Map<String, String> asProperties) {
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
	
			Polygon oImageFootPrint = new Polygon(Arrays.asList(new Position(dWest, dNorth),
					new Position(dWest, dSouth),
					new Position(dEast, dSouth),
					new Position(dEast, dNorth),
					new Position(dWest, dNorth)
					));

			return oImageFootPrint;
		} catch (Exception oEx) {
			System.out.println("asProperties: " + asProperties);
			oEx.printStackTrace();
		}

		return null;
	}

	private static String readFile(AmazonS3 oConn, String sBucketName, String sFilePath) {
		S3Object oS3Object = oConn.getObject(
		        new GetObjectRequest(sBucketName, sFilePath)
		);

		S3ObjectInputStream oS3ObjectInputStream = oS3Object.getObjectContent();
		String sXml = s3ObjectInputStreamToString(oS3ObjectInputStream);

		return sXml;
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

}
