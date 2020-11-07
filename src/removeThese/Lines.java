package removeThese;

import java.awt.geom.*;
import java.util.*;

public class Lines {
	/**
	 * Rotate the given line around its starting point, by the given angle, and
	 * stores the result in the given result line. If the result line is
	 * <code>null</code>, then a new line will be created and returned.
	 * 
	 * @param line     The line
	 * @param angleRad The rotation angle
	 * @param The      result line
	 * @return The result line
	 */
	static Line2D rotate(Line2D line, double angleRad, Line2D result) {
		double x0 = line.getX1();
		double y0 = line.getY1();
		double x1 = line.getX2();
		double y1 = line.getY2();
		double dx = x1 - x0;
		;
		double dy = y1 - y0;
		double sa = Math.sin(angleRad);
		double ca = Math.cos(angleRad);
		double nx = ca * dx - sa * dy;
		double ny = sa * dx + ca * dy;
		if (result == null) {
			result = new Line2D.Double();
		}
		result.setLine(x0, y0, x0 + nx, y0 + ny);
		return result;
	}// Take rays and find the intersection closest to origin

	static List<Line2D> getLinesInRadius(List<Line2D> lines, Point2D origin, double radius, double lineDetail) {
		final double sqDist = radius * radius;
		List<Line2D> ret = new ArrayList<Line2D>();
		// Check each line in list, if closest point is close enough then include it
		for (Line2D line : lines) {
			if (origin.distanceSq(Points.getClosestPointOnSegment(line, origin)) < sqDist) {
				ret.add(line);
			}
		}
		// Add border
		Area circle = new Area(
				new Ellipse2D.Double(origin.getX() - radius, origin.getY() - radius, 2 * radius, 2 * radius));
		// Add to array
		ret.addAll(Shapes.computeLineSegments(circle, lineDetail));
		return ret;
	}

	static List<Point2D> getPointsInRadius(List<Point2D> points, Point2D origin, double radius) {
		final double sqDist = radius * radius;
		List<Point2D> ret = new ArrayList<Point2D>();
		// Check each line in list, if closest point is close enough then include it
		for (Point2D p : points) {
			if (origin.distanceSq(p) < sqDist) {
				ret.add(p);
			}
		}
		return ret;
	}

	// Threshold of interesting in px dist
	static final double interestingThreshold = 1;

	static List<Point2D> getPointsOfInterest(List<Line2D> lines, Point2D origin) {
		List<Point2D> ret = new ArrayList<Point2D>();
		for (Line2D line : lines) {
			// Rotate around point
			Line2D ray = new Line2D.Double(origin, line.getP1());
			Line2D rot0 = Lines.rotate(ray, +deltaRad, null);
			Line2D rot1 = Lines.rotate(ray, -deltaRad, null);
			// Compute closest
			Point2D point0 = computeClosestIntersection(rot0, lines);
			Point2D point1 = computeClosestIntersection(rot1, lines);
			// If far enough away, interesting point
			if (point0.distanceSq(point1) > interestingThreshold) {
				ret.add(line.getP1());
			}
		}
		return ret;

	}

	static List<Point2D> extendPoints(List<Point2D> points, Point2D origin, double amt) {
		List<Point2D> ret = new ArrayList<Point2D>();
		for (Point2D point : points) {
			if (point.getX() != origin.getX() || point.getY() != origin.getY()) {
				double hypot = point.distance(origin);
				ret.add(new Point2D.Double(point.getX() + (point.getX() - origin.getX()) * amt / hypot,
						point.getY() + (point.getY() - origin.getY()) * amt / hypot));
//				point.setLocation(point.getX() + (point.getX() - origin.getX()) * amt / hypot,
//						point.getY() + (point.getY() - origin.getY()) * amt / hypot);
			}
		}

		return ret;
	}

	static List<Point2D> computeClosestIntersections(List<Line2D> rays, List<Line2D> segments) {
		List<Point2D> closestIntersections = new ArrayList<Point2D>();
		for (Line2D ray : rays) {
			Point2D closestIntersection = computeClosestIntersection(ray, segments);
			if (closestIntersection != null) {
				closestIntersections.add(closestIntersection);
			}
		}
		return closestIntersections;
	}

	static List<Point2D> computeClosestIntersectionsLimited(List<Line2D> rays, List<Line2D> segments, double radius) {
		double radiusSq = radius * radius;
		List<Point2D> closestIntersections = new ArrayList<Point2D>();
		for (Line2D ray : rays) {
			Point2D closestIntersection = computeClosestIntersection(ray, segments);
			if (closestIntersection != null) {
				if (ray.getP1().distanceSq(closestIntersection) <= radiusSq) {
					closestIntersections.add(closestIntersection);
				} else { // Out of range. make a new one
					Point2D p1 = ray.getP1(), p2 = ray.getP2();
					double sizeMult = radius / p1.distance(p2);
					Point2D end = new Point2D.Double(p1.getX() + (p2.getX() - p1.getX()) * sizeMult,
							p1.getY() + (p2.getY() - p1.getY()) * sizeMult);
					closestIntersections.add(end);

				}

			} else {
				// Calculate point at the end of the ray given radius
				Point2D p1 = ray.getP1(), p2 = ray.getP2();
				double sizeMult = radius / p1.distance(p2);
				Point2D end = new Point2D.Double(p1.getX() + (p2.getX() - p1.getX()) * sizeMult,
						p1.getY() + (p2.getY() - p1.getY()) * sizeMult);
				closestIntersections.add(end);
			}
		}
		return closestIntersections;
	}

	protected static final double EPSILON = 1e-8;

	// Compute closest intersection given ray and shape
	protected static Point2D computeClosestIntersection(Line2D ray, List<Line2D> segments) {
		Point2D relativeLocation = new Point2D.Double();
		Point2D absoluteLocation = new Point2D.Double();
		Point2D closestIntersection = null;
		double minRelativeDistance = Double.MAX_VALUE;
		for (Line2D lineSegment : segments) {
			boolean intersect = Intersection.intersectLineLine(ray, lineSegment, relativeLocation, absoluteLocation);
			if (intersect) {
				if (relativeLocation.getY() >= -EPSILON && relativeLocation.getY() <= 1 + EPSILON) {
					if (relativeLocation.getX() >= -EPSILON && relativeLocation.getX() < minRelativeDistance) {
						minRelativeDistance = relativeLocation.getX();
						closestIntersection = new Point2D.Double(absoluteLocation.getX(), absoluteLocation.getY());
					}
				}
			}

		}
		return closestIntersection;
	}

	protected static double computeClosestIntersectionAlongLine(Line2D ray, List<Line2D> segments) {
		Point2D closest = computeClosestIntersection(ray, segments);
		if (closest == null) {
			return Double.MAX_VALUE;
		}
		if (ray.getX1() != ray.getX2()) {
			// Use x diff
			return (closest.getX() - ray.getX1()) / (ray.getX2() - ray.getX1());
		} else if (ray.getY1() != ray.getY2()) {
			// Use y diff
			return (closest.getY() - ray.getY1()) / (ray.getY2() - ray.getY1());
		} else {
			// Line was just a point
			return 0;
		}
	}

	static List<Line2D> addCircleLinesAndReturn(List<Line2D> base, Point2D origin, double radius) {
		// Make circle
		Area circle = new Area(
				new Ellipse2D.Double(origin.getX() - radius, origin.getY() - radius, radius * 2, radius * 2));
		List<Line2D> ret = Shapes.computeLineSegments(circle, radius / 10);
		// Add current
		for (Line2D l : base) {
			ret.add(l);
		}
		return ret;
	}

	static final double TAU = 2 * Math.PI;

	// Take rays and find the intersection closest to origin, but with some
	// constraints
	static List<Point2D> computeClosestIntersectionsWithBlockage(Point2D origin, List<Line2D> rays,
			List<Line2D> segments, double baseAngle, double stoppingDist) {
		// Sort rays into CW from angle and CCW from angle
		List<Line2D> raysCW = new ArrayList<Line2D>();
		List<Line2D> raysCCW = new ArrayList<Line2D>();
		for (Line2D ray : rays) {
			raysCW.add(ray);
			raysCCW.add(ray);
		}
		// Now sort CW and CCW
		Collections.sort(raysCW, byAngleComparatorClockwise(baseAngle));
		Collections.sort(raysCCW, byAngleComparatorCounterClockwise(baseAngle));

		// Square dist for efficiency
		double distSq = stoppingDist * stoppingDist;
		// Intersection return array
		List<Point2D> closestIntersections = new ArrayList<Point2D>();
		// Now loop through each array and stop if it's closer than given value
		for (Line2D ray : raysCW) {
			Point2D closestIntersection = computeClosestIntersection(ray, segments);
			// If hit, add
			if (closestIntersection != null) {
				closestIntersections.add(closestIntersection);
				// Check distance
				double intersectionRad = (closestIntersection.getX() - origin.getX())
						* (closestIntersection.getX() - origin.getX())
						+ (closestIntersection.getY() - origin.getY()) * (closestIntersection.getY() - origin.getY());
				if (intersectionRad < distSq) {
					break;
				}
			}
		}
		// Now loop through each array and stop if it's closer than given value
		for (Line2D ray : raysCCW) {
			Point2D closestIntersection = computeClosestIntersection(ray, segments);
			// If hit, add
			if (closestIntersection != null) {
				closestIntersections.add(closestIntersection);
				// Check distance
				double intersectionRad = (closestIntersection.getX() - origin.getX())
						* (closestIntersection.getX() - origin.getX())
						+ (closestIntersection.getY() - origin.getY()) * (closestIntersection.getY() - origin.getY());
				if (intersectionRad < distSq) {
					break;
				}
			}
		}
		return closestIntersections;
	}

	public static Line2D raycastOffset(double x0, double y0, double x1, double y1, double rad, double offs,
			Area block) {
		// Calculate current hypot and angle
		double angle = Math.atan2(y1 - y0, x1 - x0) + offs;
		double hypot = Math.hypot(x1 - x0, y1 - y0);
		// Make new points
		x1 = x0 + hypot * Math.cos(angle);
		y1 = y0 + hypot * Math.sin(angle);
		// Do raycast
		return raycast(x0, y0, x1, y1, rad, block);
	}

	public static Line2D raycast(double x0, double y0, double x1, double y1, double rad, Area block) {
		// Calculate base line with radius
		double xd = x1 - x0, yd = y1 - y0, dlen = Math.hypot(xd, yd);
		// Safety
		if (dlen == 0)
			return new Line2D.Double(x0, y0, x0, y0);
		xd *= 1 / dlen;
		yd *= 1 / dlen;
		Line2D ret = new Line2D.Double(x0, y0, x0 + xd * rad, y0 + yd * rad);
		// Compute line segments
		List<Line2D> segments = Shapes.computeLineSegments(block, 1);
		Point2D collision = computeClosestIntersection(ret, segments);
		// If it collided, set end point
		if (collision != null && Math.hypot(collision.getX() - x0, collision.getY() - y0) <= rad) {
			ret.setLine(x0, y0, collision.getX(), collision.getY());
		}
		// Return result
		return ret;
	}

	final static double deltaRad = 0.0001;

	// Create rays from origin to each point in a list of lines.
	public static List<Line2D> createRays(Point2D origin, List<Line2D> segments) {

		List<Line2D> rays = new ArrayList<Line2D>();
		for (Line2D line : segments) {
			Line2D ray0 = new Line2D.Double(origin, line.getP1());
//			rays.add(ray0);
			rays.add(Lines.rotate(ray0, +deltaRad, null));
			rays.add(Lines.rotate(ray0, -deltaRad, null));

//			Line2D ray1 = new Line2D.Double(origin, line.getP2());
//			rays.add(ray1);
//			rays.add(Lines.rotate(ray1, +deltaRad, null));
//			rays.add(Lines.rotate(ray1, -deltaRad, null));

		}
		return rays;
	}

	/**
	 * Creates a comparator that compares points by the angle of the line between
	 * the point and the given center
	 * 
	 * @param center The center
	 * @return The comparator
	 */
	public static Comparator<Line2D> byAngleComparatorClockwise(final double baseAngle) {
		return new Comparator<Line2D>() {
			@Override
			public int compare(Line2D l0, Line2D l1) {
				double dx0 = l0.getX2() - l0.getX1();
				double dy0 = l0.getY2() - l0.getY1();
				double dx1 = l1.getX2() - l1.getX1();
				double dy1 = l1.getY2() - l1.getY1();
				double angle0 = Math.atan2(dy0, dx0) + baseAngle;
				double angle1 = Math.atan2(dy1, dx1) + baseAngle;
				// Normalize
				angle0 = (angle0 % TAU + TAU) % TAU;
				angle1 = (angle1 % TAU + TAU) % TAU;
				return Double.compare(angle0, angle1);
			}
		};
	}

	public static Comparator<Line2D> byAngleComparatorCounterClockwise(final double baseAngle) {
		return new Comparator<Line2D>() {
			@Override
			public int compare(Line2D l0, Line2D l1) {
				double dx0 = l0.getX2() - l0.getX1();
				double dy0 = l0.getY2() - l0.getY1();
				double dx1 = l1.getX2() - l1.getX1();
				double dy1 = l1.getY2() - l1.getY1();
				double angle0 = Math.atan2(dy0, dx0) + baseAngle;
				double angle1 = Math.atan2(dy1, dx1) + baseAngle;
				// Normalize
				angle0 = (angle0 % TAU + TAU) % TAU;
				angle1 = (angle1 % TAU + TAU) % TAU;
				return Double.compare(angle1, angle0);
			}
		};
	}
}