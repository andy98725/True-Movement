import java.awt.geom.Area;

import helpers.GeometryBucket;
import helpers.Util;

public class Movement {

	public static GeometryBucket getMovement(double x, double y, Area obs, double rad, double thickness) {
		if (rad <= 0)
//			return new Area();
			return new GeometryBucket();

		obs = new Area(obs);
		obs.add(new Area(Util.extendArea(obs, thickness / 2 - 0.5)));
		if (obs.contains(x, y))
//			return new Area();
			return new GeometryBucket();

		// Convert into geometry primitives
		GeometryBucket bucket = new GeometryBucket(obs);

		return bucket;

	}
}
