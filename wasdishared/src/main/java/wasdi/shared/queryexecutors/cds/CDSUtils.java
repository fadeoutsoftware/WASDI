package wasdi.shared.queryexecutors.cds;

import java.util.Arrays;
import java.util.List;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public final class CDSUtils {
	
	// ERA5 datasets' names
	public static final String s_sPRESSURE_LEVELS_DATASET = "reanalysis-era5-pressure-levels";
	public static final String s_sSEA_TEMPERATURE_DATASET = "satellite-sea-surface-temperature";
	
	
	// filters values from the client
	public static final String s_sALL_PRESSURE_LEVELS = "all_available_pressure_levels";
	
	
	// hard-coded values
	public static final List<String> s_asTIME_HOURS = Arrays.asList("00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00", "08:00", 
			"09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00");
	
	public static final List<String> s_asPRESSURE_LEVELS = Arrays.asList(
			"1", "2", "3", "5", "7", "10", 
			"20", "30", "50", "70", "100", 
			"125", "150", "175", "200", 
			"225", "250", "300", "350", 
			"400", "450", "500", "550", 
			"600", "650", "700", "750", 
			"775", "800", "825", "850", 
			"875", "900", "925", "950", 
			"975", "1000"); 
	
	
	// private constructor to prevent initialisation of the class
	private CDSUtils() {
		throw new java.lang.UnsupportedOperationException("CDSUtils.java is an helper class and it can not be instantiated");
	}
	
	
	/**
	 * 
	 * @param sVariable
	 * @return
	 */
	public static String inflateVariable(String sVariable) {
		switch (sVariable) {
		case "OMMR":
			return "ozone_mass_mixing_ratio";
		case "RH":
			return "relative_humidity";
		case "SH":
			return "specific_humidity";
		case "SP":
			return "surface_pressure";
		case "SSRD":
			return "surface_solar_radiation_downwards";
		case "SST":
			return "sea_surface_temperature";
		case "ST":
			return "skin_temperature";
		case "T":
			return "temperature";
		case "TCWV":
			return "total_column_water_vapour";
		case "TISR":
			return "toa_incident_solar_radiation";
		case "TP":
			return "total_precipitation";
		case "TSDSRAS":
			return "total_sky_direct_solar_radiation_at_surface";
		case "U":
			return "u_component_of_wind";
		case "V":
			return "v_component_of_wind";
		case "2DT":
			return "2m_dewpoint_temperature";
		case "2T":
			return "2m_temperature";
		case "10U":
			return "10m_u_component_of_wind";
		case "10V":
			return "10m_v_component_of_wind";
		default:
			WasdiLog.debugLog("CDSUtils.inflateVariable: variable does not need to be inflated: " + sVariable + ".");
			return sVariable;
		}
	}
	
	/**
	 * Create the string representing the footprint of the data, to be included in the ERA5 file name
	 * @param oW: west
	 * @param oE: east
	 * @param oN: north
	 * @param oS: south
	 * @param sDatasetName: name of the dataset 
	 * @return the representation of the footprint, in a standard format
	 */
	public static String getFootprintForFileName(Double oN, Double oW, Double oS, Double oE, String sDatasetName) {
		String sFootprint = "";
		if ((oW == null && oE == null && oS == null && oN == null) || sDatasetName.equalsIgnoreCase(s_sSEA_TEMPERATURE_DATASET)) {
			oW = -180d;
			oN = 90d;
			oE = 180d;
			oS = -90d;
		} 
		
		String sW = getStringRepresentationOfPoint(oW, "W");
		String sE = getStringRepresentationOfPoint(oE, "E");
		String sS = getStringRepresentationOfPoint(oS, "S");
		String sN = getStringRepresentationOfPoint(oN, "N");
		sFootprint = sN + sW + sS + sE;
		
		return sFootprint;
	}
	
	/**
	 * Format a single point of the bounding box according to the following pattern:
	 * - "n" or "p" to represent a negative or a positive value
	 * - the value of the point. The integer part of the value always contains two digits (for north or south points) or three digits (for west and east points).
	 * The decimal part of the value always contain three digits and it is contiguous to the integer part (i.e. no special characters are used to separate the integer and the decimal part)
	 * - a letter representing the cardinal point (can be: W, E, N, S)
	 * @param oValue the  value representing the position of the point
	 * @param sCoordinate a letter representing the cardinal point (W, E, N, S)
	 * @return a string following one of the following patterns: [n|p]\d\d\d.\d\d\d[W|E] for east and west cardinal points, [n|p]\d\d.\d\d\d[S|W] for north and south cardinal points
	 */
	public static String getStringRepresentationOfPoint(Double oValue, String sCoordinate) {
		
		String sFormattedCoordinate = "";
		
		if (oValue != null && !oValue.isNaN()) {
		
			// 1 - prefix to determine if the number is positive or negative
			sFormattedCoordinate += oValue < 0.0 ? "n" : "p";
			
			try {
			
				// 2 - put in a standard format the integer and decimal part of the coordinate 
				
				// "%.3" is used to keep three decimal values (if there are not decimal values, then it adds zeros)
				String[] asValues = String.format("%.3f", oValue).split("\\.");	
				
				if (asValues.length == 2) { 
					String sIntPart = asValues[0].replace("-", "");
					
					// pad the representation of the number with zeros if necessary
					if (sCoordinate.equalsIgnoreCase("N") || sCoordinate.equalsIgnoreCase("S"))
						sIntPart = String.format("%02d", Integer.parseInt(sIntPart));
					else
						sIntPart = String.format("%03d", Integer.parseInt(sIntPart));
					
					sFormattedCoordinate += sIntPart + asValues[1]; 
							
					// 3 - add the reference to the cardinal point
					sFormattedCoordinate += sCoordinate;
				}
				else {
					WasdiLog.debugLog("CDSUtils.getStringRepresentationOfPoint. Cannot find integer and decimal part in the number: " + oValue);
					sFormattedCoordinate = "null";
				}
			} catch (Exception oE) {
				WasdiLog.debugLog("CDSUtils.getStringRepresentationOfPoint.Exception while formatting the value " + oValue);
				sFormattedCoordinate = "null";
			}
		} else {
			// this is a fallback that should never happen
			WasdiLog.debugLog("CDSUtils.getStringRepresentationOfPoint. The coordinate poit is null or not a number " + oValue);
			sFormattedCoordinate = "null";
		}
		
		
		return sFormattedCoordinate;
	}
	
	
	public static String getFileName(String sDataset, String sVariables, String sDailyDate, String sStartDate, String sEndDate, String sExtension, String sFootprint) {

		return Utils.isNullOrEmpty(sStartDate) || Utils.isNullOrEmpty(sEndDate) 
				? String.join("_", Platforms.ERA5, sDataset, sVariables, sDailyDate, sFootprint).replaceAll("[\\W]", "_") + sExtension
				: String.join("_", Platforms.ERA5, sDataset, sVariables, sStartDate, sEndDate, sFootprint).replaceAll("[\\W]", "_") + sExtension;
	}
}