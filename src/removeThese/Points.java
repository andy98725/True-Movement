package removeThese;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

public class Points {
	static final double TAU = 2 * Math.PI;

	/**
	 * Creates a comparator that compares points by the angle of the line between
	 * the point and the given center
	 * 
	 * @param center The center
	 * @return The comparator
	 */
	public static Comparator<Point2D> byAngleComparator(final Point2D center) {
		return new Comparator<Point2D>() {
			@Override
			public int compare(Point2D p0, Point2D p1) {
				double dx0 = p0.getX() - center.getX();
				double dy0 = p0.getY() - center.getY();
				double dx1 = p1.getX() - center.getX();
				double dy1 = p1.getY() - center.getY();
				double angle0 = Math.atan2(dy0, dx0);
				double angle1 = Math.atan2(dy1, dx1);
				return Double.compare(angle0, angle1);
			}
		};
	}

	public static Point2D rotate(Point2D rot, double angleRad, Point2D perspective) {

		double x0 = perspective.getX();
		double y0 = perspective.getY();
		double x1 = rot.getX();
		double y1 = rot.getY();
		double dx = x1 - x0;
		double dy = y1 - y0;
		double sa = Math.sin(angleRad);
		double ca = Math.cos(angleRad);
		double nx = ca * dx - sa * dy;
		double ny = sa * dx + ca * dy;
		return new Point2D.Double(x0 + nx, y0 + ny);
	}

	public static Point2D nudge(Point2D rot, double angleRad, Point2D perspective, double nudge) {
		double x0 = perspective.getX();
		double y0 = perspective.getY();
		double x1 = rot.getX();
		double y1 = rot.getY();
		double dx = x1 - x0;
		double dy = y1 - y0;
		if (dx != 0 || dy != 0) {
			nudge = (1 + nudge / Math.hypot(dx, dy));
			dx *= nudge;
			dy *= nudge;
		}
		double sa = Math.sin(angleRad);
		double ca = Math.cos(angleRad);
		double nx = ca * dx - sa * dy;
		double ny = sa * dx + ca * dy;
		return new Point2D.Double(x0 + nx, y0 + ny);
	}

	public static Point2D shift(Point2D point, double angleRad, double dist) {
		double sa = Math.sin(angleRad);
		double ca = Math.cos(angleRad);
		return new Point2D.Double(point.getX() + dist * ca, point.getY() + dist * sa);

	}

	static final double radiansToCheck = Math.PI / 3;

	public static void makeBlockingPoints(Point2D origin, List<Point2D> pointArray, double dist,
			List<Point2D> blockingArray) {
		// Loop through array
		for (int i = 0; i < pointArray.size(); i++) {
			Point2D point1 = pointArray.get(i);
			// Check if close to origin, then skip
			if (point1.distance(origin) < 1) {
				continue;
			}
			for (int j = (i + 1) % pointArray.size(); j != i; j = (j + 1) % pointArray.size()) {
				Point2D point2 = pointArray.get(j);
				// Check if close to origin, then skip
				if (point2.distance(origin) < 1) {
					continue;
				}
				// Compare angles
				double angle1 = Math.atan2(point1.getX() - origin.getX(), point1.getY() - origin.getY());
				double angle2 = Math.atan2(point2.getX() - origin.getX(), point2.getY() - origin.getY());
				// Break if past angle limit
				if (((angle1 - angle2) % TAU + TAU) % TAU > radiansToCheck) {
					break;
				}
				// If in range, add to blocked points and remove everything in between
				if (point1.distance(point2) <= dist) {
					blockingArray.add(point1);
					blockingArray.add(point2);
					// Remove
					// Special case: End of array
					if (j < i) {
						// Remove end of list after i
						while (pointArray.size() > i + 1) {
							pointArray.remove(i + 1);
						}
						// Remove at the beginning of the list j times
						for (int k = 0; k < j; k++) {
							pointArray.remove(0);
							i--; // Adjust position
						}
					} else {
						// Remove j-i-1 elements at spot i+1
						for (int k = 0; k < j - i - 1; k++) {
							pointArray.remove(i + 1);
						}
					}
				}
			}
		}
	}

	public static Point2D getClosestPointOnSegment(Line2D seg, Point2D p) {
		return getClosestPointOnSegment(seg.getP1(), seg.getP2(), p);
	}

	/**
	 * Returns closest point on segment to point
	 * 
	 * @param ss segment start point
	 * @param se segment end point
	 * @param p  point to found closest point on segment
	 * @return closest point on segment to p
	 */
	public static Point2D getClosestPointOnSegment(Point2D ss, Point2D se, Point2D p) {
		return getClosestPointOnSegment(ss.getX(), ss.getY(), se.getX(), se.getY(), p.getX(), p.getY());
	}

	/**
	 * Returns closest point on segment to point
	 * 
	 * @param sx1 segment x coord 1
	 * @param sy1 segment y coord 1
	 * @param sx2 segment x coord 2
	 * @param sy2 segment y coord 2
	 * @param px  point x coord
	 * @param py  point y coord
	 * @return closest point on segment to point
	 */
	public static Point2D getClosestPointOnSegment(double sx1, double sy1, double sx2, double sy2, double px,
			double py) {
		double xDelta = sx2 - sx1;
		double yDelta = sy2 - sy1;

		if ((xDelta == 0) && (yDelta == 0)) {
			return new Point2D.Double(sx1, sy1);
		}

		double u = ((px - sx1) * xDelta + (py - sy1) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

		final Point2D closestPoint;
		if (u < 0) {
			closestPoint = new Point2D.Double(sx1, sy1);
		} else if (u > 1) {
			closestPoint = new Point2D.Double(sx2, sy2);
		} else {
			closestPoint = new Point2D.Double(sx1 + u * xDelta, sy1 + u * yDelta);
		}

		return closestPoint;
	}

	static void findPointsOfInterest(Point2D origin, double dist, List<Point2D> points,
			List<Point2D> pointsOfInterest) {
		double distSq = dist * dist;
		// Add point if close enough
		for (Point2D p : points) {
			if (origin.distanceSq(p) < distSq) {
				pointsOfInterest.add(p);
			}
		}
	}

	// Comparator for sorting by descending
//	private static final Comparator<Double> descending = (Double d1, Double d2) -> {
//		if (d1 > d2) {
//			return 1;
//		}
//		if (d2 > d1) {
//			return -1;
//		}
//		return 0;
//	};
	// Comparator for sorting by descending
	private static final Comparator<Double> descending = (Double d1, Double d2) -> {
		if (d1 > d2) {
			return -1;
		}
		if (d2 > d1) {
			return 1;
		}
		return 0;
	};

	// Sort list of points by values in map, descending
	static List<Point2D> sortList(List<Point2D> points, Map<Point2D, Double> vals) {
		// Use tree map to
		TreeMap<Double, Point2D> sorted = new TreeMap<Double, Point2D>(descending);
		for (Point2D p : points) {
			if (!vals.containsKey(p)) {
				throw new RuntimeException("Point not found in map.");
			}
			sorted.put(vals.get(p), p);
		}
		// Invert
		return new ArrayList<Point2D>(sorted.values());
	}

	// Quicksort list from index
}
