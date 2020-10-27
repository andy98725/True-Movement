package helpers;

public interface CustomCurve {

	public double[] getTangentPoints(double x, double y);

	public double[] getTangentLines(CustomCurve other);

	public double[][] getTangentTimes(CustomCurve other);
}
