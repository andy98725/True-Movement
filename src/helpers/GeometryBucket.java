package helpers;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.ArrayList;

import helpers.geomNodes.*;

public class GeometryBucket {

	private final Area base;
	private final ArrayList<Shape> path = new ArrayList<Shape>();
	private final ArrayList<Shape> rawGeom = new ArrayList<Shape>();

	private final ArrayList<BucketNode> nodes;

	public GeometryBucket() {
		this(new Area());
	}
	public GeometryBucket(Area a) {
		base = a;
		double[] pcoords = new double[2];
		double[] coords = new double[6];
		double[] mcoords = new double[2];
		for (PathIterator iter = a.getPathIterator(null); !iter.isDone(); iter.next()) {
			switch (iter.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
				mcoords[0] = coords[0];
				mcoords[1] = coords[1];
				pcoords[0] = coords[0];
				pcoords[1] = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				coords[0] = mcoords[0];
				coords[1] = mcoords[1];
				rawGeom.add(new Line2D.Double(pcoords[0], pcoords[1], coords[0], coords[1]));
				path.add(null); // Null signifies close of shape
				break;
			case PathIterator.SEG_LINETO:
				Line2D l = new Line2D.Double(pcoords[0], pcoords[1], coords[0], coords[1]);
				rawGeom.add(l);
				path.add(l);
				pcoords[0] = coords[0];
				pcoords[1] = coords[1];
				break;
			case PathIterator.SEG_QUADTO:
				Quad q = new Quad(pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3]);
				rawGeom.add(q);
				path.add(q);
				pcoords[0] = coords[2];
				pcoords[1] = coords[3];
				break;
			case PathIterator.SEG_CUBICTO:
				// Split cubics to ensure consistent concavity and convexity
				Cubic base = new Cubic(pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3], coords[4],
						coords[5]);
				if (base.isRegular()) {
					rawGeom.add(base);
					path.add(base);
				} else {
					// Split into regulars at 0.5
					Cubic c1 = new Cubic(base.subCurve(0, 0.5));
					rawGeom.add(c1);
					path.add(c1);
					Cubic c2 = new Cubic(base.subCurve(0.5, 1));
					rawGeom.add(c2);
					path.add(c2);
				}
				pcoords[0] = coords[4];
				pcoords[1] = coords[5];
				break;
			}
		}

		nodes = new ArrayList<BucketNode>();
		genAndConnectNodes();

	}

	private double prevX, prevY, curX, curY;
	private BucketNode prevLink;
	boolean saveStart;
	private double startX, startY, startNX, startNY;

	private boolean saveStartLink;
	private BucketNode startLink;

	private ArrayList<BucketNode> genAndConnectNodes() {
		saveStart = true;
		saveStartLink = false;
		startLink = null;
		prevLink = null;

		// Generate nodes from path
		for (int i = 0; i < path.size(); i++) {
			// Close segment
			if (path.get(i) == null) {
				// Do ending link as a line to start
				addLineNode(curX, curY, startX, startY);

				// Close out with initial node
				prevX = curX;
				prevY = curY;
				PointNode start = addLineNode(startX, startY, startNX, startNY);
				if (start != null && startLink != null) {
					start.neighborConnect(startLink);
				}

				// Save start next time
				saveStart = true;

			} else if (path.get(i) instanceof Line2D) {
				Line2D s = (Line2D) path.get(i);

				if (saveStart) {
					saveStart(s.getX1(), s.getY1(), s.getX2(), s.getY2());
				} else {
					addLineNode(s.getX1(), s.getY1(), s.getX2(), s.getY2());
				}

				updatePositioning(s.getX1(), s.getY1(), s.getX2(), s.getY2());
			} else if (path.get(i) instanceof CustomCurve) {
				CustomCurve s = (CustomCurve) path.get(i);

				if (saveStart) {
					saveStart(s.getX1(), s.getY1(), s.getCX1(), s.getCY1());
				} else {
					addLineNode(s.getX1(), s.getY1(), s.getCX1(), s.getCY1());
				}

				addCurveNode(s);

				updatePositioning(s.getCX2(), s.getCY2(), s.getX2(), s.getY2());
			}
		}

		// Nodes are made now
		// Connect in O(n^3) time
		for (int i = 1; i < nodes.size(); i++) {
			for (int j = 0; j < i; j++) {
				nodes.get(i).tryConnect(nodes.get(j), base, rawGeom);
			}
		}
		return nodes;
	}

	private PointNode addLineNode(double x, double y, double nx, double ny) {
		PointNode p = PointNode.genNode(prevX, prevY, x, y, nx, ny);
		if (p != null) {
			nodes.add(p);
			if (prevLink != null)
				p.neighborConnect(prevLink);
		}

		prevLink = p;
		if (saveStartLink) {
			saveStartLink = false;
			startLink = p;
		}

		return p;
	}

	private CurveNode addCurveNode(CustomCurve s) {
		CurveNode c = CurveNode.genCurveNode(s);
		if (c != null) {
			nodes.add(c);
			if (prevLink != null)
				c.neighborConnect(prevLink);
		}

		prevLink = c;
		if (saveStartLink) {
			saveStartLink = false;
			startLink = c;
		}
		return c;
	}

	private void saveStart(double x, double y, double nx, double ny) {
		saveStart = false;
		saveStartLink = true;
		prevLink = null;
		startLink = null;
		startX = x;
		startY = y;
		startNX = nx;
		startNY = ny;
	}

	private void updatePositioning(double px, double py, double x, double y) {
		prevX = x;
		prevY = y;
		curX = x;
		curY = y;
	}

	public void drawNodes(Graphics2D g) {
		for (BucketNode n : nodes) {
			n.draw(g);
		}

	}

}
