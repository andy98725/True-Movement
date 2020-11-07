package helpers.geomNodes;

import java.util.PriorityQueue;

public class RecursionCase implements Comparable<RecursionCase> {

	private final double dist;
	private final CurveNode node;
	private final double time;

	public RecursionCase(double distLeft, CurveNode n, double t) {
		this.dist = distLeft;
		this.node = n;
		this.time = t;
	}

	@Override
	public int compareTo(RecursionCase o) {
		return (int) Math.signum(dist - o.dist);
	}

	public void propogate(PriorityQueue<RecursionCase> recurse) {
		node.addDistance(time, dist, recurse);
	}
}
