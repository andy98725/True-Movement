package helpers.geomNodes;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import helpers.CustomCurve;

public class CurveNode {

	// Curve type node
	private final CustomCurve curve;
	private final HashMap<CurveConnection, CurveNode> connections;
	// Prev neighbor connects this's t=0 with its t=1. next neighbor reverses
	private CurveNode prevNeighbor = null, nextNeighbor = null;

	// Curve node constructor
	private CurveNode(CustomCurve c) {
		curve = c;
		connections = new HashMap<CurveConnection, CurveNode>();

	}

	public static CurveNode genCurveNode(CustomCurve c, Ellipse2D baseRange) {
		// Only convex nodes are worth considering
		if (!c.isConvex())
			return null;

		// Only nodes in range are worth considering
		if (!baseRange.intersects(c.getBounds2D()))
			return null;

		return new CurveNode(c);
	}

	public void setNextNeighbor(CurveNode other) {
		nextNeighbor = other;
		other.prevNeighbor = this;
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
				} else if (s instanceof CustomCurve) {
					CustomCurve c = (CustomCurve) s;
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

	public void draw(Graphics2D g) {
		g.setColor(Color.GREEN);
		g.fill(curve);
		g.setColor(Color.BLUE);
		g.draw(curve);

		g.setColor(Color.RED);
		for (Map.Entry<CurveConnection, CurveNode> e : connections.entrySet()) {
			CurveConnection con = e.getKey();
			CustomCurve other = e.getValue().curve;
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
