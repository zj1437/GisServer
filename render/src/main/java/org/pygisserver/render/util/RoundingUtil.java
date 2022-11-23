package org.pygisserver.render.util;

public class RoundingUtil {

	private static double[] SCALE = { 1d, 10d, 100d, 1000d, 10000d, 100000d, 1000000d, 10000000d, 100000000d };

	public static double round(double value, int numDecimals) {
		// Technically this code will handle -numDecimals by rounding digits to the left
		// of the
		// decimal point, but that is
		// probably a use case that is not really needed in practice.

		double scale = (numDecimals < 8) ? SCALE[numDecimals] : Math.pow(10, numDecimals);

		// Prevent us exceeding the maximum precision. We make sure the minimum spacing
		// between
		// values is sufficient
		// for the given scale otherwise just return the value.
		if (Math.ulp(value) * scale > 1d)
			return value;

		return Math.floor(value * scale + 0.5) / scale;
	}
}
