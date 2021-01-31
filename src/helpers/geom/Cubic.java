package helpers.geom;

import java.awt.Color;
import java.awt.geom.*;
import java.util.ArrayList;

import helpers.Util;

@SuppressWarnings("serial")
public class Cubic extends CubicCurve2D.Double implements Curve {

	protected static final int SPLIT_COUNT = 128;

	protected static final boolean ALLOW_INTERSECTING_TANS = false;

	private final CubicCurve2D convexCurve, concaveCurve;

	private final ArrayList<Point2D> approxPts;
	private final ArrayList<java.lang.Double> approxTimes;
	private final ArrayList<java.lang.Double> approxDists;

	private final Point2D Ptest1, Ptest2;

	public Cubic(CubicCurve2D other) {
		this(other.getX1(), other.getY1(), other.getCtrlX1(), other.getCtrlY1(), other.getCtrlX2(), other.getCtrlY2(),
				other.getX2(), other.getY2());
	}

	public Cubic(double[] pcoords, double[] coords) {
		this(pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
	}

	public Cubic(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
		super(x0, y0, x1, y1, x2, y2, x3, y3);

		Ptest1 = new Point2D.Double(2 * x0 - x1, 2 * y0 - y1);
		Ptest2 = new Point2D.Double(2 * x3 - x2, 2 * y3 - y2);

		// Determine concavity pair
		// Split into subcurves (necessarily convex/concave each)
		CubicCurve2D.Double c1 = new CubicCurve2D.Double(), c2 = new CubicCurve2D.Double();
		splitCurve(0.5, c1, c2);
		final double o1 = Util.orientation(c1.x1, c1.y1, (c1.ctrlx1 + c1.ctrlx2) / 2, (c1.ctrly1 + c1.ctrly2) / 2,
				c1.x2, c1.y2);
		final double o2 = Util.orientation(c2.x1, c2.y1, (c2.ctrlx1 + c2.ctrlx2) / 2, (c2.ctrly1 + c2.ctrly2) / 2,
				c2.x2, c2.y2);

		// Is it wholly concave/convex?
		if (o1 == o2) {
			if (o1 > 0) {
				convexCurve = this;
				concaveCurve = null;
			} else if (o1 < 0) {
				convexCurve = null;
				concaveCurve = this;
			} else {
				convexCurve = null;
				concaveCurve = null;
			}
		} else {
			if (o1 > 0) {
				// c1 is convex
				convexCurve = c1;
				if (o2 < 0) {
					concaveCurve = c2;
				} else {
					concaveCurve = null;
				}
			} else if (o1 < 0) {
				// c1 is concave
				concaveCurve = c1;
				if (o2 > 0) {
					convexCurve = c2;
				} else {
					convexCurve = null;
				}
			} else {
				// c1 is linear; c2 is convex or concave
				if (o2 > 0) {
					convexCurve = c2;
					concaveCurve = null;
				} else {
					concaveCurve = c2;
					convexCurve = null;
				}
			}
		}

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

	public Cubic getConvexCurve() {
		if (convexCurve == null)
			return null;
		else if (convexCurve == this)
			return this;
		else {
			return new Cubic(convexCurve.getX1(), convexCurve.getY1(), convexCurve.getCtrlX1(), convexCurve.getCtrlY1(),
					convexCurve.getCtrlX2(), convexCurve.getCtrlY2(), convexCurve.getX2(), convexCurve.getCtrlY2());
		}
	}

	public boolean isConvex() {
		return convexCurve == this;
	}

	// Is it wholly convex or concave?
	public boolean isRegular() {
		return convexCurve == this || concaveCurve == this;
	}

	protected static Color CONVEX = new Color(255, 127, 127);
	protected static Color CONCAVE = new Color(127, 127, 255);

	public double[] getTangentPoints(double x, double y) {
		ArrayList<Point2D> foundPoints = new ArrayList<Point2D>();

		// Use flattening approximation
		for (int i = 0; i < approxPts.size(); i++) {
			if (testTangent(i, x, y)) {
				foundPoints.add(approxPts.get(i));
			}
		}
		// Convert to primitive
		double[] ret = new double[2 * foundPoints.size()];
		for (int i = 0; i < foundPoints.size(); i++) {
			ret[2 * i] = foundPoints.get(i).getX();
			ret[2 * i + 1] = foundPoints.get(i).getY();
		}
		return ret;
	}

	private boolean testTangent(int i, double x, double y) {
		Point2D p = approxPts.get(i);
		Point2D p1 = i > 0 ? approxPts.get(i - 1) : Ptest1;
		Point2D p2 = i < approxPts.size() - 1 ? approxPts.get(i + 1) : Ptest2;

		double oPrev = Util.orientation(x, y, p.getX(), p.getY(), p1.getX(), p1.getY());
		double oNext = Util.orientation(x, y, p2.getX(), p2.getY(), p.getX(), p.getY());

		if (oPrev != oNext && (i == 0 || oPrev != 0)) {
			// Potential tangent
			if (ALLOW_INTERSECTING_TANS) {
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

	public CubicCurve2D subCurve(double t1, double t2) {
		if (t1 >= t2) {
			throw new RuntimeException("t1 must < t2");
		}
		if (t1 == 0 && t2 == 1)
			return this;

		CubicCurve2D sub = new CubicCurve2D.Double();
		splitCurve(t2, x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2, sub, false);
		splitCurve(t1 / t2, sub.getX1(), sub.getY1(), sub.getCtrlX1(), sub.getCtrlY1(), sub.getCtrlX2(),
				sub.getCtrlY2(), sub.getX2(), sub.getY2(), sub, true);
		return sub;
	}

	// Split curve at point in time
	public void splitCurve(double t, CubicCurve2D prev, CubicCurve2D next) {
		if (prev != null)
			splitCurve(t, x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2, prev, false);
		if (next != null)
			splitCurve(t, x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2, next, true);
	}

	private void splitCurve(double t, double x1, double y1, double x2, double y2, double x3, double y3, double x4,
			double y4, CubicCurve2D curve, boolean flip) {
		if (flip) {
			t = 1 - t;
			double xt = x1;
			double yt = y1;
			x1 = x4;
			y1 = y4;
			x4 = xt;
			y4 = yt;
			xt = x2;
			yt = y2;
			x2 = x3;
			y2 = y3;
			x3 = xt;
			y3 = yt;
		}
		double x12 = (x2 - x1) * t + x1;
		double y12 = (y2 - y1) * t + y1;
		double x23 = (x3 - x2) * t + x2;
		double y23 = (y3 - y2) * t + y2;
		double x34 = (x4 - x3) * t + x3;
		double y34 = (y4 - y3) * t + y3;

		double x123 = (x23 - x12) * t + x12;
		double y123 = (y23 - y12) * t + y12;
		double x234 = (x34 - x23) * t + x23;
		double y234 = (y34 - y23) * t + y23;

		double x1234 = (x234 - x123) * t + x123;
		double y1234 = (y234 - y123) * t + y123;

		// Maintain original orientation
		if (flip)
			curve.setCurve(x1234, y1234, x123, y123, x12, y12, x1, y1);
		else
			curve.setCurve(x1, y1, x12, y12, x123, y123, x1234, y1234);
	}

	// Do through rough approximation
	@Override
	public double[] getTangentLines(Curve other) {
		ArrayList<Point2D> foundPoints = new ArrayList<Point2D>();
		ArrayList<Point2D> otherPoints = new ArrayList<Point2D>();

		// Brute force each point
		for (int i = 10; i < approxPts.size(); i++) {
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
	}

	@Override
	public double[] getTangentTimes(double x, double y) {
		ArrayList<java.lang.Double> foundPoints = new ArrayList<java.lang.Double>();

		// Use flattening approximation
		for (int i = 0; i < approxPts.size(); i++) {
			if (testTangent(i, x, y)) {
				foundPoints.add(approxTimes.get(i));
			}
		}

		// Convert to array of times
		double[] times = new double[foundPoints.size()];
		for (int i = 0; i < foundPoints.size(); i++) {
			times[i] = foundPoints.get(i);
		}
		return times;
	}

	// Approximate times of tangent
	public double[][] getTangentTimes(Curve other) {
		ArrayList<double[]> foundPairs = new ArrayList<double[]>();

		// Brute force each point
		for (int i = 0; i < approxPts.size(); i++) {
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
		if (t <= (approxTimes.get(iMin) + approxTimes.get(iMax)) / 2)
			return iMin;
		else
			return iMax;

	}

	public Point2D eval(double t) {
		return eval(t, x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
	}

	public static Point2D eval(double t, CubicCurve2D curve) {
		return eval(t, curve.getX1(), curve.getY1(), curve.getCtrlX1(), curve.getCtrlY1(), curve.getCtrlX2(),
				curve.getCtrlY2(), curve.getX2(), curve.getY2());
	}

	public static Point2D eval(double t, double x1, double y1, double ctrlx1, double ctrly1, double ctrlx2,
			double ctrly2, double x2, double y2) {
		double mt = 1 - t;
		return new Point2D.Double(
				mt * mt * mt * x1 + 3.0 * t * mt * mt * ctrlx1 + 3.0 * t * t * mt * ctrlx2 + t * t * t * x2,
				mt * mt * mt * y1 + 3.0 * t * mt * mt * ctrly1 + 3.0 * t * t * mt * ctrly2 + t * t * t * y2);
	}

	@Override
	public double getCX1() {
		return this.getCtrlX1();
	}

	@Override
	public double getCY1() {
		return this.getCtrlY1();
	}

	@Override
	public double getCX2() {
		return this.getCtrlX2();
	}

	@Override
	public double getCY2() {
		return this.getCtrlY2();
	}

	@Override
	public Area getProjection(double t, double r) {
		final int index = getApproxTimeIndex(t);
		final Point2D origin = approxPts.get(index);

		Path2D path = new Path2D.Double();
		path.moveTo(origin.getX(), origin.getY());

		if (t > 0) {
			warpCurvePre(path, index, r);
			addSemicircle(path, index, r, false);

		} else {
			addSemicircle(path, index, r, true);
		}
		if (t < 1) {
			warpCurvePost(path, index, r);
		}
		path.lineTo(origin.getX(), origin.getY());
		path.closePath();

		return new Area(path);
	}

	// Add the semicircle shape to the given path
	private void addSemicircle(Path2D path, int i, double r, boolean lineTo) {
		Point2D p = approxPts.get(i);

		double rot = getAngle(i);

		// Form an ellipse appoximately
		// From
		// http://digerati-illuminatus.blogspot.com/2008/05/approximating-semicircle-with-cubic.html
		double x1 = p.getX() + r * Math.cos(rot);
		double y1 = p.getY() + r * Math.sin(rot);
		double x2 = p.getX() - r * Math.cos(rot);
		double y2 = p.getY() - r * Math.sin(rot);

		rot -= Math.PI / 2;
		double xc1 = x1 + r * (Math.cos(rot) * 4 / 3 - Math.sin(rot) * 0.05);
		double yc1 = y1 + r * (Math.sin(rot) * 4 / 3 + Math.cos(rot) * 0.05);

		double xc2 = x2 + r * (Math.cos(rot) * 4 / 3 + Math.sin(rot) * 0.05);
		double yc2 = y2 + r * (Math.sin(rot) * 4 / 3 - Math.cos(rot) * 0.05);

		if (lineTo)
			path.lineTo(x1, y1);
		path.curveTo(xc1, yc1, xc2, yc2, x2, y2);
	}

	public static Cubic getSemicircle(double x, double y, double r, double rot) {
		double x1 = x + r * Math.cos(rot);
		double y1 = y + r * Math.sin(rot);
		double x2 = x - r * Math.cos(rot);
		double y2 = y - r * Math.sin(rot);

		rot -= Math.PI / 2;
		double xc1 = x1 + r * (Math.cos(rot) * 4 / 3 - Math.sin(rot) * 0.05);
		double yc1 = y1 + r * (Math.sin(rot) * 4 / 3 + Math.cos(rot) * 0.05);

		double xc2 = x2 + r * (Math.cos(rot) * 4 / 3 + Math.sin(rot) * 0.05);
		double yc2 = y2 + r * (Math.sin(rot) * 4 / 3 - Math.cos(rot) * 0.05);

		return new Cubic(x1, y1, xc1, yc1, xc2, yc2, x2, y2);
	}

	private void warpCurvePre(Path2D path, int baseIndex, double r) {
		double baseDist = approxDists.get(baseIndex);

		// Find how far along the curve it travels
		int finalIndex = baseIndex;
		for (int i = baseIndex; i >= 0; i--) {
			if (r < baseDist - approxDists.get(i))
				break;
			finalIndex = i;

		}

		// Form partial curve
		double[] coords = new double[8];
		// Lerp gradually from index to 0
		for (int lerp = 0; lerp < 4; lerp++) {
			int index = (baseIndex * lerp / 3) + (finalIndex * (3 - lerp) / 3);

			Point2D bloc = approxPts.get(index);
			double br = r - (baseDist - approxDists.get(index));
			double ba = getAngle(index);

			coords[2 * lerp] = bloc.getX() + br * Math.cos(ba);
			coords[2 * lerp + 1] = bloc.getY() + br * Math.sin(ba);
		}
		Cubic curve = getCurveFollowingPoints(coords);

		path.lineTo(curve.getX1(), curve.getY1());
		path.curveTo(curve.getCtrlX1(), curve.getCtrlY1(), curve.getCtrlX2(), curve.getCtrlY2(), curve.getX2(),
				curve.getY2());
	}

	private void warpCurvePost(Path2D path, int baseIndex, double r) {
		double baseDist = approxDists.get(baseIndex);

		// Find how far along the curve it travels
		int finalIndex = baseIndex;
		for (int i = baseIndex; i < approxDists.size(); i++) {
			if (r < approxDists.get(i) - baseDist)
				break;
			finalIndex = i;

		}

		// Form partial curve
		double[] coords = new double[8];
		// Lerp gradually from index to 0
		for (int lerp = 0; lerp < 4; lerp++) {
			int index = (baseIndex * (3 - lerp) / 3) + (finalIndex * lerp / 3);
			Point2D bloc = approxPts.get(index);
			double br = r - (approxDists.get(index) - baseDist);
			double ba = getAngle(index);

			coords[2 * lerp] = bloc.getX() - br * Math.cos(ba);
			coords[2 * lerp + 1] = bloc.getY() - br * Math.sin(ba);
		}
		Cubic curve = getCurveFollowingPoints(coords);

		path.curveTo(curve.getCtrlX1(), curve.getCtrlY1(), curve.getCtrlX2(), curve.getCtrlY2(), curve.getX2(),
				curve.getY2());
	}

	public static final double EPSILON = 0.0001;

	@Override
	public Point2D getRaycastPoint(double t) {
		Point2D ret = eval(t);
		if (t <= 0.5) {
			// Move towards ctrl1 and away from p2
			double ac = Math.atan2(ctrly1 - ret.getY(), ctrlx1 - ret.getX());
			ret.setLocation(ret.getX() + EPSILON * (Math.cos(ac)), ret.getY() + EPSILON * (Math.sin(ac)));

		}
		if (t >= 0.5) {
			// Move towards ctrl2 and away from p1
			double ac = Math.atan2(ctrly2 - ret.getY(), ctrlx2 - ret.getX());
			ret.setLocation(ret.getX() + EPSILON * (Math.cos(ac)), ret.getY() + EPSILON * (Math.sin(ac)));
		}
		return ret;
	}

	private double getAngle(int i) {
		if (i == 0)
			return Math.atan2(y1 - ctrly1, x1 - ctrlx1);
		if (i == approxPts.size() - 1)
			return Math.atan2(ctrly2 - y2, ctrlx2 - x2);
		return Math.atan2(approxPts.get(i - 1).getY() - approxPts.get(i).getY(),
				approxPts.get(i - 1).getX() - approxPts.get(i).getX());
	}

	public static Cubic getCurveFollowingPoints(double[] coords) {
		return getCurveFollowingPoints(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], coords[6],
				coords[7]);
	}

	// https://math.stackexchange.com/questions/301736/how-do-i-find-a-bezier-curve-that-goes-through-a-series-of-points
	public static Cubic getCurveFollowingPoints(double x1, double y1, double x2, double y2, double x3, double y3,
			double x4, double y4) {
		double rx2 = (-5 * x1 + 18 * x2 - 9 * x3 + 2 * x4) / 6;
		double ry2 = (-5 * y1 + 18 * y2 - 9 * y3 + 2 * y4) / 6;
		double rx3 = (2 * x1 - 9 * x2 + 18 * x3 - 5 * x4) / 6;
		double ry3 = (2 * y1 - 9 * y2 + 18 * y3 - 5 * y4) / 6;
		return new Cubic(x1, y1, rx2, ry2, rx3, ry3, x4, y4);
	}

	public boolean equals(Cubic q) {
		return x1 == q.x1 && y1 == q.y1 && ctrlx1 == q.ctrlx1 && ctrly1 == q.ctrly1 && ctrlx2 == q.ctrlx2
				&& ctrly2 == q.ctrly2 && x2 == q.x2 && y2 == q.y2;
	}

	@Override
	public String toString() {
		return "{Cubic (" + x1 + ", " + y1 + ") -> (" + x2 + ", " + y2 + ") controls (" + ctrlx1 + ", " + ctrly1
				+ "), (" + ctrlx2 + ", " + ctrly2 + ")}";
	}

}
