package removeThese;

import java.awt.Shape;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class Shapes {
	/**
	 * Create a list containing line segments that approximate the given shape.
	 * 
	 * @param shape    The shape
	 * @param flatness The allowed flatness
	 * @return The list of line segments
	 */
	public static ArrayList<Line2D> computeLineSegments(Shape shape, double flatness) {
		ArrayList<Line2D> result = new ArrayList<Line2D>();
		PathIterator pi = new FlatteningPathIterator(shape.getPathIterator(null), flatness);
		double coords[] = new double[2];
		double previous[] = new double[2];
		double first[] = new double[2];
		while (!pi.isDone()) {
			int segment = pi.currentSegment(coords);
			switch (segment) {
			case PathIterator.SEG_MOVETO:
				previous[0] = coords[0];
				previous[1] = coords[1];
				first[0] = coords[0];
				first[1] = coords[1];
				break;

			case PathIterator.SEG_CLOSE:
				result.add(new Line2D.Double(previous[0], previous[1], first[0], first[1]));
				previous[0] = first[0];
				previous[1] = first[1];
				break;

			case PathIterator.SEG_LINETO:
				result.add(new Line2D.Double(previous[0], previous[1], coords[0], coords[1]));
				previous[0] = coords[0];
				previous[1] = coords[1];
				break;

			case PathIterator.SEG_QUADTO:
				// Should never occur
				throw new AssertionError("SEG_QUADTO in flattened path!");

			case PathIterator.SEG_CUBICTO:
				// Should never occur
				throw new AssertionError("SEG_CUBICTO in flattened path!");
			}
			pi.next();
		}
		return result;
	}

	static List<Line2D> computeRelevantLineSegments(Area shape, double flatness, Point2D origin, double radius) {
		double sqDist = radius * radius;

		// First, flatten area
		List<Line2D> result = new ArrayList<Line2D>();
		PathIterator pi = new FlatteningPathIterator(shape.getPathIterator(null), flatness);
		double coords[] = new double[2];
		double previous[] = new double[2];
		double first[] = new double[2];
		while (!pi.isDone()) {
			int segment = pi.currentSegment(coords);
			switch (segment) {
			case PathIterator.SEG_MOVETO:
				previous[0] = coords[0];
				previous[1] = coords[1];
				first[0] = coords[0];
				first[1] = coords[1];
				break;

			case PathIterator.SEG_CLOSE:
				// Compare to closest point
				Point2D closest = Points.getClosestPointOnSegment(previous[0], previous[1], first[0], first[1],
						origin.getX(), origin.getY());
				if (closest.distanceSq(origin) < sqDist) {
					result.add(new Line2D.Double(previous[0], previous[1], first[0], first[1]));
				}
				previous[0] = first[0];
				previous[1] = first[1];
				break;

			case PathIterator.SEG_LINETO:
				// Compare to closest point
				closest = Points.getClosestPointOnSegment(previous[0], previous[1], coords[0], coords[1], origin.getX(),
						origin.getY());
				if (closest.distanceSq(origin) < sqDist) {
					result.add(new Line2D.Double(previous[0], previous[1], coords[0], coords[1]));
				}
				previous[0] = coords[0];
				previous[1] = coords[1];
				break;

			case PathIterator.SEG_QUADTO:
				// Should never occur
				throw new AssertionError("SEG_QUADTO in flattened path!");

			case PathIterator.SEG_CUBICTO:
				// Should never occur
				throw new AssertionError("SEG_CUBICTO in flattened path!");
			}
			pi.next();
		}
		return result;
	}

	/**
	 * Create a list containing line segments that approximate the given shape, with
	 * limiting segments as well.
	 * 
	 * @param shape    The shape
	 * @param flatness The allowed flatness
	 * @param dist     The max distance to draw a line between
	 * @param rad      The radius of vision
	 * @return The list of line segments
	 */
	static List<Line2D> computeLineSegments(Point2D origin, Shape shape, double flatness, double dist, double rad,
			int wid, int hei) {
		List<Line2D> result = computeLineSegments(shape, flatness);
		List<Line2D> baseArr = new ArrayList<Line2D>();
		for (Line2D line : result) {
			baseArr.add(line);
		}
		// Loop through and add connections
		for (Line2D line : baseArr) {
			// Check if line has no length
			if (line.getP1().equals(line.getP2())) {
				result.remove(line);
				continue;
			}
			// Check if line is border line
			if (origin.distance(line.getP1()) > rad - dist && origin.distance(line.getP2()) > rad - dist) {
				continue;
			}
			// Only use point 1
			Point2D base = line.getP1();
			// Loop through lines and get distance
			for (Line2D lineCompare : baseArr) {
				// Check if geometry line
				if (line.intersectsLine(lineCompare)) {
					continue;
				}
				// Check if line is border line
				if (origin.distance(lineCompare.getP1()) > rad - dist
						&& origin.distance(lineCompare.getP2()) > rad - dist) {
					continue;
				}
				Point2D closestOnLine = Points.getClosestPointOnSegment(lineCompare, base);
				double pointDistance = base.distance(closestOnLine);
				if (pointDistance > 0 && pointDistance < dist) {
					// We have a connection!
					result.add(new Line2D.Double(base, Points.getClosestPointOnSegment(lineCompare, base)));
				}
			}
		}
		// Now add borders of map too
		result.add(new Line2D.Double(0, 0, 0, hei));
		result.add(new Line2D.Double(wid, 0, wid, hei));
		result.add(new Line2D.Double(0, 0, wid, 0));
		result.add(new Line2D.Double(0, hei, wid, hei));
		return result;
	}

	static List<Point2D> computeCornerPoints(Shape shape, double flatness) {
		List<Point2D> result = new ArrayList<Point2D>();
		PathIterator pi = new FlatteningPathIterator(shape.getPathIterator(null), flatness);
		double coords[] = new double[2];
		while (!pi.isDone()) {
			pi.currentSegment(coords);
			result.add(new Point2D.Double(coords[0], coords[1]));
			pi.next();
		}
		return result;
	}

	static List<Point2D> computeCornerPoints(List<Line2D> lines) {
		List<Point2D> result = new ArrayList<Point2D>();
		for (Line2D line : lines) {
			result.add(line.getP1());
		}
		return result;
	}

	static List<Point2D> computeIntersectingPoints(Area sight, List<Point2D> points, double leeway) {
		List<Point2D> result = new ArrayList<Point2D>();
		for (Point2D p : points) {
			if (distance(p.getX(), p.getY(), sight) <= leeway)
				result.add(p);

		}
		return result;
	}

	public static double distance(double x, double y, Area a) {
		// Quick check
		if (a.contains(x, y)) {
			return 0;
		}
		// Distance to store
		double sqDist = Double.MAX_VALUE;
		// Points stored here
		double[] coords = new double[6];
		// Iterate through a1
		for (PathIterator iter = a.getPathIterator(null, 1.0f); !iter.isDone(); iter.next()) {
			// Get next segment
			final int oper = iter.currentSegment(coords);
			switch (oper) {
			case PathIterator.SEG_MOVETO:
				// Store previous
				coords[2] = coords[0];
				coords[3] = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				break;
			case PathIterator.SEG_LINETO:
				// Line, calculate distance to line
				double sum = Line2D.ptLineDistSq(coords[0], coords[1], coords[2], coords[3], x, y);
				// Easy return
				if (sum == 0) {
					return sum;
				}
				// Set
				if (sum < sqDist) {
					sqDist = sum;
				}
				// Store previous
				coords[2] = coords[0];
				coords[3] = coords[1];
				break;
			}
		}

		// Return square root value
		return Math.sqrt(sqDist);
	}
}