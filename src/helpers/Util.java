package helpers;

import java.awt.*;
import java.awt.geom.*;

public class Util {

	public static Shape extendArea(Shape a, double dist) {
		Stroke extendStroke = new BasicStroke(-1 + 2 * (float) dist, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
		return extendStroke.createStrokedShape(a);
	}

	public static Area createFlattenedArea(Area a, double flatness) {
		Path2D path = new Path2D.Double();
		path.append(new FlatteningPathIterator(a.getPathIterator(null), flatness), true);
		return new Area(path);
	}

	public static boolean lineIntersectsArea(double x1, double y1, double x2, double y2, Area a) {
		final double[] coords = new double[6];
		for (PathIterator pi = a.getPathIterator(null, 1); !pi.isDone(); pi.next()) {
			final int oper = pi.currentSegment(coords);
			switch (oper) {
			case PathIterator.SEG_MOVETO:
				coords[4] = coords[0];
				coords[5] = coords[1];
				coords[2] = coords[0];
				coords[3] = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				coords[0] = coords[4];
				coords[1] = coords[5];
			case PathIterator.SEG_LINETO:
				if (Line2D.linesIntersect(x1, y1, x2, y2, coords[0], coords[1], coords[2], coords[3])) {
					return true;
				}
				coords[2] = coords[0];
				coords[3] = coords[1];
				break;
			}
		}
		return false;
	}

	public static double orientation(double x0, double y0, double x1, double y1, double x2, double y2) {
		return Math.signum((y1 - y0) * (x2 - x1) - (y2 - y1) * (x1 - x0));
	}

	public static int areaComplexity(Area a) {
		int size = 0;
		PathIterator i = a.getPathIterator(null);
		while (!i.isDone()) {
			size++;
			i.next();
		}
		return size;
	}

}
