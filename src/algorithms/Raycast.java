package algorithms;

import java.awt.Shape;
import java.awt.geom.*;
import java.util.Stack;

import helpers.Util;
import helpers.geom.*;

public class Raycast {

	private static final double RAYCAST_DIST = 1E12;

//	public static Area raycastIndividuals(Area base, double x, double y, ArrayList<Shape> geom, Shape exclude) {
//		for (Shape s : geom) {
//			if (s.equals(exclude))
//				continue;
//
//			Rectangle visionBounds = base.getBounds();
//			if (s instanceof Line2D) {
//				Line2D l = (Line2D) s;
//
//				if (intersectsLine(visionBounds, l.getX1(), l.getY1(), l.getX2(), l.getY2()))
//					subtractLine(x, y, base, l.getX1(), l.getY1(), l.getX2(), l.getY2());
//			} else if (s instanceof QuadCurve2D) {
//				QuadCurve2D c = (QuadCurve2D) s;
//
//				if (intersectsQuad(visionBounds, c.getX1(), c.getY1(), c.getCtrlX(), c.getCtrlY(), c.getX2(),
//						c.getY2()))
//					subtractQuad(x, y, base, c.getX1(), c.getY1(), c.getCtrlX(), c.getCtrlY(), c.getX2(), c.getY2());
//			} else if (s instanceof CubicCurve2D) {
//				CubicCurve2D c = (CubicCurve2D) s;
//
//				if (intersectsCubic(visionBounds, c.getX1(), c.getY1(), c.getCtrlX1(), c.getCtrlY1(), c.getCtrlX2(),
//						c.getCtrlY2(), c.getX2(), c.getY2()))
//					subtractCubic(x, y, base, c.getX1(), c.getY1(), c.getCtrlX1(), c.getCtrlY1(), c.getCtrlX2(),
//							c.getCtrlY2(), c.getX2(), c.getY2());
//
//			}
//		}
//		return base;
//	}
//
//	public static Area raycastIndividuals(double x, double y, Area obs, double rad) {
//
//		if (rad <= 0 || obs.contains(x, y))
//			return new Area();
//
//		Ellipse2D visionShape = new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2);
//		Area ret = new Area(visionShape);
//		Rectangle visionBounds = visionShape.getBounds();
//
//		// Form blocked vision
//		double[] pcoords = new double[2];
//		double[] coords = new double[6];
//		double[] mcoords = new double[2];
//		for (PathIterator iter = obs.getPathIterator(null); !iter.isDone(); iter.next()) {
//			final int oper = iter.currentSegment(coords);
//			switch (oper) {
//			case PathIterator.SEG_MOVETO:
//				mcoords[0] = coords[0];
//				mcoords[1] = coords[1];
//				pcoords[0] = coords[0];
//				pcoords[1] = coords[1];
//				break;
//			case PathIterator.SEG_CLOSE:
//				coords[0] = mcoords[0];
//				coords[1] = mcoords[1];
//			case PathIterator.SEG_LINETO:
//				if (intersectsLine(visionBounds, pcoords[0], pcoords[1], coords[0], coords[1])) {
//					subtractLine(x, y, ret, pcoords[0], pcoords[1], coords[0], coords[1]);
//				}
//				pcoords[0] = coords[0];
//				pcoords[1] = coords[1];
//				break;
//			case PathIterator.SEG_QUADTO:
//				if (intersectsQuad(visionBounds, pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3])) {
//					subtractQuad(x, y, ret, pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3]);
//				}
//				pcoords[0] = coords[2];
//				pcoords[1] = coords[3];
//				break;
//			case PathIterator.SEG_CUBICTO:
//				if (intersectsCubic(visionBounds, pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3],
//						coords[4], coords[5])) {
//					subtractCubic(x, y, ret, pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3],
//							coords[4], coords[5]);
//				}
//				pcoords[0] = coords[4];
//				pcoords[1] = coords[5];
//				break;
//			}
//		}
//
//		return ret;
//	}
//
//	public static void subtractLine(double cx, double cy, Area shape, double x1, double y1, double x2, double y2) {
//
//		Path2D block = new Path2D.Double();
//		block.moveTo(x1, y1);
//		block.lineTo(x2, y2);
//
//		Stack<Shape> reverse = new Stack<Shape>();
//		reverse.add(new Line2D.Double(x2, y2, x1, y1));
//		finishPath(shape, block, cx, cy, reverse);
//		shape.subtract(new Area(block));
//	}
//
//	//	private static final double EPSILON = 1E-6;
//	public static void subtractQuad(double cx, double cy, Area shape, double x1, double y1, double x2, double y2,
//			double x3, double y3) {
//		Quad arc = new Quad(x1, y1, x2, y2, x3, y3);
//
//		// Add path for each subtangent
//		double[] t = arc.getTangentTimes(cx, cy);
//		double pt = 0;
//
//		for (int i = 0; i < t.length; i++) {
//			if (pt == t[i])
//				continue;
//			QuadCurve2D sarc = arc.subCurve(pt, t[i]);
//			pt = t[i];
//
//			Path2D block = new Path2D.Double();
//			block.moveTo(sarc.getX1(), sarc.getY1());
//			block.quadTo(sarc.getCtrlX(), sarc.getCtrlY(), sarc.getX2(), sarc.getY2());
//
//			Stack<Shape> reverse = new Stack<Shape>();
//			reverse.add(new QuadCurve2D.Double(sarc.getX2(), sarc.getY2(), sarc.getCtrlX(), sarc.getCtrlY(),
//					sarc.getX1(), sarc.getY1()));
//			finishPath(shape, block, cx, cy, reverse);
//
//			shape.subtract(new Area(block));
//		}
//
//		if (pt < 1) {
//			// Add final path
//			QuadCurve2D sarc = arc.subCurve(pt, 1);
//
//			Path2D block = new Path2D.Double();
//			block.moveTo(sarc.getX1(), sarc.getY1());
//			block.quadTo(sarc.getCtrlX(), sarc.getCtrlY(), sarc.getX2(), sarc.getY2());
//
//			Stack<Shape> reverse = new Stack<Shape>();
//			reverse.add(new QuadCurve2D.Double(sarc.getX2(), sarc.getY2(), sarc.getCtrlX(), sarc.getCtrlY(),
//					sarc.getX1(), sarc.getY1()));
//			finishPath(shape, block, cx, cy, reverse);
//
//			shape.subtract(new Area(block));
//		}
//	}
//
//	public static void subtractCubic(double cx, double cy, Area shape, double x1, double y1, double x2, double y2,
//			double x3, double y3, double x4, double y4) {
//		Cubic arc = new Cubic(x1, y1, x2, y2, x3, y3, x4, y4);
//
//		// Add path for each subtangent
//		double[] t = arc.getTangentTimes(cx, cy);
//		double pt = 0;
//
//		for (int i = 0; i < t.length; i++) {
//			if (pt == t[i])
//				continue;
//			CubicCurve2D sarc = arc.subCurve(pt, t[i]);
//			pt = t[i];
//
//			Path2D block = new Path2D.Double();
//			block.moveTo(sarc.getX1(), sarc.getY1());
//			block.curveTo(sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getCtrlX2(), sarc.getCtrlY2(), sarc.getX2(),
//					sarc.getY2());
//
//			Stack<Shape> reverse = new Stack<Shape>();
//			reverse.add(new CubicCurve2D.Double(sarc.getX2(), sarc.getY2(), sarc.getCtrlX2(), sarc.getCtrlY2(),
//					sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getX1(), sarc.getY1()));
//			finishPath(shape, block, cx, cy, reverse);
//
//			shape.subtract(new Area(block));
//		}
//
//		// Add final path
//		if (pt < 1) {
//			CubicCurve2D sarc = arc.subCurve(pt, 1);
//
//			Path2D block = new Path2D.Double();
//			block.moveTo(sarc.getX1(), sarc.getY1());
//			block.curveTo(sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getCtrlX2(), sarc.getCtrlY2(), sarc.getX2(),
//					sarc.getY2());
//
//			Stack<Shape> reverse = new Stack<Shape>();
//			reverse.add(new CubicCurve2D.Double(sarc.getX2(), sarc.getY2(), sarc.getCtrlX2(), sarc.getCtrlY2(),
//					sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getX1(), sarc.getY1()));
//			finishPath(shape, block, cx, cy, reverse);
//
//			shape.subtract(new Area(block));
//		}
//	}

	private final double x, y;
	private final Area obs;
	private Area ret;
	private final Rectangle2D visionBounds;
	private final Curve ignore;

	public Raycast(double x, double y, Area obs, double rad) {
		this(new Area(new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2)), x, y, obs, null);
	}

	public Raycast(Area shape, double x, double y, Area obs, Curve ignore) {
		this.x = x;
		this.y = y;
		this.obs = obs;
		this.ignore = ignore;

		if (shape.isEmpty() || obs.contains(x, y)) {
			ret = new Area();
			visionBounds = null;
		} else {
			ret = shape;
			visionBounds = ret.getBounds2D();
			raycast();
			// Get rid of native artifacts from rounding (rare edge case)
			if (!visionBounds.contains(ret.getBounds2D()))
				handleArtifacts();

		}
	}

	// Builtin area subtract has some weeeeird edge cases.
	// Detects when some area gets *added* and goes outside of the intended bounds,
	// and removes the extra line.
	private void handleArtifacts() {

		Path2D fixed = new Path2D.Double();

		boolean needsMove = false;

		for (PathIterator iter = ret.getPathIterator(null); !iter.isDone(); iter.next()) {
			final int oper = iter.currentSegment(coords);
			switch (oper) {
			case PathIterator.SEG_CLOSE:
				fixed.closePath();
				break;
			case PathIterator.SEG_MOVETO:
				// Weird segments which go way out of bounds for no reason
				if (visionBounds.contains(coords[0], coords[1])) {
					fixed.moveTo(coords[0], coords[1]);

				} else {
					needsMove = true;
				}
				break;
			case PathIterator.SEG_LINETO:
				// Weird segments which go way out of bounds for no reason
				if (visionBounds.contains(coords[0], coords[1])) {
					if (needsMove) {
						fixed.moveTo(coords[0], coords[1]);
						needsMove = false;
					} else {
						fixed.lineTo(coords[0], coords[1]);
					}

				}
				break;
			case PathIterator.SEG_QUADTO:
				if (visionBounds.contains(coords[2], coords[3])) {
					if (needsMove) {
						fixed.moveTo(coords[2], coords[3]);
						needsMove = false;
					} else {
						fixed.quadTo(coords[0], coords[1], coords[2], coords[3]);
					}
				} else
					System.out.println("B");
				break;
			case PathIterator.SEG_CUBICTO:
				if (visionBounds.contains(coords[4], coords[5])) {
					if (needsMove) {
						fixed.moveTo(coords[4], coords[5]);
						needsMove = false;
					} else {
						fixed.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);

					}
				}
				break;

			}
		}
		ret = new Area(fixed);
	}

	public Area get() {
		return ret;
	}

	// Raycasting helpers
	private Path2D path = null;
	private final Stack<Shape> pathRev = new Stack<Shape>();
	private int dir;

	private final double[] coords = new double[6];
	private final double[] pcoords = new double[2], mcoords = new double[2];

	private void raycast() {
		for (PathIterator iter = obs.getPathIterator(null); !iter.isDone(); iter.next()) {
			final int oper = iter.currentSegment(coords);
			switch (oper) {
			case PathIterator.SEG_MOVETO:
				System.arraycopy(coords, 0, mcoords, 0, 2);
				System.arraycopy(coords, 0, pcoords, 0, 2);
				move();
				break;
			case PathIterator.SEG_CLOSE:
				extendLine(pcoords[0], pcoords[1], mcoords[0], mcoords[1]);
				close();
				break;
			case PathIterator.SEG_LINETO:
				extendLine(pcoords[0], pcoords[1], coords[0], coords[1]);

				// Copy coords to prev coords
				System.arraycopy(coords, 0, pcoords, 0, 2);
				break;
			case PathIterator.SEG_QUADTO:
				Quad q = new Quad(pcoords, coords);
				if (ignore != null && ignore instanceof Quad && q.equals((Quad) ignore)) {
					// Treat as line (assumed to be convex)
					extendLine(pcoords[0], pcoords[1], coords[2], coords[3]);
				} else {
					// Split by tangents
					double[] t = q.getTangentTimes(x, y);
					QuadCurve2D[] curves = new QuadCurve2D[t.length + 1];

					double pt = 0;
					for (int i = 0; i < t.length; i++) {
						if (pt == t[i])
							continue;
						curves[i] = q.subCurve(pt, t[i]);
						pt = t[i];
					}
					if (pt < 1)
						curves[t.length] = q.subCurve(pt, 1);

					// Do quad interpolation for each curve
					for (QuadCurve2D c : curves) {
						if (c == null)
							continue;

						extendQuad(c.getX1(), c.getY1(), c.getCtrlX(), c.getCtrlY(), c.getX2(), c.getY2());
					}
				}

				// Copy coords[2,3] to pcoords[0,1]
				System.arraycopy(coords, 2, pcoords, 0, 2);
				break;
			case PathIterator.SEG_CUBICTO:
				Cubic arc = new Cubic(pcoords, coords);
				if (ignore != null && ignore instanceof Cubic && arc.equals((Cubic) ignore)) {
					// Treat as line (assumed to be convex)
					extendLine(pcoords[0], pcoords[1], coords[4], coords[5]);
				} else {
					// Split by tangents
					double[] t = arc.getTangentTimes(x, y);
					CubicCurve2D[] curves = new CubicCurve2D[t.length + 1];

					double pt = 0;
					for (int i = 0; i < t.length; i++) {
						if (pt == t[i])
							continue;
						curves[i] = arc.subCurve(pt, t[i]);
						pt = t[i];
					}
					if (pt < 1)
						curves[t.length] = arc.subCurve(pt, 1);

					// Do cubic interpolation for each curve
					for (CubicCurve2D c : curves) {
						if (c == null)
							continue;

						extendCubic(c.getX1(), c.getY1(), c.getCtrlX1(), c.getCtrlY1(), c.getCtrlX2(), c.getCtrlY2(),
								c.getX2(), c.getY2());
					}
				}

				// Copy coords[4,5] to pcoords[0,1]
				System.arraycopy(coords, 4, pcoords, 0, 2);
				break;
			}
		}
	}

	private void extendLine(double x1, double y1, double x2, double y2) {
		boolean inBounds = intersectsLine(visionBounds, x1, y1, x2, y2);
//		System.out.println("Line (" + x1 + ", " + y1 + " - " + x2 + ", " + y2 + ")  with dir "
//				+ Util.orientation(x, y, x1, y1, x2, y2));

		if (path != null && !inBounds) {
			finishPath(ret, path, x, y, pathRev);
			path = null;
		}

		if (inBounds) {
			int lDir = Util.orientation(x, y, x1, y1, x2, y2);

			if (path == null) {
				dir = lDir;
				path = new Path2D.Double();
				path.moveTo(x1, y1);
			}

			if (dir != lDir) {
				// Change of direction. Finish and start new
				finishPath(ret, path, x, y, pathRev);
				dir = lDir;
				path = new Path2D.Double();
				path.moveTo(x1, y1);
			}

			// Do a line
			path.lineTo(x2, y2);
			pathRev.push(new Line2D.Double(x2, y2, x1, y1));
		}
	}

	private void extendQuad(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
		boolean inBounds = intersectsQuad(visionBounds, x1, y1, ctrlx, ctrly, x2, y2);

		if (path != null && !inBounds) {
			finishPath(ret, path, x, y, pathRev);
			path = null;

		}

		if (inBounds) {
			int qDir = Util.orientation(x, y, x1, y1, x2, y2);
			// Inverted for relational consistency
			if (new QuadCurve2D.Double(x1, y1, ctrlx, ctrly, x2, y2).contains(x, y))
				qDir *= -1;

			if (path == null) {
				dir = qDir;
				path = new Path2D.Double();
				path.moveTo(x1, y1);
			}

			if (dir != qDir) {
				// Change of direction. Finish and start new
				finishPath(ret, path, x, y, pathRev);
				dir = qDir;
				path = new Path2D.Double();
				path.moveTo(x1, y1);
			}

			// Do a quad
			path.quadTo(ctrlx, ctrly, x2, y2);
			pathRev.push(new QuadCurve2D.Double(x2, y2, ctrlx, ctrly, x1, y1));
		}
	}

	private void extendCubic(double x1, double y1, double ctrlx1, double ctrly1, double ctrlx2, double ctrly2,
			double x2, double y2) {
//		System.out.println("Cubic with dir " + Util.orientation(x, y, x1, y1, x2, y2));
		boolean inBounds = intersectsCubic(visionBounds, x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);

		if (path != null && !inBounds) {
			finishPath(ret, path, x, y, pathRev);
			path = null;

		}

		if (inBounds) {
			int qDir = Util.orientation(x, y, x1, y1, x2, y2);
			// Inverted for relational consistency
			if (new CubicCurve2D.Double(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2).contains(x, y))
				qDir *= -1;

			if (path == null) {
				dir = qDir;
				path = new Path2D.Double();
				path.moveTo(x1, y1);
			}

			if (dir != qDir) {
				// Change of direction. Finish and start new
				finishPath(ret, path, x, y, pathRev);
				dir = qDir;
				path = new Path2D.Double();
				path.moveTo(x1, y1);
			}

			// Do a cubic
			path.curveTo(ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
			pathRev.push(new CubicCurve2D.Double(x2, y2, ctrlx2, ctrly2, ctrlx1, ctrly1, x1, y1));
		}
	}

	private void move() {
		// Unused for now
	}

	private void close() {
		finishPath(ret, path, x, y, pathRev);
		path = null;
	}

	private static void finishPath(Area ret, Path2D path, double x, double y, Stack<Shape> pathRev) {
		if (pathRev.size() == 0)
			return;

		// Do initial raycast
		Shape init = pathRev.lastElement(), fin = pathRev.firstElement();

		double x1, y1, x2, y2;
		if (init instanceof Line2D) {
			Line2D s = (Line2D) init;
			x1 = s.getX1();
			y1 = s.getY1();
		} else if (init instanceof QuadCurve2D) {
			QuadCurve2D s = (QuadCurve2D) init;
			x1 = s.getX1();
			y1 = s.getY1();
		} else {
			CubicCurve2D s = (CubicCurve2D) init;
			x1 = s.getX1();
			y1 = s.getY1();
		}
		if (fin instanceof Line2D) {
			Line2D s = (Line2D) fin;
			x2 = s.getX2();
			y2 = s.getY2();
		} else if (fin instanceof QuadCurve2D) {
			QuadCurve2D s = (QuadCurve2D) fin;
			x2 = s.getX2();
			y2 = s.getY2();
		} else {
			CubicCurve2D s = (CubicCurve2D) fin;
			x2 = s.getX2();
			y2 = s.getY2();
		}

		// Raycast into far distance
		double a1 = Math.atan2(y1 - y, x1 - x);
		double a2 = Math.atan2(y2 - y, x2 - x);
		double ix = x + Math.cos(a1) * RAYCAST_DIST;
		double iy = y + Math.sin(a1) * RAYCAST_DIST;
		double fx = x + Math.cos(a2) * RAYCAST_DIST;
		double fy = y + Math.sin(a2) * RAYCAST_DIST;

		// Initial line outwards
		path.lineTo(ix, iy);

		// Follow path in reverse
		while (pathRev.size() > 0) {
			Shape add = pathRev.pop();
			if (add instanceof Line2D) {
				Line2D l = (Line2D) add;
				final double fx2 = x + (l.getX2() - x) * RAYCAST_DIST;
				final double fy2 = y + (l.getY2() - y) * RAYCAST_DIST;
				path.lineTo(fx2, fy2);

			} else if (add instanceof QuadCurve2D) {
				QuadCurve2D c = (QuadCurve2D) add;
				final double fx1 = x + (c.getX1() - x) * RAYCAST_DIST;
				final double fy1 = y + (c.getY1() - y) * RAYCAST_DIST;
				final double fxc = x + (c.getCtrlX() - x) * RAYCAST_DIST;
				final double fyc = y + (c.getCtrlY() - y) * RAYCAST_DIST;
				final double fx2 = x + (c.getX2() - x) * RAYCAST_DIST;
				final double fy2 = y + (c.getY2() - y) * RAYCAST_DIST;
				path.lineTo(fx1, fy1);
				path.quadTo(fxc, fyc, fx2, fy2);
			} else if (add instanceof CubicCurve2D) {
				CubicCurve2D c = (CubicCurve2D) add;
				final double fx1 = x + (c.getX1() - x) * RAYCAST_DIST;
				final double fy1 = y + (c.getY1() - y) * RAYCAST_DIST;
				final double fxc1 = x + (c.getCtrlX1() - x) * RAYCAST_DIST;
				final double fyc1 = y + (c.getCtrlY1() - y) * RAYCAST_DIST;
				final double fxc2 = x + (c.getCtrlX2() - x) * RAYCAST_DIST;
				final double fyc2 = y + (c.getCtrlY2() - y) * RAYCAST_DIST;
				final double fx2 = x + (c.getX2() - x) * RAYCAST_DIST;
				final double fy2 = y + (c.getY2() - y) * RAYCAST_DIST;
				path.lineTo(fx1, fy1);
				path.curveTo(fxc1, fyc1, fxc2, fyc2, fx2, fy2);
			}
		}
		// Ending line
		path.lineTo(fx, fy);
		path.closePath();

		ret.subtract(new Area(path));
	}

	private static boolean intersectsLine(Rectangle2D visionBounds, double x1, double y1, double x2, double y2) {
		return visionBounds.intersectsLine(x1, y1, x2, y2);
	}

	private static boolean intersectsQuad(Rectangle2D bounds, double x1, double y1, double x2, double y2, double x3,
			double y3) {
		// Do by bounds
		double minX = Math.min(x1, Math.min(x2, x3));
		double maxX = Math.max(x1, Math.max(x2, x3));
		double minY = Math.min(y1, Math.min(y2, y3));
		double maxY = Math.max(y1, Math.max(y2, y3));
		return bounds.intersects(minX, minY, maxX - minX, maxY - minY);
	}

	private static boolean intersectsCubic(Rectangle2D bounds, double x1, double y1, double x2, double y2, double x3,
			double y3, double x4, double y4) {
		// Do by bounds
		double minX = Math.min(x1, Math.min(x2, Math.min(x3, x4)));
		double maxX = Math.max(x1, Math.max(x2, Math.max(x3, x4)));
		double minY = Math.min(y1, Math.min(y2, Math.min(y3, y4)));
		double maxY = Math.max(y1, Math.max(y2, Math.max(y3, y4)));
		return bounds.intersects(minX, minY, maxX - minX, maxY - minY);
	}

}
