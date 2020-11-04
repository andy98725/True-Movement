package helpers.geomNodes;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.HashMap;

import helpers.CustomCurve;

public class CurveNode extends BucketNode {

	// Curve type node
	final CustomCurve curve;
	final HashMap<BucketNode, Double> curveConnects;

	// Curve node constructor
	private CurveNode(CustomCurve c) {
		curve = c;
		curveConnects = new HashMap<BucketNode, Double>();

	}

	public static CurveNode genCurveNode(CustomCurve c) {
		return c.isConvex() ? new CurveNode(c) : null;
	}

	// Force a connection
	public void neighborConnect(BucketNode other) {
		if(other instanceof CurveNode) {
			throw new RuntimeException("Curve cannot neighbor curve");
		}
		else if(other instanceof PointNode) {
			PointNode p = (PointNode) other;
			p.locConnects.add(this);
			curveConnects.put(other, curve.getClosestTime(p.x, p.y));
		}

	}

	@Override
	public void draw(Graphics2D g) {
		g.setColor(Color.GREEN);
		g.fill(curve);
		g.setColor(Color.RED);
		g.draw(curve);

	}

	@Override
	protected void tryConnect(CurveNode other, Area base, ArrayList<Shape> geom) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void tryConnect(PointNode other, Area base, ArrayList<Shape> geom) {
		// TODO Auto-generated method stub

	}
}
