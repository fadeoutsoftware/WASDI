package wasdi.shared.utils.gis;

import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;

import wasdi.shared.utils.log.WasdiLog;

/**
 * Utility class for BoundingBox related operations.
 * 
 * @author PetruPetrescu
 *
 */
public class BoundingBoxUtils {

	private BoundingBoxUtils() {
		throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}
	
	
	/**
	 * Get a WKT Polygon Representation of a lat lon rectangle
	 * @param dSouth South coordinate
	 * @param dWest West  coordinate
	 * @param dNorth North  coordinate
	 * @param dEast East  coordinate
	 * @return WKT representation 
	 */
	public static String getWKTPolygon(double dSouth, double dWest, double dNorth, double dEast ) {
		String sFootPrint = "POLYGON(( " + dWest + " " +dSouth + "," + dWest + " " + dNorth + "," + dEast + " " + dNorth + "," + dEast + " " + dSouth + "," + dWest + " " +dSouth + "))";
		return sFootPrint;
	}
	
	/**
	 * Check interesection between two rectangle bboxes
	 * @param dSouth1 Rect1 south coordinate
	 * @param dWest1 Rect1 west coordinate
	 * @param dNorth1 Rect1 north coordinate
	 * @param dEast1 Rect1 east coordinate
	 * @param dSouth2 Rect2 south coordinate
	 * @param dWest2 Rect2 west coordinate
	 * @param dNorth2 Rect2 north coordinate
	 * @param dEast2 Rect2 east coordinate
	 * @return
	 */
	public static boolean bboxIntersects(double dSouth1, double dWest1, double dNorth1, double dEast1, double dSouth2, double dWest2, double dNorth2, double dEast2) {
		
		Rectangle2D oBbox1 = new Rectangle2D.Double(dWest1, dSouth1, dEast1-dWest1, dNorth1-dSouth1);
		Rectangle2D oBbox2 = new Rectangle2D.Double(dWest2, dSouth2, dEast2-dWest2, dNorth2-dSouth2);
		
		return oBbox1.intersects(oBbox2);
	}
	
	/**
	 * Parse a bbox string in the format north, west, south, east in a list of doubles
	 * @param sBoundingBox
	 * @return
	 */
	public static List<Double> parseBoundingBox(String sBoundingBox) {
		if (sBoundingBox != null && !sBoundingBox.contains("null")) {
			try {
				String[] asBoundingBox = sBoundingBox.split(", ");

				if (asBoundingBox.length == 4) {
					double dNorth = Double.parseDouble(asBoundingBox[0]);
					double dWest = Double.parseDouble(asBoundingBox[1]);
					double dSouth = Double.parseDouble(asBoundingBox[2]);
					double dEast = Double.parseDouble(asBoundingBox[3]);

					return Arrays.asList(dNorth, dWest, dSouth, dEast);
				}
			} catch(PatternSyntaxException | NullPointerException | NumberFormatException oE) {
				WasdiLog.errorLog("BoundingBoxUtils.parseBoundingBox: issue with the bounding box: " + sBoundingBox + ": ", oE);
			}
		}

		return null;
	}

	public static List<Integer> expandBoundingBoxUpToADegree(List<Double> adBoundingBox) {
		if (adBoundingBox == null || adBoundingBox.size() != 4) {
			return null;
		}

		Double dNorth = adBoundingBox.get(0);
		Double dWest = adBoundingBox.get(1);
		Double dSouth = adBoundingBox.get(2);
		Double dEeast = adBoundingBox.get(3);

		if (dNorth == null || dWest == null || dSouth == null || dEeast == null) {
			return null;
		}

		Integer iExpandedNorth;
		Integer iExpandedWest;
		Integer iExpandedSouth;
		Integer iExpandedEast;

		if (dNorth == 0) {
			iExpandedNorth = 0;
		} else {
			iExpandedNorth = roundUpAsInt.apply(dNorth);
		}

		if (dWest == 0) {
			iExpandedWest = 0;
		} else {
			iExpandedWest = roundDownAsInt.apply(dWest);
		}

		if (dSouth == 0) {
			iExpandedSouth = 0;
		} else {
			iExpandedSouth = roundDownAsInt.apply(dSouth);
		}

		if (dEeast == 0) {
			iExpandedEast = 0;
		} else {
			iExpandedEast = roundUpAsInt.apply(dEeast);
		}

		List<Integer> aiExpandedBoundingBox = new ArrayList<>();
		aiExpandedBoundingBox.add(iExpandedNorth);
		aiExpandedBoundingBox.add(iExpandedWest);
		aiExpandedBoundingBox.add(iExpandedSouth);
		aiExpandedBoundingBox.add(iExpandedEast);

		return aiExpandedBoundingBox;
	}

	public static List<Double> expandBoundingBoxUpToAQuarterDegree(List<Double> adBoundingBox) {
		if (adBoundingBox == null || adBoundingBox.size() != 4) {
			return null;
		}

		Double dNorth = adBoundingBox.get(0);
		Double dWest = adBoundingBox.get(1);
		Double dSouth = adBoundingBox.get(2);
		Double dEast = adBoundingBox.get(3);

		if (dNorth == null || dWest == null || dSouth == null || dEast == null) {
			return adBoundingBox;
		}

		Double dExpandedNorth;
		Double dExpandedWest;
		Double dExpandedSouth;
		Double dExpandedEast;

		if (dNorth == 0) {
			dExpandedNorth = dNorth;
		} else {
			dExpandedNorth = roundToQuarterUp.apply(dNorth);
		}

		if (dWest == 0) {
			dExpandedWest = dWest;
		} else {
			dExpandedWest = roundToQuarterDown.apply(dWest);
		}

		if (dSouth == 0) {
			dExpandedSouth = dSouth;
		} else {
			dExpandedSouth = roundToQuarterDown.apply(dSouth);
		}

		if (dEast == 0) {
			dExpandedEast = dEast;
		} else {
			dExpandedEast = roundToQuarterUp.apply(dEast);
		}

		List<Double> adExpandedBoundingBox = new ArrayList<>();
		adExpandedBoundingBox.add(dExpandedNorth);
		adExpandedBoundingBox.add(dExpandedWest);
		adExpandedBoundingBox.add(dExpandedSouth);
		adExpandedBoundingBox.add(dExpandedEast);

		return adExpandedBoundingBox;
	}
	
	/**
	 * Confert a Polygon WKT String in a set of Lat Lon Points comma separated
	 * 
	 * @param sContent
	 * @return
	 */
	public static String polygonToBounds(String sContent) {
		sContent = sContent.replace("MULTIPOLYGON ", "");
		sContent = sContent.replace("MULTIPOLYGON", "");
		sContent = sContent.replace("POLYGON ", "");
		sContent = sContent.replace("POLYGON", "");
		sContent = sContent.replace("(((", "");
		sContent = sContent.replace(")))", "");
		sContent = sContent.replace("((", "");
		sContent = sContent.replace("))", "");

		String[] asContent = sContent.split(",");

		String sOutput = "";

		for (int iIndexBounds = 0; iIndexBounds < asContent.length; iIndexBounds++) {
			String sBounds = asContent[iIndexBounds];
			sBounds = sBounds.trim();
			String[] asNewBounds = sBounds.split(" ");

			if (iIndexBounds > 0)
				sOutput += ", ";

			try {
				sOutput += asNewBounds[1] + "," + asNewBounds[0];
			} catch (Exception oEx) {
				WasdiLog.errorLog("Utils.polygonToBounds: error", oEx);
			}

		}
		return sOutput;

	}	

	private static BiFunction<Double, Double, Double> multiply = (a, b) -> BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b)).doubleValue();
	private static Function<Double, Double> multiplyBy4 = (a) -> multiply.apply(a, 4.0);

	private static BiFunction<Double, Double, Double> divide = (a, b) -> BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b)).doubleValue();
	private static Function<Double, Double> divideBy4 = (a) -> divide.apply(a, 4.0);

	private static Function<Double, Double> roundUp = (a) -> BigDecimal.valueOf(a).setScale(0, BigDecimal.ROUND_CEILING).doubleValue();
	private static Function<Double, Double> roundDown = (a) -> BigDecimal.valueOf(a).setScale(0, BigDecimal.ROUND_FLOOR).doubleValue();

	private static Function<Double, Integer> roundUpAsInt = (a) -> BigDecimal.valueOf(a).setScale(0, BigDecimal.ROUND_CEILING).intValue();
	private static Function<Double, Integer> roundDownAsInt = (a) -> BigDecimal.valueOf(a).setScale(0, BigDecimal.ROUND_FLOOR).intValue();

	public static Function<Double, Double> roundToQuarterUp = (a) -> multiplyBy4.andThen(roundUp).andThen(divideBy4).apply(a);
	public static Function<Double, Double> roundToQuarterDown = (a) -> multiplyBy4.andThen(roundDown).andThen(divideBy4).apply(a);

}
