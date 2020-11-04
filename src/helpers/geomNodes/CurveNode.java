package helpers.geomNodes;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;

import helpers.CustomCurve;

public class CurveNode {

	// Curve type node
	private final CustomCurve curve;
	private final HashMap<CurveNode, Double> curveConnects;
	// Prev neighbor connects this's t=0 with its t=1. next neighbor reverses
	private CurveNode prevNeighbor = null, nextNeighbor = null;

	// Curve node constructor
	private CurveNode(CustomCurve c) {
		curve = c;
		curveConnects = new HashMap<CurveNode, Double>();

	}

	public static CurveNode genCurveNode(CustomCurve c) {
		return c.isConvex() ? new CurveNode(c) : null;
	}

	public void setNextNeighbor(CurveNode other) {
		nextNeighbor = other;
		other.prevNeighbor = this;
	}
	public void connectByTangents(CurveNode other, Area base, ArrayList<Shape> geom) {
		double[][] times = curve.getTangentTimes(other.curve);
		for(double[] t : times) {
			// Determine if valid tangent pair
			boolean isValid = true;
			// Invalid if line intersects any non-local geometry
			Point2D p1 = curve.eval(t[0]), p2 = other.curve.eval(1);
		}
		
	}

	public void draw(Graphics2D g) {
		g.setColor(Color.GREEN);
		g.fill(curve);
		g.setColor(Color.RED);
		g.draw(curve);

	}


}
