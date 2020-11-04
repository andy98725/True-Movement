package helpers.geomNodes;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.ArrayList;

public abstract class BucketNode implements Comparable<BucketNode> {

	// (Largest) radius raycasting
	double r = 2;

	protected BucketNode() {
	}

	// Link to individual methods
	public void tryConnect(BucketNode other, Area base, ArrayList<Shape> geom) {
		if (other instanceof CurveNode) {
			tryConnect((CurveNode) other, base, geom);
		} else {
			tryConnect((PointNode) other, base, geom);

		}
	}

	protected abstract void tryConnect(PointNode other, Area base, ArrayList<Shape> geom);

	protected abstract void tryConnect(CurveNode other, Area base, ArrayList<Shape> geom);

	public abstract void draw(Graphics2D g);

	@Override
	public int compareTo(BucketNode o) {
		return (int) Math.signum(r - o.r);
	}

}
