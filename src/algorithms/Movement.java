package algorithms;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.PriorityQueue;

import helpers.Util;
import helpers.geom.*;
import helpers.geomNodes.CurveNode;
import helpers.geomNodes.RecursionCase;

public class Movement {

	private final Area base;

	private final ArrayList<CurveNode> nodes = new ArrayList<CurveNode>();

	private final double originX, originY, rad;
	private final double thickness;

	private final Area result;

	public Movement() {
		this(new Area(), 1, 0, 0, 0);
	}

	public Movement(Area a, double x, double y, double maxRad, double minWidth) {
		minWidth /= 2;
		this.originX = x;
		this.originY = y;
		this.rad = maxRad - minWidth;
		this.thickness = minWidth;
		// Since the area is necessarily extended, we can make the nice assumption
		// that the only POI are curved nodes.
		// This allows a complete exclusion of a pointNode class.

		// Before calling extend, limit to bounds
		Area block = new Area(a);
		double ir = maxRad + thickness * 2;
		block.intersect(new Area(new Ellipse2D.Double(x - ir, y - ir, 2 * ir, 2 * ir)));
		block.add(new Area(Util.extendArea(block, thickness)));
		block = Util.getSimple(block);
		this.base = block;

		if (block.contains(x, y)) {
			result = new Area();
			return;
		}

		generateGeometry();

		genAndConnectNodes();

		addStartingNode();
		propogate();

		result = getRaycast();

	}

	private final ArrayList<Shape> path = new ArrayList<Shape>();
	private final ArrayList<Shape> rawGeom = new ArrayList<Shape>();

	private void generateGeometry() {
		Rectangle2D bounds = new Rectangle2D.Double(originX - rad, originY - rad, 2 * rad, 2 * rad);

		double[] pcoords = new double[2];
		double[] coords = new double[6];
		double[] mcoords = new double[2];
		for (PathIterator iter = base.getPathIterator(null); !iter.isDone(); iter.next()) {
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
				if (bounds.intersectsLine(pcoords[0], pcoords[1], coords[0], coords[1])) {
					Line2D l = new Line2D.Double(pcoords[0], pcoords[1], coords[0], coords[1]);
					rawGeom.add(l);
					path.add(l);
				}
				path.add(null); // Null signifies close of shape
				break;
			case PathIterator.SEG_LINETO:
				if (bounds.intersectsLine(pcoords[0], pcoords[1], coords[0], coords[1])) {
					Line2D l = new Line2D.Double(pcoords[0], pcoords[1], coords[0], coords[1]);
					rawGeom.add(l);
					path.add(l);
				}
				pcoords[0] = coords[0];
				pcoords[1] = coords[1];
				break;
			case PathIterator.SEG_QUADTO:
				Quad q = new Quad(pcoords, coords);
				if (q.intersects(bounds)) {
					rawGeom.add(q);
					path.add(q);
				}
				pcoords[0] = coords[2];
				pcoords[1] = coords[3];
				break;
			case PathIterator.SEG_CUBICTO:
				// Split cubics to ensure consistent concavity and convexity
				Cubic base = new Cubic(pcoords, coords);
				if (base.isRegular()) {
					if (base.intersects(bounds)) {
						rawGeom.add(base);
						path.add(base);
					}
				} else {
					// Split into regulars at 0.5
					Cubic c1 = new Cubic(base.subCurve(0, 0.5));
					if (c1.intersects(bounds)) {
						rawGeom.add(c1);
						path.add(c1);
					}
					Cubic c2 = new Cubic(base.subCurve(0.5, 1));
					if (c2.intersects(bounds)) {
						rawGeom.add(c2);
						path.add(c2);
					}
				}
				pcoords[0] = coords[4];
				pcoords[1] = coords[5];
				break;
			}
		}

	}

	private void genAndConnectNodes() {
		final Ellipse2D bounds = new Ellipse2D.Double(originX - rad, originY - rad, 2 * rad, 2 * rad);

		CurveNode prevLink = null;
		Point2D prevP = null;
		CurveNode startLink = null;
		boolean saveStart = true;

		// Generate nodes from path
		for (int i = 0; i < path.size(); i++) {
			// Close segment
			if (path.get(i) == null) {
				// Connect previous to start
				if (prevLink != null && startLink != null && startLink.matches(prevP)) {
					prevLink.setNextNeighbor(startLink);
				}
				prevLink = null;
				prevP = null;
				startLink = null;
				saveStart = true;

			} else if (path.get(i) instanceof Line2D) {
				// Propogate previous point
				Line2D l = (Line2D) path.get(i);
				if (l.getP1().equals(prevP)) {
					prevP = l.getP2();
				}
			} else if (path.get(i) instanceof Curve) {
				Curve s = (Curve) path.get(i);

				CurveNode c = CurveNode.genCurveNode(s, bounds);
				if (c != null) {
					nodes.add(c);
					if (prevLink != null && c.matches(prevP))
						prevLink.setNextNeighbor(c);
				}
				if (saveStart) {
					saveStart = false;
					startLink = c;
				}
				prevLink = c;
				prevP = c != null ? c.getP2() : null;

			}
		}

		// Nodes are made now
		// Connect in O(n^3) time
		for (int i = 1; i < nodes.size(); i++) {
			for (int j = 0; j < i; j++) {
				nodes.get(i).connectByTangents(nodes.get(j), base, rawGeom);
			}
		}
	}

	public void drawNodes(Graphics2D g) {
		System.out.println("Geometry complexity " + Util.areaComplexity(base) + ", Node complexity " + nodes.size());
		for (CurveNode n : nodes) {
			n.draw(g);
		}

	}

	private final PriorityQueue<RecursionCase> recurseCases = new PriorityQueue<RecursionCase>();

	private void addStartingNode() {
		for (CurveNode n : nodes) {
			n.addStartingCases(recurseCases, originX, originY, rad, base, rawGeom);
		}

	}

	private void propogate() {
		while (!recurseCases.isEmpty()) {
			recurseCases.remove().propogate(recurseCases);
		}
	}

	private Area getRaycast() {
		Area res = new Raycast(originX, originY, base, rad).get();

		for (CurveNode n : nodes)
			res.add(n.getDistShape(base));

		res.subtract(base);

		return Util.getSimple(res);
	}

	public Area getResult() {
		return result;
	}

}
