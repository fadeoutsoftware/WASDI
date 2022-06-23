package wasdi.shared.utils;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for JSON related operations.
 * 
 * @author PetruPetrescu
 *
 */
public final class JsonUtils {
	
	private static ObjectMapper s_oMapper = new ObjectMapper();

	private JsonUtils() {
		throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	public static String stringify(Map<String, Object> aoJSONMap) {
		try {
			return s_oMapper.writeValueAsString(aoJSONMap);
		} catch (JsonProcessingException oE) {
			Utils.debugLog("JsonUtils.stringify: could not stringify the object due to " + oE + ".");
		}

		return null;
	}

	public static String stringify(Object object) {
		try {
			return s_oMapper.writeValueAsString(object);
		} catch (JsonProcessingException oE) {
			Utils.debugLog("JsonUtils.stringify: could not stringify the object due to " + oE + ".");
		}

		return null;
	}

	public static Map<String, Object> jsonToMapOfObjects(String sJson) {

		Map<String, Object> aoJSONMap;
		try {
			aoJSONMap = s_oMapper.readValue(sJson, new TypeReference<Map<String, Object>>(){});

			return aoJSONMap;
		} catch (JsonProcessingException oE) {
			Utils.debugLog("JsonUtils.jsonToMapOfObjects: could not parse the JSON payload due to " + oE + ".");
		}

		return null;
	}

	public static Map<String, String> jsonToMapOfStrings(String sJson) {

		Map<String, String> aoJSONMap;
		try {
			aoJSONMap = s_oMapper.readValue(sJson, new TypeReference<Map<String, String>>(){});

			return aoJSONMap;
		} catch (JsonProcessingException oE) {
			Utils.debugLog("JsonUtils.jsonToMapOfStrings: could not parse the JSON payload due to " + oE + ".");
		}

		return null;
	}

	public static List<String> jsonToListOfStrings(String sJson) {

		List<String> aoJSONList;
		try {
			aoJSONList = s_oMapper.readValue(sJson, new TypeReference<List<String>>(){});

			return aoJSONList;
		} catch (JsonProcessingException oE) {
			Utils.debugLog("JsonUtils.jsonToListOfStrings: could not parse the JSON payload due to " + oE + ".");
		}

		return null;
	}

	public static List<Map<String, Object>> jsonToListOfMapOfObjects(String sJson) {

		List<Map<String, Object>> aoJSONList;
		try {
			aoJSONList = s_oMapper.readValue(sJson, new TypeReference<List<Map<String, Object>>>(){});

			return aoJSONList;
		} catch (JsonProcessingException oE) {
			Utils.debugLog("JsonUtils.jsonToListOfMapOfObjects: could not parse the JSON payload due to " + oE + ".");
		}

		return null;
	}

	public static String getProperty(Map<String, String> aoJSONMap, String sKey) {
		if (aoJSONMap == null) {
			return null;
		}

		return aoJSONMap.get(sKey);
	}

	public static Object getPropertyByPath(Map<String, Object> aoJSONMap, String sPath) {
		if (aoJSONMap == null) {
			return null;
		}

		if (sPath == null) {
			return aoJSONMap.get(sPath);
		}

		String[] aoTokens = sPath.split(".");

		Map<String, Object> aoMap = aoJSONMap;
		Object oTarget = null;
		for (int i = 0; i < aoTokens.length; i++) {
			aoMap.get(aoTokens[i]);
		}

		return oTarget;
	}

	@SuppressWarnings("unchecked")
	public static Object getProperty(Object oObject, String sPath) {
		if (oObject == null || sPath == null) {
			return oObject;
		}

		String[] aoTokens = sPath.split("\\.");

		if (oObject instanceof Map<?, ?>) {
			Map<String, Object> map = (Map<String, Object>) oObject;

			if (aoTokens.length == 1) {
				return map.get(sPath);
			}

			return getProperty(map.get(aoTokens[0]), sPath.substring(sPath.indexOf(".") + 1));
		} else if (oObject instanceof List<?>) {
			List<?> list = (List<?>) oObject;

			int ordinal = Integer.parseInt(aoTokens[0]);

			if (list.size() <= ordinal) {
				return null;
			}

			if (aoTokens.length == 1) {
				return list.get(ordinal);
			}

			return getProperty(list.get(ordinal), sPath.substring(sPath.indexOf(".") + 1));
		}

		return null;
	}

}
