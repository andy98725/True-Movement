package helpers.geomNodes;

import java.awt.*;
import java.awt.geom.Area;
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

	public void draw(Graphics2D g) {
		g.setColor(Color.GREEN);
		g.fill(curve);
		g.setColor(Color.RED);
		g.draw(curve);

	}

	public void connectByTangents(CurveNode other, Area base, ArrayList<Shape> geom) {
		// TODO
	}

}
