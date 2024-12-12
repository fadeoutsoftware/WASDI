package wasdi.shared.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import wasdi.shared.utils.log.WasdiLog;

/**
 * Utility class for JSON related operations.
 * 
 * @author PetruPetrescu
 *
 */
public final class JsonUtils {
	
	public static ObjectMapper s_oMapper = new ObjectMapper();

	private JsonUtils() {
		throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}
	
	public static boolean writeMapAsJsonFile(Map<String, Object> aoJSONMap, String sFileFullPath) throws FileNotFoundException, IOException {

		if (aoJSONMap == null) {
			WasdiLog.errorLog("WasdiFileUtils.writeMapAsJsonFile: aoJSONMap is null");

			return false;
		}

		if (Utils.isNullOrEmpty(sFileFullPath)) {
			WasdiLog.errorLog("WasdiFileUtils.writeMapAsJsonFile: sFileFullPath is null");

			return false;
		}

		String sJson = JsonUtils.stringify(aoJSONMap);

		return WasdiFileUtils.writeFile(sJson, sFileFullPath);
	}

	
	/**
	 * Load the JSON content of a file.
	 * @param sFileFullPath the full path of the file
	 * @return the JSONObject that contains the payload
	 */
	public static JSONObject loadJsonFromFile(String sFileFullPath) {
		Preconditions.checkNotNull(sFileFullPath);

		JSONObject oJson = null;
		try(FileReader oReader = new FileReader(sFileFullPath);){
			
			JSONTokener oTokener = new JSONTokener(oReader);
			oJson = new JSONObject(oTokener);
		} catch (FileNotFoundException oFnf) {
			WasdiLog.errorLog("WasdiFileUtils.loadJsonFromFile: file " + sFileFullPath + " was not found: " + oFnf);
		} catch (Exception oE) {
			WasdiLog.errorLog("WasdiFileUtils.loadJsonFromFile: " + oE);
		}
		return oJson;
	}

	/**
	 * Convert a Map<String, Object> to a string JSON representation
	 * @param aoJSONMap Map to render in Json
	 * @return String with the JSON
	 */
	public static String stringify(Map<String, Object> aoJSONMap) {
		try {
			return s_oMapper.writeValueAsString(aoJSONMap);
		} catch (JsonProcessingException oE) {
			WasdiLog.errorLog("JsonUtils.stringify: could not stringify the object due to " + oE + ".");
		}

		return "";
	}
	
	/**
	 * Obtains the string representation of an object
	 * @param object Object to render as JSON
	 * @return String with the JSON
	 */
	public static String stringify(Object object) {
		try {
			return s_oMapper.writeValueAsString(object);
		} catch (JsonProcessingException oE) {
			WasdiLog.errorLog("JsonUtils.stringify: could not stringify the object due to " + oE + ".");
		}

		return "";
	}
	
	/**
	 * Converts a JSON to a Map<String Object>
	 * @param sJson String with the JSON
	 * @return Corresponding Java Map Object
	 */
	public static Map<String, Object> jsonToMapOfObjects(String sJson) {

		Map<String, Object> aoJSONMap;
		try {
			aoJSONMap = s_oMapper.readValue(sJson, new TypeReference<Map<String, Object>>(){});

			return aoJSONMap;
		} catch (Throwable oE) {
			WasdiLog.debugLog("JsonUtils.jsonToMapOfObjects: could not parse the JSON payload due to " + oE + ".");
		}

		return null;
	}
	
	/**
	 * Converts a JSON in a Map of strings
	 * @param sJson String with the JSON
	 * @return Corresponding Java Map Object
	 */
	public static Map<String, String> jsonToMapOfStrings(String sJson) {

		Map<String, String> aoJSONMap;
		try {
			aoJSONMap = s_oMapper.readValue(sJson, new TypeReference<Map<String, String>>(){});

			return aoJSONMap;
		} catch (JsonProcessingException oE) {
			WasdiLog.errorLog("JsonUtils.jsonToMapOfStrings: could not parse the JSON payload due to " + oE + ".");
		}

		return null;
	}
	
	/**
	 * Converts a JSON to a list of strings 
	 * @param sJson String with the JSON
	 * @return Corresponding Java Map Object
	 */
	public static List<String> jsonToListOfStrings(String sJson) {

		List<String> aoJSONList = new ArrayList<>();
		try {
			aoJSONList = s_oMapper.readValue(sJson, new TypeReference<List<String>>(){});

			return aoJSONList;
		} catch (JsonProcessingException oE) {
			WasdiLog.errorLog("JsonUtils.jsonToListOfStrings: could not parse the JSON payload due to " + oE + ".");
		}

		return aoJSONList;
	}

	/**
	 * Converts a JSON to a list of Maps
	 * @param sJson String with the JSON
	 * @return Corresponding Java Map Object
	 */
	public static List<Map<String, Object>> jsonToListOfMapOfObjects(String sJson) {

		List<Map<String, Object>> aoJSONList;
		try {
			aoJSONList = s_oMapper.readValue(sJson, new TypeReference<List<Map<String, Object>>>(){});

			return aoJSONList;
		} catch (JsonProcessingException oE) {
			WasdiLog.errorLog("JsonUtils.jsonToListOfMapOfObjects: could not parse the JSON payload due to " + oE + ".");
		}

		return null;
	}
	
	/**
	 * Get a property of the map
	 * @param aoJSONMap Java Map
	 * @param sKey Key
	 * @return corresponding object
	 */
	public static String getProperty(Map<String, String> aoJSONMap, String sKey) {
		if (aoJSONMap == null) {
			return null;
		}

		return aoJSONMap.get(sKey);
	}
	
	/**
	 * Get a nested property from a map 
	 * @param aoJSONMap Java Map
	 * @param sPath keys, splitted by "."
	 * @return corresponding object
	 */
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
			oTarget = aoMap.get(aoTokens[i]);
		}

		return oTarget;
	}
	
	/**
	 * Get a nested property from an Object
	 * @param oObject
	 * @param sPath
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Object getProperty(Object oObject, String sPath) {
		if (oObject == null || sPath == null) {
			return oObject;
		}
		
		try {
			String[] aoTokens = sPath.split("\\.");

			if (oObject instanceof Map<?, ?>) {
				Map<String, Object> aoMap = (Map<String, Object>) oObject;

				if (aoTokens.length == 1) {
					return aoMap.get(sPath);
				}

				return getProperty(aoMap.get(aoTokens[0]), sPath.substring(sPath.indexOf(".") + 1));
			} else if (oObject instanceof List<?>) {
				List<?> aoList = (List<?>) oObject;

				int iOrdinal = Integer.parseInt(aoTokens[0]);

				if (aoList.size() <= iOrdinal) {
					return null;
				}

				if (aoTokens.length == 1) {
					return aoList.get(iOrdinal);
				}

				return getProperty(aoList.get(iOrdinal), sPath.substring(sPath.indexOf(".") + 1));
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JsonUtils.getProperty: ",  oEx);
		}

		return null;
	}
	
	/**
	 * Get the Json Schema compatible type description from a Java Object
	 * @param oValue
	 * @return
	 */
	public static String getTypeDescriptionString(Object oValue) {
		try {
			
			if (oValue instanceof String) {
				return "string";
			}
			else if (oValue instanceof Integer) {
				return "number";
			}
			else if (oValue instanceof Float || oValue instanceof Double) {
				return "double";
			}			
			else if (oValue.getClass().isArray()) {
				return "array";
			}
			else if (oValue instanceof Boolean) {
				return "boolean";
			}			
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JsonUtils.getTypeDescriptionString: " + oEx + ".");
		}
		
		return "";
	}

}
