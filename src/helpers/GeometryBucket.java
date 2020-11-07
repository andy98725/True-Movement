package helpers;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.PriorityQueue;

import helpers.geom.*;
import helpers.geomNodes.CurveNode;
import helpers.geomNodes.RecursionCase;

public class GeometryBucket {

	private final Area base;

	private final ArrayList<CurveNode> nodes = new ArrayList<CurveNode>();

	private final double originX, originY, rad;

	private final Area result;

	public GeometryBucket() {
		this(new Area(), 1, 0, 0, 0);
	}

	public GeometryBucket(Area a, double extend, double x, double y, double maxRad) {
		this.originX = x;
		this.originY = y;
		this.rad = maxRad;
		// Since the area is necessarily extended, we can make the nice assumption
		// that the only POI are curved nodes.
		// This allows a complete exclusion of a pointNode class.
		base = new Area(a);
		base.add(new Area(Util.extendArea(a, extend)));

		generateGeometry();

		genAndConnectNodes();

		addStartingNode();
		propogate();

		result = getRaycast();

	}

	private final ArrayList<Shape> path = new ArrayList<Shape>();
	private final ArrayList<Shape> rawGeom = new ArrayList<Shape>();

	private void generateGeometry() {
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

	}

	private boolean saveStart;
	private CurveNode prevLink, startLink;

	private void genAndConnectNodes() {
		final Ellipse2D bounds = new Ellipse2D.Double(originX - rad, originY - rad, 2 * rad, 2 * rad);

		prevLink = null;
		startLink = null;
		saveStart = true;

		// Generate nodes from path
		for (int i = 0; i < path.size(); i++) {
			// Close segment
			if (path.get(i) == null) {
				// Connect previous to start
				if (prevLink != null && startLink != null) {
					prevLink.setNextNeighbor(startLink);
				}
				prevLink = null;
				startLink = null;
				saveStart = true;

			} else if (path.get(i) instanceof Line2D) {
				// Do nothing! Only curves are POI
//				Line2D l = (Line2D) path.get(i);
			} else if (path.get(i) instanceof Curve) {
				Curve s = (Curve) path.get(i);

				CurveNode c = CurveNode.genCurveNode(s, bounds);
				if (c != null) {
					nodes.add(c);
					if (prevLink != null)
						prevLink.setNextNeighbor(c);
				}
				if (saveStart) {
					saveStart = false;
					startLink = c;
				}
				prevLink = c;

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
			n.addStartingCases(recurseCases, originX, originY, rad, rawGeom);
		}

	}

	private void propogate() {
		while (!recurseCases.isEmpty()) {
			recurseCases.remove().propogate(recurseCases);
		}
	}

	private Area getRaycast() {
		Area res = new Area();
//		Area res = new Area(new Ellipse2D.Double(originX - rad, originY - rad, 2 * rad, 2 * rad));
//		Raycast.raycastIndividuals(res, originX, originY, rawGeom, null);
		for (CurveNode n : nodes) {
			res.add(n.getDistShape(rawGeom));
		}

		return res;
	}

	public Area getResult() {
		return result;
	}

}
