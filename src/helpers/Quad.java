package helpers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class Quad extends QuadCurve2D.Double {

	private final boolean isConvex, isConcave;

	// For segment on segment estimation
	private final ArrayList<Point2D> approxPts;
	private final ArrayList<java.lang.Double> approxDist;

	public Quad(double x0, double y0, double x1, double y1, double x2, double y2) {
		super(x0, y0, x1, y1, x2, y2);
		isConvex = Util.orientation(x0, y0, x1, y1, x2, y2) > 0;
		isConcave = Util.orientation(x0, y0, x1, y1, x2, y2) < 0;

		// NOTE: this approximation is lazy, using a builtin method (which would get the
		// distances wrong, if that's a problem.)
		approxPts = new ArrayList<Point2D>();
		approxDist = new ArrayList<java.lang.Double>();
		double distCovered = 0;
		Path2D path = new Path2D.Double();
		path.moveTo(x0, y0);
		path.quadTo(x1, y1, x2, y2);
		path.closePath();
		double[] coords = new double[6];
		for (PathIterator p = path.getPathIterator(null, Cubic.FLATNESS); !p.isDone(); p.next()) {
			switch (p.currentSegment(coords)) {
			case PathIterator.SEG_LINETO:
				// Increment distance
				distCovered += Math.hypot(coords[0] - coords[2], coords[1] - coords[3]);
			case PathIterator.SEG_MOVETO:
				approxPts.add(new Point2D.Double(coords[0], coords[1]));
				approxDist.add(distCovered);
				// Save previous coords
				coords[2] = coords[0];
				coords[3] = coords[1];
				break;

			}
		}
	}

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

		double[] p0 = (t0 >= 0 && t0 <= 1) ? eval(t0) : new double[] {};
		double[] p1 = (t1 >= 0 && t1 <= 1) ? eval(t1) : new double[] {};

		double[] ret = new double[p0.length + p1.length];
		for (int i = 0; i < p0.length; i++) {
			ret[i] = p0[i];
		}
		for (int i = 0; i < p1.length; i++) {
			ret[p0.length + i] = p1[i];
		}

		return ret;
	}

	public void draw(Graphics2D g) {
		if (isConvex) {
			g.setColor(Cubic.CONVEX);
			g.fill(this);
		}
		if (isConcave) {
			g.setColor(Cubic.CONCAVE);
			g.fill(this);
		}
		g.setColor(Color.BLACK);
		g.draw(this);
	}

	private double[] eval(double t) {
		return new double[] { (1 - t) * (1 - t) * x1 + 2 * t * (1 - t) * ctrlx + t * t * x2,
				(1 - t) * (1 - t) * y1 + 2 * t * (1 - t) * ctrly + t * t * y2 };
	}

	private boolean testTangent(int i, double x, double y) {
		Point2D p = approxPts.get(i), p1 = approxPts.get(i - 1), p2 = approxPts.get(i + 1);
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

	public double[] getTangentLines(Cubic other) {
		// Use faster method
		return other.getTangentLines(this);
	}

	// Do through rough approximation
	public double[] getTangentLines(Quad other) {
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
	}
}
