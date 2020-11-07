package helpers.geomNodes;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import algorithms.Raycast;
import helpers.geom.Curve;

public class CurveNode {

	// Curve type node
	private final Curve curve;
	private final HashMap<CurveConnection, CurveNode> connections = new HashMap<CurveConnection, CurveNode>();
	private final HashMap<Double, Double> distances = new HashMap<Double, Double>();
	// Prev neighbor connects this's t=0 with its t=1. next neighbor reverses
	private CurveNode prevNeighbor = null, nextNeighbor = null;

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

			boolean isValid = true;
			// Invalid if line intersects any non-local geometry
			for (Shape s : geom) {
				if (s.equals(curve) || s.equals(other.curve))
					continue;

				if (s instanceof Line2D) {
					Line2D l = (Line2D) s;
					if (l.intersectsLine(p1.getX(), p1.getY(), p2.getX(), p2.getY())) {
						isValid = false;
						break;
					}
				} else if (s instanceof Curve) {
					Curve c = (Curve) s;
					if (c.intersectsLine(p1.getX(), p1.getY(), p2.getX(), p2.getY())) {
						isValid = false;
						break;
					}
				}
			}

			// Also invalid if goes through shape
			if (isValid && base.contains((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2)) {
				isValid = false;
			}

			if (isValid) {
				// Map in connections
				final double dist = Math.hypot(p1.getX() - p2.getX(), p1.getY() - p2.getY());
				connections.put(new CurveConnection(t[0], t[1], dist), other);
				other.connections.put(new CurveConnection(t[1], t[0], dist), this);
			}

		}

	}

	public void addStartingCases(PriorityQueue<RecursionCase> recurseCases, double x, double y, double rad,
			ArrayList<Shape> geom) {
		double[] times = curve.getTangentTimes(x, y);
		for (double t : times) {
			Point2D p = curve.eval(t);

			boolean isValid = true;
			for (Shape s : geom) {
				if (s.equals(curve))
					continue;

				if (s instanceof Line2D) {
					Line2D l = (Line2D) s;
					if (l.intersectsLine(x, y, p.getX(), p.getY()) && !p.equals(l.getP1()) && !p.equals(l.getP2())) {
						isValid = false;
						break;
					}
				} else if (s instanceof Curve) {
					Curve c = (Curve) s;
					if (c.intersectsLine(x, y, p.getX(), p.getY()) && p.equals(c.getP1()) && !p.equals(c.getP2())) {
						isValid = false;
						break;
					}
				}
			}

			if (isValid) {
				// Add new case
				double dist = rad - Math.hypot(x - p.getX(), y - p.getY());
				recurseCases.add(new RecursionCase(dist, this, t));
				addDistance(t, dist, recurseCases);
			}
		}
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

	public Area getDistShape(ArrayList<Shape> geom) {
		Area ret = new Area();

		for (Map.Entry<Double, Double> e : distances.entrySet()) {
			if (e.getValue() > 0)
				ret.add(subShape(e.getKey(), e.getValue(), geom));
		}

		return ret;
	}

	private Area subShape(double t, double r, ArrayList<Shape> geom) {
		Area curveShape = curve.getProjection(t, r);

		// Get point slightly above & in
		Point2D p1 = curve.getRaycastPoint1();
		Point2D p2 = curve.getRaycastPoint2();

		Raycast.raycastIndividuals(curveShape, p1.getX(), p1.getY(), geom, curve);
		Raycast.raycastIndividuals(curveShape, p2.getX(), p2.getY(), geom, curve);

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
