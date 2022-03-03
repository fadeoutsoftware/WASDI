package wasdi.shared.utils.gis;

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

	public static Function<Double, Double> roundToQuarterUp = (a) -> multiplyBy4.andThen(roundUp).andThen(divideBy4).apply(a);
	public static Function<Double, Double> roundToQuarterDown = (a) -> multiplyBy4.andThen(roundDown).andThen(divideBy4).apply(a);

}
