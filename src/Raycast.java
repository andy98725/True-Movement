import java.awt.Rectangle;
import java.awt.geom.*;

public class Raycast {


	private static final int RAYCAST_DIST = Integer.MAX_VALUE;

	private static final double EPSILON = 1E-6;

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
				if (visionBounds.intersectsLine(pcoords[0], pcoords[1], coords[0], coords[1])) {
					Path2D p = new Path2D.Double();
					// Extend slightly
					final double angle = Math.atan2(pcoords[1] - coords[1], pcoords[0] - coords[0]);
					p.moveTo(pcoords[0] + EPSILON * Math.cos(angle), pcoords[1] + EPSILON * Math.sin(angle));
					p.lineTo(coords[0] - EPSILON * Math.cos(angle), coords[1] - EPSILON * Math.sin(angle));
//					p.moveTo(pcoords[0], pcoords[1]);
//					p.lineTo(coords[0], coords[1]);
					addBorder(p, visionBounds, x, y, pcoords[0], pcoords[1], coords[0], coords[1]);
					p.closePath();
					ret.subtract(new Area(p));
				}
				pcoords[0] = coords[0];
				pcoords[1] = coords[1];
				break;
			}
		}

		return ret;
	}

	// Add the outer border path to the generalPath, avoiding going inside the
	// bounds provided
	private static void addBorder(Path2D p, Rectangle visionBounds, double x, double y, double x1, double y1, double x2,
			double y2) {
		// Raycast segment into distance
		final double angle1 = Math.atan2(y1 - y, x1 - x);
		final double angle2 = Math.atan2(y2 - y, x2 - x);
//		final double angle1 = Math.atan2(y - y1, x - x1);
//		final double angle2 = Math.atan2(y - y2, x - x2);
		final double fx1 = x + Math.cos(angle1) * RAYCAST_DIST;
		final double fy1 = y + Math.sin(angle1) * RAYCAST_DIST;
		final double fx2 = x + Math.cos(angle2) * RAYCAST_DIST;
		final double fy2 = y + Math.sin(angle2) * RAYCAST_DIST;

		p.lineTo(fx2, fy2);
		// Ensure outer bound doesn't intersect vision shape by adding another point
		if (visionBounds.intersectsLine(fx1, fy1, fx2, fy2)) {
			double avgAngle = Math.atan2(Math.sin(angle2) - Math.sin(angle1), Math.cos(angle2) - Math.cos(angle1));
			final double medx = x + Math.cos(avgAngle) * RAYCAST_DIST;
			final double medy = y + Math.sin(avgAngle) * RAYCAST_DIST;

			p.lineTo(medx, medy);
		}
		p.lineTo(fx1, fy1);

	}

}
