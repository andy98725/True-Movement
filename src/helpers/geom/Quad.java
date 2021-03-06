package helpers.geom;

import java.awt.geom.*;
import java.util.ArrayList;

import helpers.Util;

@SuppressWarnings("serial")
public class Quad extends QuadCurve2D.Double implements Curve {

	@SuppressWarnings("unused")
	private final boolean isConvex, isConcave;
	private static final double SPLIT_COUNT = 16;

	// For segment on segment estimation
	private final ArrayList<Point2D> approxPts;
	private final ArrayList<java.lang.Double> approxDists, approxTimes;

	public Quad(double[] pcoords, double[] coords) {
		this(pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3]);
	}

	public Quad(double x0, double y0, double x1, double y1, double x2, double y2) {
		super(x0, y0, x1, y1, x2, y2);

		isConvex = Util.orientation(x0, y0, x1, y1, x2, y2) > 0;
		isConcave = Util.orientation(x0, y0, x1, y1, x2, y2) < 0;

		approxPts = new ArrayList<Point2D>();
		approxTimes = new ArrayList<java.lang.Double>();
		approxDists = new ArrayList<java.lang.Double>();
		double distCovered = 0;
		for (double t = 0; t <= 1; t += 1.0 / SPLIT_COUNT) {
			approxTimes.add(t);
			Point2D p = eval(t);
			if (t == 0) {
				approxDists.add(0.0);
			} else {
				distCovered += p.distance(approxPts.get(approxPts.size() - 1));
				approxDists.add(distCovered);
			}
			approxPts.add(p);
		}
	}

	public Quad getConvexCurve() {
		if (isConvex)
			return this;
		else
			return null;
	}

	@Override
	public boolean isConvex() {
		return isConvex;
	}

	@Override
	public double[] getTangentPoints(double x, double y) {
		final double a = x1, b = ctrlx, c = x2;
		final double j = y1, k = ctrly, l = y2;

		// Calculate divisor
		double div = 2 * (a * k - a * l - b * j + b * l + c * j - c * k);
		if (div == 0) {
			return new double[] {};
		}

		double base = 2 * a * k - a * l - a * y - 2 * b * j + 2 * b * y + c * j - c * y + j * x - 2 * k * x + l * x;
		double rt = Math
				.pow(2 * a * k - a * l - a * y - 2 * b * j + 2 * b * y + c * j - c * y + j * x - 2 * k * x + l * x, 2);
		rt += 4 * (a * k - a * l - b * j + b * l + c * j - c * k) * (-a * k + a * y + b * j - b * y - j * x + k * x);
		if (rt < 0) {
			return new double[] {};
		}

		double t0 = (base - Math.sqrt(rt)) / div;
		double t1 = (base + Math.sqrt(rt)) / div;

		Point2D p0 = (t0 >= 0 && t0 <= 1) ? eval(t0) : null;
		Point2D p1 = (t1 >= 0 && t1 <= 1) ? eval(t1) : null;

		if (p0 != null && p1 != null) {
			return new double[] { p0.getX(), p0.getY(), p1.getX(), p1.getY() };
		} else if (p0 != null) {
			return new double[] { p0.getX(), p0.getY() };
		} else if (p1 != null) {
			return new double[] { p1.getX(), p1.getY() };
		} else {
			return new double[] {};
		}
	}

	@Override
	public double[] getTangentTimes(double x, double y) {
		final double a = x1, b = ctrlx, c = x2;
		final double j = y1, k = ctrly, l = y2;

		// Calculate divisor
		double div = 2 * (a * k - a * l - b * j + b * l + c * j - c * k);
		if (div == 0) {
			return new double[] {};
		}

		double base = 2 * a * k - a * l - a * y - 2 * b * j + 2 * b * y + c * j - c * y + j * x - 2 * k * x + l * x;
		double rt = Math
				.pow(2 * a * k - a * l - a * y - 2 * b * j + 2 * b * y + c * j - c * y + j * x - 2 * k * x + l * x, 2);
		rt += 4 * (a * k - a * l - b * j + b * l + c * j - c * k) * (-a * k + a * y + b * j - b * y - j * x + k * x);
		if (rt < 0) {
			return new double[] {};
		}

		double t0 = (base - Math.sqrt(rt)) / div;
		double t1 = (base + Math.sqrt(rt)) / div;

		// Return either in valid range
		if (t0 >= 0 && t0 <= 1) {
			if (t1 >= 0 && t1 <= 1) {
				return new double[] { t0, t1 };
			} else {
				return new double[] { t0 };
			}
		} else {
			if (t1 >= 0 && t1 <= 1) {
				return new double[] { t1 };
			} else {
				return new double[] {};
			}
		}
	}

	private boolean testTangent(int i, double x, double y) {

		Point2D p = approxPts.get(i);
		Point2D p1 = approxPts.get(i - 1);
		Point2D p2 = approxPts.get(i + 1);

		double oPrev = Util.orientation(x, y, p.getX(), p.getY(), p1.getX(), p1.getY());
		double oNext = Util.orientation(x, y, p2.getX(), p2.getY(), p.getX(), p.getY());

		if (oPrev != oNext && (i == 1 || oPrev != 0)) {
			// Potential tangent
			if (Cubic.ALLOW_INTERSECTING_TANS) {
				return true;
			} else {
				// Detect any crosses
				Point2D p3 = approxPts.get(0);
				boolean allow = true;
				for (int j = 0; allow && j < i; j++) {
					Point2D p4 = approxPts.get(j);
					if (Util.orientation(x, y, p.getX(), p.getY(), p3.getX(), p3.getY()) != Util.orientation(x, y,
							p.getX(), p.getY(), p4.getX(), p4.getY())
							&& Util.orientation(p3.getX(), p3.getY(), p4.getX(), p4.getY(), x, y) != Util
									.orientation(p3.getX(), p3.getY(), p4.getX(), p4.getY(), p.getX(), p.getY())) {
						allow = false;
					}
				}
				p3 = approxPts.get(approxPts.size() - 1);
				for (int j = i + 1; allow && j < approxPts.size(); j++) {
					Point2D p4 = approxPts.get(j);
					if (Util.orientation(x, y, p.getX(), p.getY(), p3.getX(), p3.getY()) != Util.orientation(x, y,
							p.getX(), p.getY(), p4.getX(), p4.getY())
							&& Util.orientation(p3.getX(), p3.getY(), p4.getX(), p4.getY(), x, y) != Util
									.orientation(p3.getX(), p3.getY(), p4.getX(), p4.getY(), p.getX(), p.getY())) {
						allow = false;
					}
				}
				if (allow) {
					return true;
				}
			}
		}
		return false;
	}

	public QuadCurve2D subCurve(double t1, double t2) {
		if (t1 >= t2) {
			throw new RuntimeException("t1 must < t2");
		}
		if (t1 == 0 && t2 == 1)
			return this;

		QuadCurve2D sub = new QuadCurve2D.Double();
		splitCurve(t2, x1, y1, ctrlx, ctrly, x2, y2, sub, false);
		splitCurve(t1 / t2, sub.getX1(), sub.getY1(), sub.getCtrlX(), sub.getCtrlY(), sub.getX2(), sub.getY2(), sub,
				true);
		return sub;
	}

	// Split curve at point in time
	public void splitCurve(double t, QuadCurve2D prev, QuadCurve2D next) {
		if (prev != null)
			splitCurve(t, x1, y1, ctrlx, ctrly, x2, y2, prev, false);
		if (next != null)
			splitCurve(t, x1, y1, ctrlx, ctrly, x2, y2, next, true);
	}

	private static void splitCurve(double t, double x1, double y1, double x2, double y2, double x3, double y3,
			QuadCurve2D curve, boolean flip) {
		if (flip) {
			t = 1 - t;
			double xt = x1;
			double yt = y1;
			x1 = x3;
			y1 = y3;
			x3 = xt;
			y3 = yt;
		}
		double x12 = (x2 - x1) * t + x1;
		double y12 = (y2 - y1) * t + y1;
		double x23 = (x3 - x2) * t + x2;
		double y23 = (y3 - y2) * t + y2;

		double x123 = (x23 - x12) * t + x12;
		double y123 = (y23 - y12) * t + y12;

		// Maintain original orientation
		if (flip)
			curve.setCurve(x123, y123, x12, y12, x1, y1);
		else
			curve.setCurve(x1, y1, x12, y12, x123, y123);
	}

	// Do through rough approximation
	@Override
	public double[] getTangentLines(Curve other) {
		if (other instanceof Cubic) {
			// This method is faster, because it uses quad getTangentPoints.
			return other.getTangentLines(this);
		} else if (other instanceof Quad) {
			ArrayList<Point2D> foundPoints = new ArrayList<Point2D>();
			ArrayList<Point2D> otherPoints = new ArrayList<Point2D>();

			// Brute force each point
			for (int i = 1; i < approxPts.size() - 1; i++) {
				Point2D p = approxPts.get(i);
				double[] tans = other.getTangentPoints(p.getX(), p.getY());
				// Test tangent back
				for (int j = 0; j < tans.length; j += 2) {
					if (testTangent(i, tans[j], tans[j + 1])) {
						foundPoints.add(p);
						otherPoints.add(new Point2D.Double(tans[j], tans[j + 1]));
					}

				}
			}

			// Combine into an array of lines
			double[] allLines = new double[foundPoints.size() * 4];
			for (int i = 0; i < foundPoints.size(); i++) {
				Point2D p = foundPoints.get(i);
				Point2D p2 = otherPoints.get(i);
				allLines[4 * i] = p.getX();
				allLines[4 * i + 1] = p.getY();
				allLines[4 * i + 2] = p2.getX();
				allLines[4 * i + 3] = p2.getY();
			}
			return allLines;
		} else {
			throw new RuntimeException();
		}
	}

	// Approximate times of tangent
	public double[][] getTangentTimes(Curve other) {
		if (other instanceof Cubic) {
			// This method is faster, because it uses quad getTangentTimes.
			double[][] ret = other.getTangentTimes(this);
			for (int i = 0; i < ret.length; i++) {
				double tmp = ret[i][0];
				ret[i][0] = ret[i][1];
				ret[i][1] = tmp;
			}
			return ret;
		} else if (other instanceof Quad) {
			ArrayList<double[]> foundPairs = new ArrayList<double[]>();

			// Brute force each point
			for (int i = 1; i < approxPts.size() - 1; i++) {
				Point2D p = approxPts.get(i);
				double[] tans = other.getTangentTimes(p.getX(), p.getY());
				// Test tangent back
				for (double t2 : tans) {
					Point2D p2 = other.eval(t2);
					if (testTangent(i, p2.getX(), p2.getY()))
						foundPairs.add(new double[] { approxTimes.get(i), t2 });
				}
			}

			// Combine into an array of times
			double[][] pairs = new double[foundPairs.size()][];
			for (int i = 0; i < pairs.length; i++) {
				pairs[i] = foundPairs.get(i);

			}
			return pairs;
		} else {
			throw new RuntimeException();
		}
	}

	@Override
	public boolean intersectsLine(double x1, double y1, double x2, double y2) {
		if (!getBounds().intersectsLine(x1, y1, x2, y2)) {
			return false;
		}

		for (int i = 1; i < approxPts.size(); i++) {
			if (Util.linesIntersect(x1, y1, x2, y2, approxPts.get(i - 1).getX(), approxPts.get(i - 1).getY(),
					approxPts.get(i).getX(), approxPts.get(i).getY())) {
				return true;
			}
		}
		return false;
	}

	// Approx closest location on point
	@Override
	public double getClosestTime(double x, double y) {
		if (x1 == x && y1 == y)
			return 0;

		if (x2 == x && y2 == y)
			return 1;

		double sqDist = java.lang.Double.MAX_VALUE;
		double time = -1;
		for (int i = 0; i < approxTimes.size(); i++) {
			double dist = Point2D.distanceSq(x, y, approxPts.get(i).getX(), approxPts.get(i).getY());
			if (dist < sqDist) {
				double t = approxTimes.get(i);
				if (dist == 0)
					return t;

				sqDist = dist;
				time = t;

			}
		}
		return time;
	}

	@Override
	public double distanceAlongCurve(double t1, double t2) {
		return Math.abs(approxDists.get(getApproxTimeIndex(t1)) - approxDists.get(getApproxTimeIndex(t2)));
	}

	private int getApproxTimeIndex(double t) {
		// Binary search time array
		int iMin = 0, iMax = approxTimes.size() - 1;

		while (iMin + 1 < iMax) {
			int index = (iMin + iMax) / 2;
			if (approxTimes.get(index) > t) {
				iMax = index;
			} else if (approxTimes.get(index + 1) < t) {
				iMin = index;
			} else {
				// Between index and index+1
				iMin = index;
				iMax = index + 1;
			}
		}

		if (t <= (approxTimes.get(iMin) + approxTimes.get(iMax) / 2))
			return iMin;
		else
			return iMax;

	}

	public Point2D eval(double t) {
		return eval(t, x1, y1, ctrlx, ctrly, x2, y2);
	}

	public static Point2D eval(double t, QuadCurve2D curve) {
		return eval(t, curve.getX1(), curve.getY1(), curve.getCtrlX(), curve.getCtrlY(), curve.getX2(), curve.getY2());
	}

	public static Point2D eval(double t, double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
		double mt = 1 - t;
		return new Point2D.Double(mt * mt * x1 + 2 * mt * t * ctrlx + t * t * x2,
				mt * mt * y1 + 2 * mt * t * ctrly + t * t * y2);

	}

	@Override
	public double getCX1() {
		return this.getCtrlX();
	}

	@Override
	public double getCY1() {
		return this.getCtrlY();
	}

	@Override
	public double getCX2() {
		return this.getCtrlX();
	}

	@Override
	public double getCY2() {
		return this.getCtrlY();
	}

	@Override
	public Area getProjection(double t, double r) {
		// TODO
		Point2D origin = eval(t);
		r /= 20;
		return new Area(new Ellipse2D.Double(origin.getX() - r, origin.getY() - r, 2 * r, 2 * r));
	}

	@Override
	public Point2D getRaycastPoint(double t) {
		Point2D ret = eval(t);
		if (t <= 0.5) {
			// Move towards ctrl and away from p2
			double ac = Math.atan2(ctrly - ret.getY(), ctrlx - ret.getX());
			double ap = Math.atan2(y2 - ret.getY(), x2 - ret.getX());
			ret.setLocation(ret.getX() + Cubic.EPSILON * (Math.cos(ac) - Cubic.EPSILON * (Math.cos(ap))),
					ret.getY() + Cubic.EPSILON * (Math.sin(ac) - Cubic.EPSILON * (Math.sin(ap))));

		}
		if (t >= 0.5) {
			// Move towards ctrl and away from p1
			double ac = Math.atan2(ctrly - ret.getY(), ctrlx - ret.getX());
			double ap = Math.atan2(y1 - ret.getY(), x1 - ret.getX());
			ret.setLocation(ret.getX() + Cubic.EPSILON * (Math.cos(ac) - Cubic.EPSILON * (Math.cos(ap))),
					ret.getY() + Cubic.EPSILON * (Math.sin(ac) - Cubic.EPSILON * (Math.sin(ap))));
		}
		return ret;
	}

	public boolean equals(Quad q) {
		return x1 == q.x1 && y1 == q.y1 && ctrlx == q.ctrlx && ctrly == q.ctrly && x2 == q.x2 && y2 == q.y2;
	}

	@Override
	public String toString() {
		return "{Quad (" + x1 + ", " + y1 + ") -> (" + x2 + ", " + y2 + ") control (" + ctrlx + ", " + ctrly + ")}";
	}
}
