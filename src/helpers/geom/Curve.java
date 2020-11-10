package helpers.geom;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;

public interface Curve extends Shape {

	public double[] getTangentPoints(double x, double y);

	public double[] getTangentLines(Curve other);

	public double[] getTangentTimes(double x, double y);

	public double[][] getTangentTimes(Curve other);

	public double getClosestTime(double x, double y);

	public double getX1();

	public double getY1();

	public Point2D getP1();

	public double getX2();

	public double getY2();

	public Point2D getP2();

	public double getCX1();

	public double getCY1();

	public double getCX2();

	public double getCY2();

	public boolean intersectsLine(double x1, double y1, double x2, double y2);

	public boolean isConvex();

	public double distanceAlongCurve(double t1, double t2);

	public Point2D eval(double t);

	public Area getProjection(double t, double r);

	public Point2D getRaycastPoint(double t);

}
