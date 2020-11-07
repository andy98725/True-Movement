package removeThese;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public class Intersection {
	/**
	 * Epsilon for floating point computations
	 */
	private static final double EPSILON = 1e-6;

	/**
	 * Computes the intersection of the given lines.
	 * 
	 * @param line0            The first line
	 * @param line1            The second line
	 * @param relativeLocation Optional location that stores the relative location
	 *                         of the intersection point on the given line segments
	 * @param absoluteLocation Optional location that stores the absolute location
	 *                         of the intersection point
	 * @return Whether the lines intersect
	 */
	public static boolean intersectLineLine(Line2D line0, Line2D line1, Point2D relativeLocation,
			Point2D absoluteLocation) {
		return intersectLineLine(line0.getX1(), line0.getY1(), line0.getX2(), line0.getY2(), line1.getX1(),
				line1.getY1(), line1.getX2(), line1.getY2(), relativeLocation, absoluteLocation);
	}

	/**
	 * Computes the intersection of the specified lines.
	 * 
	 * Ported from http://www.geometrictools.com/LibMathematics/Intersection/
	 * Wm5IntrSegment2Segment2.cpp
	 * 
	 * @param s0x0             x-coordinate of point 0 of line segment 0
	 * @param s0y0             y-coordinate of point 0 of line segment 0
	 * @param s0x1             x-coordinate of point 1 of line segment 0
	 * @param s0y1             y-coordinate of point 1 of line segment 0
	 * @param s1x0             x-coordinate of point 0 of line segment 1
	 * @param s1y0             y-coordinate of point 0 of line segment 1
	 * @param s1x1             x-coordinate of point 1 of line segment 1
	 * @param s1y1             y-coordinate of point 1 of line segment 1
	 * @param relativeLocation Optional location that stores the relative location
	 *                         of the intersection point on the given line segments
	 * @param absoluteLocation Optional location that stores the absolute location
	 *                         of the intersection point
	 * @return Whether the lines intersect
	 */
	public static boolean intersectLineLine(double s0x0, double s0y0, double s0x1, double s0y1, double s1x0,
			double s1y0, double s1x1, double s1y1, Point2D relativeLocation, Point2D absoluteLocation) {
		double dx0 = s0x1 - s0x0;
		double dy0 = s0y1 - s0y0;
		double dx1 = s1x1 - s1x0;
		double dy1 = s1y1 - s1y0;

		double invLen0 = 1.0 / Math.sqrt(dx0 * dx0 + dy0 * dy0);
		double invLen1 = 1.0 / Math.sqrt(dx1 * dx1 + dy1 * dy1);

		double dir0x = dx0 * invLen0;
		double dir0y = dy0 * invLen0;
		double dir1x = dx1 * invLen1;
		double dir1y = dy1 * invLen1;

		double c0x = s0x0 + dx0 * 0.5;
		double c0y = s0y0 + dy0 * 0.5;
		double c1x = s1x0 + dx1 * 0.5;
		double c1y = s1y0 + dy1 * 0.5;

		double cdx = c1x - c0x;
		double cdy = c1y - c0y;

		double dot = dotPerp(dir0x, dir0y, dir1x, dir1y);
		if (Math.abs(dot) > EPSILON) {
			if (relativeLocation != null || absoluteLocation != null) {
				double dot0 = dotPerp(cdx, cdy, dir0x, dir0y);
				double dot1 = dotPerp(cdx, cdy, dir1x, dir1y);
				double invDot = 1.0 / dot;
				double s0 = dot1 * invDot;
				double s1 = dot0 * invDot;
				if (relativeLocation != null) {
					double n0 = (s0 * invLen0) + 0.5;
					double n1 = (s1 * invLen1) + 0.5;
					relativeLocation.setLocation(n0, n1);
				}
				if (absoluteLocation != null) {
					double x = c0x + s0 * dir0x;
					double y = c0y + s0 * dir0y;
					absoluteLocation.setLocation(x, y);
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns the perpendicular dot product, i.e. the length of the vector
	 * (x0,y0,0)x(x1,y1,0).
	 * 
	 * @param x0 Coordinate x0
	 * @param y0 Coordinate y0
	 * @param x1 Coordinate x1
	 * @param y1 Coordinate y1
	 * @return The length of the cross product vector
	 */
	private static double dotPerp(double x0, double y0, double x1, double y1) {
		return x0 * y1 - y0 * x1;
	}

	public static Point2D getIntersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4,
			double y4) {

		final double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3) * (x1 * y2 - x2 * y1))
				/ ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
		final double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2) * (x3 * y4 - x4 * y3))
				/ ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));

		return new Point2D.Double(x, y);

	}

	// Check if line segments intersect
//	public static boolean intersectSegSeg(double x0, double y0, double x1, double y1, double x2, double y2, double x3,
//			double y3) {
//		// Check equal points
//		if (Math.abs(x0 - x1) < 1 && Math.abs(y0 - y1) < 1) {
//			return false;
//		}
//		if (Math.abs(x2 - x3) < 1 && Math.abs(y2 - y3) < 1) {
//			return false;
//		}
//		// Get 4 orientations
//		int o1 = getOrientation(x0, y0, x1, y1, x2, y2);
//		int o2 = getOrientation(x0, y0, x1, y1, x3, y3);
//		int o3 = getOrientation(x2, y2, x3, y3, x1, y1);
//		int o4 = getOrientation(x2, y2, x3, y3, x2, y2);
//		// Return if winding rules differ
//		return (o1 != o2 && o3 != o4);
//	}
//
//	// Orientations
//	private static final int O_COL = 0, O_CW = 1, O_CCW = 2;
//
//	// Calculate triangle orientation
//	private static int getOrientation(double x0, double y0, double x1, double y1, double x2, double y2) {
//		// Calculate orientation value
//		double val = (y1 - y0) * (x2 - x1) - (x1 - x0) * (y2 - y1);
//		// If 0, return colinear
//		if (val == 0) {
//			return O_COL;
//		}
//		// If positive, clockwise
//		return val > 0 ? O_CW : O_CCW;
//	}

}