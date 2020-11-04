package helpers.geomNodes;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

import helpers.Util;

public class PointNode extends BucketNode {
	// Point type node
	final double x, y;
	// Theta ranges
	final double th1, th2;
	final Point2D pOff;
	final ArrayList<BucketNode> locConnects;

	// Point node constructor
	private PointNode(double px, double py, double x, double y, double nx, double ny) {
		super();
		this.x = x;
		this.y = y;
		this.th1 = Math.atan2(y - py, x - px);
		this.th2 = Math.atan2(y - ny, x - nx);
		locConnects = new ArrayList<BucketNode>();
		pOff = Util.genOffsetPoint(x, y, th1, th2);
	}

	private boolean pointInRange(double x, double y) {
		double angle = Math.atan2(this.y - y, this.x - x);
		if (th1 < th2) {
			return angle >= th1 && angle <= th2;
		} else {
			return angle >= th1 || angle <= th2;
		}
	}

	// Generate node if range is applicable
	public static PointNode genNode(double px, double py, double x, double y, double nx, double ny) {
		return Util.orientation(px, py, x, y, nx, ny) >= 0 ? new PointNode(px, py, x, y, nx, ny) : null;
	}

	public void neighborConnect(BucketNode other) {
		if (other instanceof PointNode) {
			locConnects.add(other);
			((PointNode) other).locConnects.add(this);

		} else if (other instanceof CurveNode) {
			((CurveNode) other).neighborConnect(this);
		}
	}

	@Override
	protected void tryConnect(PointNode other, Area base, ArrayList<Shape> geom) {
//		Line2D l = Util.genShortenedLine(x, y, other.x, other.y);
		// TODO
	}

	@Override
	protected void tryConnect(CurveNode other, Area base, ArrayList<Shape> geom) {

		// TODO Auto-generated method stub

	}

	@Override
	public void draw(Graphics2D g) {
		System.out.println("Here");
		double r = 4;
		g.setColor(Color.YELLOW);
		g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
		g.setColor(Color.RED);
		g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
	}
}
