package helpers;

import java.awt.Shape;

public interface CustomCurve extends Shape {

	public double[] getTangentPoints(double x, double y);

	public double[] getTangentLines(CustomCurve other);

	public double[][] getTangentTimes(CustomCurve other);

	public double getClosestTime(double x, double y);

	public double getX1();

	public double getY1();

	public double getX2();

	public double getY2();

	public double getCX1();

	public double getCY1();

	public double getCX2();

	public double getCY2();

	public boolean intersectsLine(double x1, double y1, double x2, double y2);

	public boolean isConvex();
}
