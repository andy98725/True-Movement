import java.awt.Rectangle;
import java.awt.geom.*;
import java.util.ArrayList;

import helpers.*;

public class Raycast {

	private static final int RAYCAST_DIST = (int) 1E9;

//	private static final double EPSILON = 1E-2;

	public static Area raycast(double x, double y, Area obs, double rad) {

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

	public static Area raycastNew(double x, double y, Area obs, double rad) {

		if (rad <= 0 || obs.contains(x, y))
			return new Area();

		Ellipse2D visionShape = new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2);
		Area ret = new Area(visionShape);
		Rectangle visionBounds = visionShape.getBounds();

		ArrayList<Path2D> paths = new ArrayList<Path2D>();

		// Form blocked vision
		Path2D path = null;
		int dir = 0;
		double sx = 0, sy = 0;
		double[] pcoords = new double[2];
		double[] coords = new double[6];
		double[] mcoords = new double[2];
		// TODO make not flattening
		for (PathIterator iter = obs.getPathIterator(null, 1); !iter.isDone(); iter.next()) {
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
				if (visionBounds.intersectsLine(pcoords[0], pcoords[1], coords[0], coords[1])) {

//					sx = pcoords[0];
//					sy = pcoords[1];
//					dir = Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1]);
//					path = new Path2D.Double();
//					path.moveTo(sx, sy);
//					path.lineTo(coords[0], coords[1]);
//					finishPath(ret, path, x, y, sx, sy, coords[0], coords[1]);
//					paths.add(path);
//					path = null;

					if (path == null) {
						sx = pcoords[0];
						sy = pcoords[1];
						dir = Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1]);
						path = new Path2D.Double();
						path.moveTo(sx, sy);
					}

					// Ensure still moving in same direction
					if (dir == Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1])) {
						path.lineTo(coords[0], coords[1]);
					} else {
						// Finish and start new!
						finishPath(ret, path, x, y, sx, sy, pcoords[0], pcoords[1], dir);
						paths.add(path);
						sx = pcoords[0];
						sy = pcoords[1];
						dir = Util.orientation(x, y, pcoords[0], pcoords[1], coords[0], coords[1]);
						path = new Path2D.Double();
						path.moveTo(sx, sy);
						path.lineTo(coords[0], coords[1]);

					}
					// Close segments are automatic finishes too
					if (oper == PathIterator.SEG_CLOSE) {
						finishPath(ret, path, x, y, sx, sy, coords[0], coords[1], dir);
						paths.add(path);
						path = null;
					}
				} else {
					// If just moved out of bounds, also finish
					if (path != null) {
						path.lineTo(coords[0], coords[1]);
						finishPath(ret, path, x, y, sx, sy, coords[0], coords[1], dir);
						paths.add(path);
						path = null;

					}
				}
				pcoords[0] = coords[0];
				pcoords[1] = coords[1];
				break;
			}
		}

		return ret;
	}

	private static void finishPath(Area ret, Path2D path, double x, double y, double startX, double startY, double endX,
			double endY, int dir) {
		Path2D bak = (Path2D) path.clone();
		addBorder(path, ret.getBounds(), x, y, startX, startY, endX, endY, 1);
		path.closePath();
		if (path.contains(x, y)) {
			System.out.println("Oh no");
			addBorder(bak, ret.getBounds(), x, y, startX, startY, endX, endY, -1); // TODO this isn't working??
			bak.closePath();
			ret.subtract(new Area(bak));
//			ret.subtract(new Area(new Ellipse2D.Double(endX-8, endY-8, 16, 16)));
		} else {
			ret.subtract(new Area(path));
		}
	}

	// Add the outer border path to the generalPath, avoiding going inside the
	// bounds provided
	private static void addBorder(Path2D p, Rectangle visionBounds, double x, double y, double x1, double y1, double x2,
			double y2, int dir) {
		// Raycast segment into distance
		final double angle1 = Math.atan2(y1 - y, x1 - x);
		final double angle2 = Math.atan2(y2 - y, x2 - x);

		final double fx1 = x + Math.cos(angle1) * RAYCAST_DIST;
		final double fy1 = y + Math.sin(angle1) * RAYCAST_DIST;
		final double fx2 = x + Math.cos(angle2) * RAYCAST_DIST;
		final double fy2 = y + Math.sin(angle2) * RAYCAST_DIST;

		p.lineTo(fx2, fy2);
//		int o = Util.orientation(x, y, x1, y1, x2, y2);
		if (dir == -1) {
			System.out.println(dir + ", ");
			double avgAngle = Math.atan2(Math.sin(angle2) - Math.sin(angle1), Math.cos(angle2) - Math.cos(angle1));
			final double medx = x + Math.cos(avgAngle) * RAYCAST_DIST * dir;
			final double medy = y + Math.sin(avgAngle) * RAYCAST_DIST * dir;

			p.lineTo(medx, medy);
		}
		// Ensure outer bound doesn't intersect vision shape by adding another point
		else {// if (visionBounds.intersectsLine(fx1, fy1, fx2, fy2)) {
			double avgAngle = Math.atan2(Math.sin(angle2) - Math.sin(angle1), Math.cos(angle2) - Math.cos(angle1));
			final double medx = x + Math.cos(avgAngle) * RAYCAST_DIST;
			final double medy = y + Math.sin(avgAngle) * RAYCAST_DIST;

			p.lineTo(medx, medy);
		}
		p.lineTo(fx1, fy1);

	}

	private static boolean intersectsLine(Rectangle bounds, double x1, double y1, double x2, double y2) {
		return bounds.intersectsLine(x1, y1, x2, y2);
	}

	private static void subtractLine(double cx, double cy, Area shape, double x1, double y1, double x2, double y2) {

		Path2D block = new Path2D.Double();
		block.moveTo(x1, y1);
		block.lineTo(x2, y2);
		closePath(cx, cy, block, x1, y1, x2, y2);
		shape.subtract(new Area(block));
	}

	private static void closePath(double cx, double cy, Path2D path, double x1, double y1, double x2, double y2) {
		// Raycast segment into distance
		final double fx1 = cx + (x1 - cx) * RAYCAST_DIST;
		final double fy1 = cy + (y1 - cy) * RAYCAST_DIST;
		final double fx2 = cx + (x2 - cx) * RAYCAST_DIST;
		final double fy2 = cy + (y2 - cy) * RAYCAST_DIST;
		path.lineTo(fx2, fy2);
		path.lineTo(fx1, fy1);
		path.closePath();
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
	private static void subtractQuad(double cx, double cy, Area shape, double x1, double y1, double x2, double y2,
			double x3, double y3) {
		Quad arc = new Quad(x1, y1, x2, y2, x3, y3);

		// Add path for each subtangent
		double[] t = arc.getTangentTimes(cx, cy);
		double pt = 0;
		for (int i = 0; i < t.length; i++) {
			QuadCurve2D sarc = arc.subCurve(pt, t[i]);
			pt = t[i];

			Path2D block = new Path2D.Double();
			block.moveTo(sarc.getX1(), sarc.getY1());
			block.quadTo(sarc.getCtrlX(), sarc.getCtrlY(), sarc.getX2(), sarc.getY2());
			closePath(cx, cy, block, sarc.getX1(), sarc.getY1(), sarc.getCtrlX(), sarc.getCtrlY(), sarc.getX2(),
					sarc.getY2());
			shape.subtract(new Area(block));
		}

		// Add final path
		QuadCurve2D sarc = arc.subCurve(pt, 1);
		Path2D block = new Path2D.Double();
		block.moveTo(sarc.getX1(), sarc.getY1());
		block.quadTo(sarc.getCtrlX(), sarc.getCtrlY(), sarc.getX2(), sarc.getY2());
		closePath(cx, cy, block, sarc.getX1(), sarc.getY1(), sarc.getCtrlX(), sarc.getCtrlY(), sarc.getX2(),
				sarc.getY2());
		shape.subtract(new Area(block));
	}

	private static void closePath(double cx, double cy, Path2D path, double x1, double y1, double x2, double y2,
			double x3, double y3) {
		// Raycast curve into distance
		final double fx1 = cx + (x1 - cx) * RAYCAST_DIST;
		final double fy1 = cy + (y1 - cy) * RAYCAST_DIST;
		final double fx2 = cx + (x2 - cx) * RAYCAST_DIST;
		final double fy2 = cy + (y2 - cy) * RAYCAST_DIST;
		final double fx3 = cx + (x3 - cx) * RAYCAST_DIST;
		final double fy3 = cy + (y3 - cy) * RAYCAST_DIST;
		path.lineTo(fx3, fy3);
		path.quadTo(fx2, fy2, fx1, fy1);
		path.closePath();
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

	private static void subtractCubic(double cx, double cy, Area shape, double x1, double y1, double x2, double y2,
			double x3, double y3, double x4, double y4) {
		Cubic arc = new Cubic(x1, y1, x2, y2, x3, y3, x4, y4);

		// Add path for each subtangent
		double[] t = arc.getTangentTimes(cx, cy);
		double pt = 0;
		for (int i = 0; i < t.length; i++) {
			CubicCurve2D sarc = arc.subCurve(pt, t[i]);
			pt = t[i];

			Path2D block = new Path2D.Double();
			block.moveTo(sarc.getX1(), sarc.getY1());
			block.curveTo(sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getCtrlX2(), sarc.getCtrlY2(), sarc.getX2(),
					sarc.getY2());
			closePath(cx, cy, block, sarc.getX1(), sarc.getY1(), sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getCtrlX2(),
					sarc.getCtrlY2(), sarc.getX2(), sarc.getY2());
			shape.subtract(new Area(block));
		}

		// Add final path
		CubicCurve2D sarc = arc.subCurve(pt, 1);
		Path2D block = new Path2D.Double();
		block.moveTo(sarc.getX1(), sarc.getY1());
		block.curveTo(sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getCtrlX2(), sarc.getCtrlY2(), sarc.getX2(),
				sarc.getY2());
		closePath(cx, cy, block, sarc.getX1(), sarc.getY1(), sarc.getCtrlX1(), sarc.getCtrlY1(), sarc.getCtrlX2(),
				sarc.getCtrlY2(), sarc.getX2(), sarc.getY2());
		shape.subtract(new Area(block));
	}

	private static void closePath(double cx, double cy, Path2D path, double x1, double y1, double x2, double y2,
			double x3, double y3, double x4, double y4) {
		// Raycast curve into distance
		final double fx1 = cx + (x1 - cx) * RAYCAST_DIST;
		final double fy1 = cy + (y1 - cy) * RAYCAST_DIST;
		final double fx2 = cx + (x2 - cx) * RAYCAST_DIST;
		final double fy2 = cy + (y2 - cy) * RAYCAST_DIST;
		final double fx3 = cx + (x3 - cx) * RAYCAST_DIST;
		final double fy3 = cy + (y3 - cy) * RAYCAST_DIST;
		final double fx4 = cx + (x4 - cx) * RAYCAST_DIST;
		final double fy4 = cy + (y4 - cy) * RAYCAST_DIST;
		path.lineTo(fx4, fy4);
		path.curveTo(fx3, fy3, fx2, fy2, fx1, fy1);
		path.closePath();
	}

}
