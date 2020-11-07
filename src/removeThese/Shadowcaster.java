package removeThese;

import java.awt.geom.*;
import java.util.Collections;
import java.util.List;

/* Shadowcaster class.
 * Computes vision shape from point and obstacle area
 * Great thanks to 
 * https://stackoverflow.com/questions/23966914/how-to-do-2d-shadow-casting-in-java
 * for tutorial and some classes.
 * 
 */
public class Shadowcaster {

	final static double deltaRad = 0.0001, deltaNudge = 0.1;

	public static Area getRaycast(Point2D origin, Area obs, double rad) {
		// Determine relevant segments
//		final List<Line2D> segments = Shapes.computeRelevantLineSegments(obs, lineDetail, origin, rad);
		final List<Line2D> segments = Shapes.computeLineSegments(obs, lineDetail);

		// Add sight shape to segments
		final Area sight = new Area(new Ellipse2D.Double(origin.getX() - rad, origin.getY() - rad, rad * 2, rad * 2));
		segments.addAll(Shapes.computeLineSegments(sight, lineDetail));

		// Create rays pointing to each point along the area.
		final List<Line2D> rays = Lines.createRays(origin, segments);
		// Find where rays intersect shape at the closest point
		final List<Point2D> intersections = Lines.computeClosestIntersections(rays, segments);
		// Sort intersections circularly
		Collections.sort(intersections, Points.byAngleComparator(origin));

		Area ret = createAreaFromPoints(intersections);
//		ret.subtract(obs);
		ret.intersect(sight);
		return ret;

	}

	public static Area raycast(Area block, Point2D origin) {
		// Convert area into line segments
		List<Line2D> segments = Shapes.computeLineSegments(block, 1);
		// Create rays pointing to each point along the area.
		List<Line2D> rays = Lines.createRays(origin, segments);
		// Find where rays intersect shape at the closest point
		List<Point2D> intersections = Lines.computeClosestIntersections(rays, segments);
		// Sort intersections circularly
		Collections.sort(intersections, Points.byAngleComparator(origin));
		// Create area from points and return
		return createAreaFromPoints(intersections);

	}

	private static final double lineDetail = 1;

	// Take list of points and return an area from them
	private static Area createAreaFromPoints(List<Point2D> points) {
		Path2D visionShape = new Path2D.Double();
		for (int i = 0; i < points.size(); i++) {
			Point2D p = points.get(i);
			double x = p.getX();
			double y = p.getY();
			if (i == 0) {
				visionShape.moveTo(x, y);
			} else {
				visionShape.lineTo(x, y);
			}
		}
		visionShape.closePath();
		return new Area(visionShape);
	}

}
