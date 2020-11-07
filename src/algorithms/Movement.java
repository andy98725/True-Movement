package algorithms;
import java.awt.geom.Area;

import helpers.GeometryBucket;
import helpers.Util;

public class Movement {

	public static GeometryBucket getMovement(double x, double y, Area obs, double rad, double thickness) {
		if (rad <= 0)
//			return new Area();
			return new GeometryBucket();

		final double extend = thickness / 2 - 0.5;
		Area check = new Area(Util.extendArea(obs, extend));
		if (check.contains(x, y))
//			return new Area();
			return new GeometryBucket();

		return new GeometryBucket(obs, extend, x, y, rad);

	}
}
