package helpers.geomNodes;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import algorithms.Raycast;
import helpers.Util;
import helpers.geom.Curve;

public class CurveNode {

	// Curve type node
	private final Curve curve;
	private final HashMap<CurveConnection, CurveNode> connections = new HashMap<CurveConnection, CurveNode>();
	private final HashMap<Double, Double> distances = new HashMap<Double, Double>();
	// Prev neighbor connects this's t=0 with its t=1. next neighbor reverses
	private CurveNode prevNeighbor = null, nextNeighbor = null;

	private boolean shouldRaycast = true;

	// Curve node constructor
	private CurveNode(Curve c) {
		curve = c;

	}

	public static CurveNode genCurveNode(Curve c, Ellipse2D baseRange) {
		// Only convex nodes are worth considering
		if (!c.isConvex())
			return null;

		// Only nodes in range are worth considering
		if (!baseRange.intersects(c.getBounds2D()))
			return null;

		return new CurveNode(c);
	}

	public boolean matches(Point2D p1) {
		return curve.getP1().equals(p1);
	}

	public Point2D getP2() {
		return curve.getP2();
	}

	// Connect this t=1 to other t=0
	public void setNextNeighbor(CurveNode other) {
		final double dist = Math.hypot(curve.getX2() - other.curve.getX1(), curve.getY2() - other.curve.getY1());
		nextNeighbor = other;
		connections.put(new CurveConnection(1, 0, dist), other);
		other.prevNeighbor = this;
		other.connections.put(new CurveConnection(0, 1, dist), this);
	}

	public void connectByTangents(CurveNode other, Area base, ArrayList<Shape> geom) {
		if (other == nextNeighbor || other == prevNeighbor)
			return;

		double[][] times = curve.getTangentTimes(other.curve);
		for (double[] t : times) {
			Point2D p1 = curve.eval(t[0]), p2 = other.curve.eval(t[1]);

			if (isValid(p1, p2, base, geom)) {
				// Map in connections
				final double dist = Math.hypot(p1.getX() - p2.getX(), p1.getY() - p2.getY());
				connections.put(new CurveConnection(t[0], t[1], dist), other);
				other.connections.put(new CurveConnection(t[1], t[0], dist), this);
			}

		}

	}

	public void addStartingCases(PriorityQueue<RecursionCase> recurseCases, double x, double y, double rad, Area base,
			ArrayList<Shape> geom) {
		double[] times = curve.getTangentTimes(x, y);
		for (double t : times) {
			Point2D p = curve.eval(t);

			if (isValid(new Point2D.Double(x, y), p, base, geom)) {
				// Add new case
				double dist = rad - Math.hypot(x - p.getX(), y - p.getY());
				recurseCases.add(new RecursionCase(dist, this, t));
				addDistance(t, dist, recurseCases);
			}
		}
	}

	private boolean isValid(Point2D p1, Point2D p2, Area base, ArrayList<Shape> geom) {
		for (Shape s : geom) {
			if (s.equals(curve))
				continue;

			if (s instanceof Line2D) {
				Line2D l = (Line2D) s;
				if (Util.linesIntersect(l.getX1(), l.getY1(), l.getX2(), l.getY2(), p1.getX(), p1.getY(), p2.getX(),
						p2.getY()) && !l.getP1().equals(p1) && !l.getP2().equals(p1) && !l.getP1().equals(p2)
						&& !l.getP2().equals(p2)) {
					return false;
				}
			} else if (s instanceof Curve) {
				Curve c = (Curve) s;
				if (c.intersectsLine(p1.getX(), p1.getY(), p2.getX(), p2.getY()) && !c.getP1().equals(p1)
						&& !c.getP2().equals(p1) && !c.getP1().equals(p2) && !c.getP2().equals(p2)) {
					return false;
				}
			}
		}

		// Also invalid if goes through shape
		if (base.contains((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2)) {
			return false;
		}
		return true;
	}

	public void addDistance(double t, double dist, PriorityQueue<RecursionCase> recurse) {
		if (distances.isEmpty()) {
			insertDistance(t, dist, recurse);
			return;
		}

		// Is a direct comparison possible?
		if (distances.containsKey(t)) {
			if (dist > distances.get(t)) {
				insertDistance(t, dist, recurse);
			}
			return;
		}

		double existingDist = getDistance(t);
		// Insert if possible
		if (dist > existingDist) {
			insertDistance(t, dist, recurse);
		}

	}

	private void insertDistance(double t, double dist, PriorityQueue<RecursionCase> recurse) {

		// Check outgoing values
		for (CurveConnection o : connections.keySet()) {
			double t2 = o.t0;
			if (t2 == t)
				continue;

			double modDist = dist - curve.distanceAlongCurve(t, t2);
			if (modDist > getDistance(t2)) {
				distances.remove(t2);
				recurse.add(new RecursionCase(modDist - o.dist, connections.get(o), o.t1));
			}
		}

		// Check existing values
		ArrayList<Double> old = new ArrayList<Double>(distances.keySet());
		for (double t2 : old) {
			double modDist = dist - curve.distanceAlongCurve(t, t2);
			if (modDist >= getDistance(t2)) {
				distances.remove(t2);
			}
		}

		// Insert at direct value
		distances.put(t, dist);
	}

	private double getDistance(double t) {
		if (distances.containsKey(t))
			return distances.get(t);
		// Find nearest keys
		double tLo = -1, tHi = 2;
		for (double time : distances.keySet()) {
			if (time <= t && time >= tLo)
				tLo = time;
			if (time >= t && time <= tHi)
				tHi = time;
		}

		// Determine distance to insert
		double maxDist = 0;
		if (tLo != -1) {
			maxDist = Math.max(maxDist, distances.get(tLo) - curve.distanceAlongCurve(tLo, t));
		}
		if (tHi != 2) {
			maxDist = Math.max(maxDist, distances.get(tHi) - curve.distanceAlongCurve(t, tHi));
		}
		return maxDist;
	}

	public Area getDistShape(Area base) {
		Area ret = new Area();

		if (shouldRaycast) {
			for (Map.Entry<Double, Double> e : distances.entrySet()) {
				if (e.getValue() > 0)
					ret.add(subShape(e.getKey(), e.getValue(), base));
			}

		}
		return ret;
	}

	private Area subShape(double t, double r, Area base) {
		Area curveShape = curve.getProjection(t, r);
		Area c2 = new Area(curveShape);

		// Raycast from slightly outside of curve
		Point2D p = curve.getRaycastPoint(0);
		Point2D p2 = curve.getRaycastPoint(1);

		curveShape = new Raycast(curveShape, p.getX(), p.getY(), base, curve).get();
		c2 = new Raycast(c2, p2.getX(), p2.getY(), base, curve).get();

		curveShape.add(c2);
		curveShape.subtract(new Area(curve));

		return curveShape;

	}

	public void draw(Graphics2D g) {
		g.setColor(Color.GREEN);
		g.fill(curve);
		g.setColor(Color.BLUE);
		g.draw(curve);

		g.setColor(Color.RED);
		for (Map.Entry<CurveConnection, CurveNode> e : connections.entrySet()) {
			CurveConnection con = e.getKey();
			Curve other = e.getValue().curve;
			Point2D p1 = curve.eval(con.t0), p2 = other.eval(con.t1);

			g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
		}

	}

	private class CurveConnection {

		protected final double t0, t1;
		protected final double dist;

		private CurveConnection(double t0, double t1, double dist) {
			this.t0 = t0;
			this.t1 = t1;
			this.dist = dist;
		}

	}
}
