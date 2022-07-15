package wasdi.shared.utils.gis;

import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;

import wasdi.shared.utils.Utils;

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
				Utils.log("ERROR", "BoundingBoxUtils.parseBoundingBox: issue with the bounding box: " + sBoundingBox + ": " + oE);
			}
		}

		return null;
	}

	public static List<Integer> expandBoundingBoxUpToADegree(List<Double> boundingBox) {
		if (boundingBox == null || boundingBox.size() != 4) {
			return null;
		}

		Double north = boundingBox.get(0);
		Double west = boundingBox.get(1);
		Double south = boundingBox.get(2);
		Double east = boundingBox.get(3);

		if (north == null || west == null || south == null || east == null) {
			return null;
		}

		Integer expandedNorth;
		Integer expandedWest;
		Integer expandedSouth;
		Integer expandedEast;

		if (north == 0) {
			expandedNorth = 0;
		} else {
			expandedNorth = roundUpAsInt.apply(north);
		}

		if (west == 0) {
			expandedWest = 0;
		} else {
			expandedWest = roundDownAsInt.apply(west);
		}

		if (south == 0) {
			expandedSouth = 0;
		} else {
			expandedSouth = roundDownAsInt.apply(south);
		}

		if (east == 0) {
			expandedEast = 0;
		} else {
			expandedEast = roundUpAsInt.apply(east);
		}

		List<Integer> expandedBoundingBox = new ArrayList<>();
		expandedBoundingBox.add(expandedNorth);
		expandedBoundingBox.add(expandedWest);
		expandedBoundingBox.add(expandedSouth);
		expandedBoundingBox.add(expandedEast);

		return expandedBoundingBox;
	}

	public static List<Double> expandBoundingBoxUpToAQuarterDegree(List<Double> boundingBox) {
		if (boundingBox == null || boundingBox.size() != 4) {
			return null;
		}

		Double north = boundingBox.get(0);
		Double west = boundingBox.get(1);
		Double south = boundingBox.get(2);
		Double east = boundingBox.get(3);

		if (north == null || west == null || south == null || east == null) {
			return boundingBox;
		}

		Double expandedNorth;
		Double expandedWest;
		Double expandedSouth;
		Double expandedEast;

		if (north == 0) {
			expandedNorth = north;
		} else {
			expandedNorth = roundToQuarterUp.apply(north);
		}

		if (west == 0) {
			expandedWest = west;
		} else {
			expandedWest = roundToQuarterDown.apply(west);
		}

		if (south == 0) {
			expandedSouth = south;
		} else {
			expandedSouth = roundToQuarterDown.apply(south);
		}

		if (east == 0) {
			expandedEast = east;
		} else {
			expandedEast = roundToQuarterUp.apply(east);
		}

		List<Double> expandedBoundingBox = new ArrayList<>();
		expandedBoundingBox.add(expandedNorth);
		expandedBoundingBox.add(expandedWest);
		expandedBoundingBox.add(expandedSouth);
		expandedBoundingBox.add(expandedEast);

		return expandedBoundingBox;
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
