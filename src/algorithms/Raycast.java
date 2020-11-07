package algorithms;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Stack;

import helpers.Util;
import helpers.geom.Cubic;
import helpers.geom.Quad;

public class Raycast {

	private static final int RAYCAST_DIST = (int) 1E9;
	private static final double EPSILON = 0.01;

	public static Area raycastIndividuals(Area base, double x, double y, ArrayList<Shape> geom, Shape exclude) {
		for (Shape s : geom) {
			if (s.equals(exclude))
				continue;

			Rectangle visionBounds = base.getBounds();
			if (s instanceof Line2D) {
				Line2D l = (Line2D) s;

				if (intersectsLine(visionBounds, l.getX1(), l.getY1(), l.getX2(), l.getY2()))
					subtractLine(x, y, base, l.getX1(), l.getY1(), l.getX2(), l.getY2());
			} else if (s instanceof QuadCurve2D) {
				QuadCurve2D c = (QuadCurve2D) s;

				if (intersectsQuad(visionBounds, c.getX1(), c.getY1(), c.getCtrlX(), c.getCtrlY(), c.getX2(),
						c.getY2()))
					subtractQuad(x, y, base, c.getX1(), c.getY1(), c.getCtrlX(), c.getCtrlY(), c.getX2(), c.getY2());
			} else if (s instanceof CubicCurve2D) {
				CubicCurve2D c = (CubicCurve2D) s;

				if (intersectsCubic(visionBounds, c.getX1(), c.getY1(), c.getCtrlX1(), c.getCtrlY1(), c.getCtrlX2(),
						c.getCtrlY2(), c.getX2(), c.getY2()))
					subtractCubic(x, y, base, c.getX1(), c.getY1(), c.getCtrlX1(), c.getCtrlY1(), c.getCtrlX2(),
							c.getCtrlY2(), c.getX2(), c.getY2());

			}
		}
		return base;
	}

	public static Area raycastIndividuals(double x, double y, Area obs, double rad) {

		if (rad <= 0 || obs.contains(x, y))
			return new Area();

		Ellipse2D visionShape = new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2);
		Area ret = new Area(visionShape);
		Rectangle visionBounds = visionShape.getBounds();

		// Form blocked vision
		double[] pcoords = new double[2];
		double[] coords = new double[6];
		double[] mcoords = new double[2];
		for (PathIterator iter = obs.getPathIterator(null); !iter.isDone(); iter.next()) {
			final int oper = iter.currentSegment(coords);
			switch (oper) {
			case PathIterator.SEG_MOVETO:
				mcoords[0] = coords[0];
				mcoords[1] = coords[1];
				pcoords[0] = coords[0];
				pcoords[1] = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				coords[0] = mcoords[0];
				coords[1] = mcoords[1];
			case PathIterator.SEG_LINETO:
				if (intersectsLine(visionBounds, pcoords[0], pcoords[1], coords[0], coords[1])) {
					subtractLine(x, y, ret, pcoords[0], pcoords[1], coords[0], coords[1]);
				}
				pcoords[0] = coords[0];
				pcoords[1] = coords[1];
				break;
			case PathIterator.SEG_QUADTO:
				if (intersectsQuad(visionBounds, pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3])) {
					subtractQuad(x, y, ret, pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3]);
				}
				pcoords[0] = coords[2];
				pcoords[1] = coords[3];
				break;
			case PathIterator.SEG_CUBICTO:
				if (intersectsCubic(visionBounds, pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3],
						coords[4], coords[5])) {
					subtractCubic(x, y, ret, pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3],
							coords[4], coords[5]);
				}
				pcoords[0] = coords[4];
				pcoords[1] = coords[5];
				break;
			}
		}

		return ret;
	}

	private static boolean intersectsLine(Rectangle bounds, double x1, double y1, double x2, double y2) {
		return bounds.intersectsLine(x1, y1, x2, y2);
	}

	public static void subtractLine(double cx, double cy, Area shape, double x1, double y1, double x2, double y2) {

		Path2D block = new Path2D.Double();
		block.moveTo(x1, y1);
		block.lineTo(x2, y2);

		Stack<Shape> reverse = new Stack<Shape>();
		reverse.add(new Line2D.Double(x2, y2, x1, y1));
		finishPath(shape, block, cx, cy, reverse);
		shape.subtract(new Area(block));
	}

	private static boolean intersectsQuad(Rectangle bounds, double x1, double y1, double x2, double y2, double x3,
			double y3) {
		// Do by bounds
		double minX = Math.min(x1, Math.min(x2, x3));
		double maxX = Math.max(x1, Math.max(x2, x3));
		double minY = Math.min(y1, Math.min(y2, y3));
		double maxY = Math.max(y1, Math.max(y2, y3));
		return bounds.intersects(minX, minY, maxX - minX, maxY - minY);
	}

//	private static final double EPSILON = 1E-6;
	public static void subtractQuad(double cx, double cy, Area shape, double x1, double y1, double x2, double y2,
			double x3, double y3) {
		Quad arc = new Quad(x1, y1, x2, y2, x3, y3);

		// Add path for each subtangent
		double[] t = arc.getTangentTimes(cx, cy);
		double pt = 0;

		for (int i = 0; i < t.length; i++) {
			if (pt == t[i])
				continue;
			QuadCurve2D sarc = arc.subCurve(pt, t[i]);
			pt = t[i];

			Path2D block = new Path2D.Double();
			block.moveTo(sarc.getX1(), sarc.getY1());
			block.quadTo(sarc.getCtrlX(), sarc.getCtrlY(), sarc.getX2(), sarc.getY2());

			Stack<Shape> reverse = new Stack<Shape>();
			reverse.add(new QuadCurve2D.Double(sarc.getX2(), sarc.getY2(), sarc.getCtrlX(), sarc.getCtrlY(),
					sarc.getX1(), sarc.getY1()));
			finishPath(shape, block, cx, cy, reverse);

			shape.subtract(new Area(block));
		}

		if (pt < 1) {
			// Add final path
			QuadCurve2D sarc = arc.subCurve(pt, 1);

			Path2D block = new Path2D.Double();
			block.moveTo(sarc.getX1(), sarc.getY1());
			block.quadTo(sarc.getCtrlX(), sarc.getCtrlY(), sarc.getX2(), sarc.getY2());

			Stack<Shape> reverse = new Stack<Shape>();
			reverse.add(new QuadCurve2D.Double(sarc.getX2(), sarc.getY2(), sarc.getCtrlX(), sarc.getCtrlY(),
					sarc.getX1(), sarc.getY1()));
			finishPath(shape, block, cx, cy, reverse);

			shape.subtract(new Area(block));
		}
	}

	private static boolean intersectsCubic(Rectangle bounds, double x1, double y1, double x2, double y2, double x3,
			double y3, double x4, double y4) {
		// Do by bounds
		double minX = Math.min(x1, Math.min(x2, Math.min(x3, x4)));
		double maxX = Math.max(x1, Math.max(x2, Math.max(x3, x4)));
		double minY = Math.min(y1, Math.min(y2, Math.min(y3, y4)));
		double maxY = Math.max(y1, Math.max(y2, Math.max(y3, y4)));
		return bounds.intersects(minX, minY, maxX - minX, maxY - minY);
	}

	public static void subtractCubic(double cx, double cy, Area shape, double x1, double y1, double x2, double y2,
			double x3, double y3, double x4, double y4) {
		Cubic arc = new Cubic(x1, y1, x2, y2, x3, y3, x4, y4);

		// Add path for each subtangent
		double[] t = arc.getTangentTimes(cx, cy);
		double pt = 0;

		for (int i = 0; i < t.length; i++) {
			if (pt == t[i])
				continue;
			CubicCurve2D sarc = arc.subCurve(pt, t[i]);
			pt = t[i];

			Path2D block = new Path2D.Double();
			block.moveTo(sarc.getX1(), sarc.getY1());
			block.curveTo(sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getCtrlX2(), sarc.getCtrlY2(), sarc.getX2(),
					sarc.getY2());

			Stack<Shape> reverse = new Stack<Shape>();
			reverse.add(new CubicCurve2D.Double(sarc.getX2(), sarc.getY2(), sarc.getCtrlX2(), sarc.getCtrlY2(),
					sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getX1(), sarc.getY1()));
			finishPath(shape, block, cx, cy, reverse);

			shape.subtract(new Area(block));
		}

		// Add final path
		if (pt < 1) {
			CubicCurve2D sarc = arc.subCurve(pt, 1);

			Path2D block = new Path2D.Double();
			block.moveTo(sarc.getX1(), sarc.getY1());
			block.curveTo(sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getCtrlX2(), sarc.getCtrlY2(), sarc.getX2(),
					sarc.getY2());

			Stack<Shape> reverse = new Stack<Shape>();
			reverse.add(new CubicCurve2D.Double(sarc.getX2(), sarc.getY2(), sarc.getCtrlX2(), sarc.getCtrlY2(),
					sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getX1(), sarc.getY1()));
			finishPath(shape, block, cx, cy, reverse);

			shape.subtract(new Area(block));
		}
	}

	private static void finishPath(Area ret, Path2D path, double x, double y, Stack<Shape> pathRev) {
		if (pathRev.size() == 0)
			// throw new RuntimeException("Bad reverse path size of 0");
			return;

		// Do initial raycast
		Shape init = pathRev.firstElement(), fin = pathRev.lastElement();

		double x1, y1, x2, y2;
		double offs;
		if (init instanceof Line2D) {
			Line2D s = (Line2D) init;
			x1 = s.getX1();
			y1 = s.getY1();
			offs = EPSILON * Util.orientation(x, y, s.getX1(), s.getY1(), s.getX2(), s.getY2());
		} else if (init instanceof QuadCurve2D) {
			QuadCurve2D s = (QuadCurve2D) init;
			x1 = s.getX1();
			y1 = s.getY1();
			offs = EPSILON * Util.orientation(x, y, s.getX1(), s.getY1(), s.getCtrlX(), s.getCtrlY());
		} else {
			CubicCurve2D s = (CubicCurve2D) init;
			x1 = s.getX1();
			y1 = s.getY1();
			offs = EPSILON * Util.orientation(x, y, s.getX1(), s.getY1(), s.getCtrlX1(), s.getCtrlY1());
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

		// "Fix" (suspected) rounding error by slight offset
		double a1 = Math.atan2(y1 - y, x1 - x) + offs;
		double a2 = Math.atan2(y2 - y, x2 - x) - offs;

		double ix = x + Math.cos(a1) * RAYCAST_DIST;
		double iy = y + Math.sin(a1) * RAYCAST_DIST;
		double fx = x + Math.cos(a2) * RAYCAST_DIST;
		double fy = y + Math.sin(a2) * RAYCAST_DIST;

		// Initial line outwards
		path.lineTo(ix, iy);

		while (pathRev.size() > 0) {
			Shape add = pathRev.pop();
			if (add instanceof Line2D) {
				Line2D l = (Line2D) add;
				final double fx2 = x + (l.getX2() - x) * RAYCAST_DIST;
				final double fy2 = y + (l.getY2() - y) * RAYCAST_DIST;
				path.lineTo(fx2, fy2);

			} else if (add instanceof QuadCurve2D) {
				QuadCurve2D c = (QuadCurve2D) add;
				final double fxc = x + (c.getCtrlX() - x) * RAYCAST_DIST;
				final double fyc = y + (c.getCtrlY() - y) * RAYCAST_DIST;
				final double fx2 = x + (c.getX2() - x) * RAYCAST_DIST;
				final double fy2 = y + (c.getY2() - y) * RAYCAST_DIST;
				path.quadTo(fxc, fyc, fx2, fy2);
			} else if (add instanceof CubicCurve2D) {
				CubicCurve2D c = (CubicCurve2D) add;
				final double fxc1 = x + (c.getCtrlX1() - x) * RAYCAST_DIST;
				final double fyc1 = y + (c.getCtrlY1() - y) * RAYCAST_DIST;
				final double fxc2 = x + (c.getCtrlX2() - x) * RAYCAST_DIST;
				final double fyc2 = y + (c.getCtrlY2() - y) * RAYCAST_DIST;
				final double fx2 = x + (c.getX2() - x) * RAYCAST_DIST;
				final double fy2 = y + (c.getY2() - y) * RAYCAST_DIST;
				path.curveTo(fxc1, fyc1, fxc2, fyc2, fx2, fy2);
			}
		}
		path.lineTo(fx, fy);
		path.closePath();
		ret.subtract(new Area(path));

	}

	// TODO this alg is broken rn
	public static Area raycastWhole(double x, double y, Area obs, double rad) {

		if (rad <= 0 || obs.contains(x, y))
			return new Area();

		Ellipse2D visionShape = new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2);
		Area ret = new Area(visionShape);
		Rectangle visionBounds = visionShape.getBounds();

		ArrayList<Path2D> paths = new ArrayList<Path2D>();

		// Form blocked vision
		Path2D path = null;
		Stack<Shape> pathRev = null;
		int dir = 0;
		double sx = 0, sy = 0;
		double[] pcoords = new double[2];
		double[] coords = new double[6];
		double[] mcoords = new double[2];
		for (PathIterator iter = obs.getPathIterator(null); !iter.isDone(); iter.next()) {
			final int oper = iter.currentSegment(coords);
			switch (oper) {
			case PathIterator.SEG_MOVETO:
				mcoords[0] = coords[0];
				mcoords[1] = coords[1];
				pcoords[0] = coords[0];
				pcoords[1] = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				coords[0] = mcoords[0];
				coords[1] = mcoords[1];
			case PathIterator.SEG_LINETO:
				// Should this segment cast a shadow?
				boolean inBounds = intersectsLine(visionBounds, pcoords[0], pcoords[1], coords[0], coords[1]);
				if (inBounds || path != null) {
					if (path == null) {
						sx = pcoords[0];
						sy = pcoords[1];
						dir = Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1]);
						path = new Path2D.Double();
						path.moveTo(sx, sy);
						pathRev = new Stack<Shape>();
					}

					// Ensure still moving in same direction
					if (inBounds && dir != Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1])) {
						finishPath(ret, path, x, y, pathRev);
						paths.add(path);
						sx = pcoords[0];
						sy = pcoords[1];
						dir = Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1]);
						path = new Path2D.Double();
						path.moveTo(sx, sy);
						pathRev = new Stack<Shape>();
					}
					if (Math.hypot(coords[0] - pcoords[0], coords[1] - pcoords[1]) > EPSILON) {
						path.lineTo(coords[0], coords[1]);
						pathRev.push(new Line2D.Double(coords[0], coords[1], pcoords[0], pcoords[1]));
					}

					// Out of bounds or close segments are automatic finishes
					if (!inBounds || oper == PathIterator.SEG_CLOSE) {
						finishPath(ret, path, x, y, pathRev);
						paths.add(path);
						path = null;
						pathRev = null;
					}
				}
				pcoords[0] = coords[0];
				pcoords[1] = coords[1];
				break;

			case PathIterator.SEG_QUADTO: {

				// Split by tangents
				Quad arc = new Quad(pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3]);
				double[] t = arc.getTangentTimes(x, y);
				QuadCurve2D[] curves = new QuadCurve2D[t.length + 1];
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
				for (QuadCurve2D c : curves) {
					if (c == null)
						continue;
					coords[0] = c.getCtrlX();
					coords[1] = c.getCtrlY();
					coords[2] = c.getX2();
					coords[3] = c.getY2();

					// Should this segment cast a shadow?
					inBounds = intersectsQuad(visionBounds, pcoords[0], pcoords[1], coords[0], coords[1], coords[2],
							coords[3]);
					if (inBounds || path != null) {
						if (path == null) {
							sx = pcoords[0];
							sy = pcoords[1];
							dir = Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1]);
							path = new Path2D.Double();
							path.moveTo(sx, sy);
							pathRev = new Stack<Shape>();
						}

						// Ensure still moving in same direction
						// if (inBounds && dir != Util.orientation(x, y, pcoords[0], pcoords[1],
						// coords[0], coords[1])) {
						// Finish and start new!
						finishPath(ret, path, x, y, pathRev);
						paths.add(path);
						sx = pcoords[0];
						sy = pcoords[1];
						dir = Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1]);
						path = new Path2D.Double();
						path.moveTo(sx, sy);
						pathRev = new Stack<Shape>();
						// }

						path.quadTo(coords[0], coords[1], coords[2], coords[3]);
						pathRev.push(new QuadCurve2D.Double(coords[2], coords[3], coords[0], coords[1], pcoords[0],
								pcoords[1]));

						// If just moved out of bounds, also finish
						if (!inBounds) {
							finishPath(ret, path, x, y, pathRev);
							paths.add(path);
							path = null;
							pathRev = null;

						}
					}
					pcoords[0] = coords[2];
					pcoords[1] = coords[3];
				}
				break;
			}

			case PathIterator.SEG_CUBICTO: {
				// Split by tangents
				Cubic arc = new Cubic(pcoords[0], pcoords[1], coords[0], coords[1], coords[2], coords[3], coords[4],
						coords[5]);
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
					coords[0] = c.getCtrlX1();
					coords[1] = c.getCtrlY1();
					coords[2] = c.getCtrlX2();
					coords[3] = c.getCtrlY2();
					coords[4] = c.getX2();
					coords[5] = c.getY2();

					// Should this segment cast a shadow?
					inBounds = intersectsCubic(visionBounds, pcoords[0], pcoords[1], coords[0], coords[1], coords[2],
							coords[3], coords[4], coords[5]);
					if (inBounds || path != null) {
						if (path == null) {
							sx = pcoords[0];
							sy = pcoords[1];
							dir = Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1]);
							path = new Path2D.Double();
							path.moveTo(sx, sy);
							pathRev = new Stack<Shape>();
						}

						// TODO why doesn't this optimization work?
						// Ensure still moving in same direction
						// if (inBounds && dir != Util.orientation(x, y, pcoords[0], pcoords[1],
						// coords[0], coords[1])) {
						// Finish and start new!
						finishPath(ret, path, x, y, pathRev);
						paths.add(path);
						sx = pcoords[0];
						sy = pcoords[1];
						dir = Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1]);
						path = new Path2D.Double();
						path.moveTo(sx, sy);
						pathRev = new Stack<Shape>();
						// }

						path.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
						pathRev.push(new CubicCurve2D.Double(coords[4], coords[5], coords[2], coords[3], coords[0],
								coords[1], pcoords[0], pcoords[1]));

						// If just moved out of bounds, also finish
						if (!inBounds) {
							finishPath(ret, path, x, y, pathRev);
							paths.add(path);
							path = null;
							pathRev = null;

						}
					}
					pcoords[0] = coords[4];
					pcoords[1] = coords[5];
				}
				break;
			}
			}
		}

		return ret;

	}

}
