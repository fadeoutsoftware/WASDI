package wasdi.jwasdilib.utils;

import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class StringUtils {

	private final static Logger LOGGER = Logger.getLogger(StringUtils.class);

	private static ObjectMapper s_oMapper = new ObjectMapper();

	private StringUtils() {
		throw new AssertionError("Utility class should not be instantiated.");
	}

	public static double[] convertBoundingBox(String s) {
		LOGGER.info("convertBoundingBox");

		if (s != null && !s.isEmpty()) {
			String[] values = s.split(",");

			if (values.length == 4) {
				Double latN = Double.valueOf(values[0]);
				Double lonW = Double.valueOf(values[1]);
				Double latS = Double.valueOf(values[2]);
				Double lonE = Double.valueOf(values[3]);

				return new double[] { latN, lonW, latS, lonE };
			} else {
				throw new RuntimeException("BBOX Not valid. Please use LATN,LONW,LATS,LONE. BBOX received: " + s);
			}
		}

		return null;
	}

	public static Integer stringToInteger(String s) {
		LOGGER.info("stringToInteger");

		return Integer.valueOf(s);
	}

	public static boolean getResponseBooleanValue(String response)
			throws JsonMappingException, JsonProcessingException {
		LOGGER.info("getResponseBooleanValue");

		Map<String, Object> oJSONMap = s_oMapper.readValue(response, new TypeReference<Map<String, Object>>() {
		});

		if (oJSONMap != null) {
			if (oJSONMap.containsKey("boolValue")) {
				return (boolean) oJSONMap.get("boolValue");
			}
		}

		return false;
	}

}
