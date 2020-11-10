package helpers;

import java.awt.*;
import java.awt.geom.*;

import helpers.geom.Cubic;
import helpers.geom.Quad;

public class Util {

	public static Area createFlattenedArea(Area a, double flatness) {
		Path2D path = new Path2D.Double();
		path.append(new FlatteningPathIterator(a.getPathIterator(null), flatness), true);
		return new Area(path);
	}

	public static Point2D getPointOnArea(Area shape, Point2D nearby) {
		if (shape.isEmpty()) {
			return null;
		}

		Point2D closest = null;
		double distSq = Double.MAX_VALUE;
		double[] coords = new double[6];
		double[] pcoords = new double[2], mcoords = new double[2];
		for (PathIterator iter = shape.getPathIterator(null); !iter.isDone(); iter.next()) {
			int oper = iter.currentSegment(coords);
			switch (oper) {
			case PathIterator.SEG_MOVETO:
				System.arraycopy(coords, 0, mcoords, 0, 2);
				System.arraycopy(coords, 0, pcoords, 0, 2);
				break;
			case PathIterator.SEG_CLOSE:
				System.arraycopy(mcoords, 0, coords, 0, 2);
			case PathIterator.SEG_LINETO:
				Point2D p = getClosestPointOnSegment(coords[0], coords[1], coords[2], coords[3], nearby.getX(),
						nearby.getY());

				if (p.distanceSq(nearby) < distSq) {
					distSq = p.distanceSq(nearby);
					closest = p;
					if (distSq == 0)
						return closest;
				}

				// Copy coords[0,1] to pcoords[0,1]
				System.arraycopy(coords, 0, pcoords, 0, 2);
				break;
			case PathIterator.SEG_QUADTO:
				Quad q = new Quad(pcoords, coords);
				p = q.eval(q.getClosestTime(nearby.getX(), nearby.getY()));

				if (p.distanceSq(nearby) < distSq) {
					distSq = p.distanceSq(nearby);
					closest = p;
					if (distSq == 0)
						return closest;
				}

				// Copy coords[2,3] to pcoords[0,1]
				System.arraycopy(coords, 2, pcoords, 0, 2);
			case PathIterator.SEG_CUBICTO:
				Cubic c = new Cubic(pcoords, coords);
				p = c.eval(c.getClosestTime(nearby.getX(), nearby.getY()));

				if (p.distanceSq(nearby) < distSq) {
					distSq = p.distanceSq(nearby);
					closest = p;
					if (distSq == 0)
						return closest;
				}

				// Copy coords[4,5] to pcoords[0,1]
				System.arraycopy(coords, 4, pcoords, 0, 2);

			}
		}
		return closest;
	}

	private static Point2D getClosestPointOnSegment(double sx1, double sy1, double sx2, double sy2, double px,
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

	public static int orientation(double x0, double y0, double x1, double y1, double x2, double y2) {
		return (int) Math.signum((y1 - y0) * (x2 - x1) - (y2 - y1) * (x1 - x0));
	}

	public static int areaComplexity(Shape a) {
		int size = 0;
		PathIterator i = a.getPathIterator(null);
		while (!i.isDone()) {
			size++;
			i.next();
		}
		return size;
	}

	/**
	 * Returns an indicator of where the specified point {@code (px,py)} lies with
	 * respect to the line segment from {@code (x1,y1)} to {@code (x2,y2)}. The
	 * return value can be either 1, -1, or 0 and indicates in which direction the
	 * specified line must pivot around its first end point, {@code (x1,y1)}, in
	 * order to point at the specified point {@code (px,py)}.
	 * <p>
	 * A return value of 1 indicates that the line segment must turn in the
	 * direction that takes the positive X axis towards the negative Y axis. In the
	 * default coordinate system used by Java 2D, this direction is
	 * counterclockwise.
	 * <p>
	 * A return value of -1 indicates that the line segment must turn in the
	 * direction that takes the positive X axis towards the positive Y axis. In the
	 * default coordinate system, this direction is clockwise.
	 * <p>
	 * A return value of 0 indicates that the point lies exactly on the line
	 * segment. Note that an indicator value of 0 is rare and not useful for
	 * determining collinearity because of floating point rounding issues.
	 * <p>
	 * If the point is colinear with the line segment, but not between the end
	 * points, then the value will be -1 if the point lies "beyond {@code (x1,y1)}"
	 * or 1 if the point lies "beyond {@code (x2,y2)}".
	 *
	 * @param x1 the X coordinate of the start point of the specified line segment
	 * @param y1 the Y coordinate of the start point of the specified line segment
	 * @param x2 the X coordinate of the end point of the specified line segment
	 * @param y2 the Y coordinate of the end point of the specified line segment
	 * @param px the X coordinate of the specified point to be compared with the
	 *           specified line segment
	 * @param py the Y coordinate of the specified point to be compared with the
	 *           specified line segment
	 * @return an integer that indicates the position of the third specified
	 *         coordinates with respect to the line segment formed by the first two
	 *         specified coordinates.
	 * @since 1.2
	 */
	public static int relativeCCW(double x1, double y1, double x2, double y2, double px, double py) {
		x2 -= x1;
		y2 -= y1;
		px -= x1;
		py -= y1;
		double ccw = px * y2 - py * x2;
		if (ccw == 0.0) {
			// The point is colinear, classify based on which side of
			// the segment the point falls on. We can calculate a
			// relative value using the projection of px,py onto the
			// segment - a negative value indicates the point projects
			// outside of the segment in the direction of the particular
			// endpoint used as the origin for the projection.
			ccw = px * x2 + py * y2;
			if (ccw > 0.0) {
				// Reverse the projection to be relative to the original x2,y2
				// x2 and y2 are simply negated.
				// px and py need to have (x2 - x1) or (y2 - y1) subtracted
				// from them (based on the original values)
				// Since we really want to get a positive answer when the
				// point is "beyond (x2,y2)", then we want to calculate
				// the inverse anyway - thus we leave x2 & y2 negated.
				px -= x2;
				py -= y2;
				ccw = px * x2 + py * y2;
				if (ccw < 0.0) {
					ccw = 0.0;
				}
			}
		}
		return (ccw < 0.0) ? -1 : ((ccw > 0.0) ? 1 : 0);
	}

	public static boolean linesIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4,
			double y4) {
		return ((relativeCCW(x1, y1, x2, y2, x3, y3) * relativeCCW(x1, y1, x2, y2, x4, y4) < 0)
				&& (relativeCCW(x3, y3, x4, y4, x1, y1) * relativeCCW(x3, y3, x4, y4, x2, y2) < 0));
	}

	private static final double EPSILON = 1E-2;

	public static Point2D inchTowards(double x, double y, double dx, double dy) {
		double t = Math.atan2(dy - y, dx - x);
		return new Point2D.Double(x + EPSILON * Math.cos(t), y + EPSILON * Math.sin(t));
	}

	public static Shape extendArea(Shape a, double dist) {
		if (dist < 1)
			return a;
		Stroke extendStroke = new BasicStroke(-1 + 2 * (float) dist, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
		return extendStroke.createStrokedShape(a);
	}
}
