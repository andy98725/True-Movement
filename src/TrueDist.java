import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Collections;

import helpers.Util;

public class TrueDist {

	private static final double EXTEND = 2;

	public static Area getMovement(double x, double y, Area obs, double rad, double thickness) {
		if (rad <= 0 || obs.contains(x, y))
			return new Area();

		final Area extendedObs = new Area(Util.extendArea(obs, thickness / 2 + EXTEND));
		extendedObs.add(obs);
		if (extendedObs.contains(x, y)) {
			return new Area();
		}

		obs.add(new Area(Util.extendArea(obs, thickness / 2)));
		final Area flattenedObs = Util.createFlattenedArea(obs, 2);

		ArrayList<MovementNode> nodes = generateNodes(x, y, extendedObs, rad);
		MovementNode base = new MovementNode(x, y, rad);
		nodes.add(base);

		connectNodes(nodes, flattenedObs);
		base.propogateMovement();

		// Remove any not reached
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).isEmpty()) {
				nodes.remove(i);
				i--;
			}
		}
		Collections.sort(nodes);

		Area ret = new Area();
		for (MovementNode n : nodes) {
			ret.add(n.getRaycast(flattenedObs));
		}
//		ret.add(base.getRaycast(obs));

//		ret.add(new Area(AreaFunctions.extendArea(ret, thickness)));
		return ret;

	}

	// Generate movement nodes for an area of movement calculator
	private static ArrayList<MovementNode> generateNodes(double x, double y, Area obs, double rad) {
		final double radSQ = rad * rad;
		ArrayList<MovementNode> nodes = new ArrayList<MovementNode>();
		MovementNode movetoNode = null, prevNode = null;
		double[] coords = new double[6];
		for (PathIterator iter = obs.getPathIterator(null, 1); !iter.isDone(); iter.next()) {
			final int oper = iter.currentSegment(coords);
			switch (oper) {
			case PathIterator.SEG_MOVETO:
				if ((coords[0] - x) * (coords[0] - x) + (coords[1] - y) * (coords[1] - y) < radSQ) {
					MovementNode node = new MovementNode(coords[0], coords[1]);
					nodes.add(node);

					prevNode = node;
					movetoNode = node;
				} else {
					prevNode = null;
					movetoNode = null;
				}
				break;
			case PathIterator.SEG_LINETO:
				if ((coords[0] - x) * (coords[0] - x) + (coords[1] - y) * (coords[1] - y) < radSQ) {
					MovementNode node = new MovementNode(coords[0], coords[1]);
					nodes.add(node);
					if (prevNode != null)
						node.addNode(prevNode);
					prevNode = node;
				} else {
					prevNode = null;
				}
				break;
			case PathIterator.SEG_CLOSE:
				if (prevNode != null && movetoNode != null)
					prevNode.addNode(movetoNode);

				break;

			}
		}

		return nodes;
	}

	// Link nodes that directly see each other
	private static void connectNodes(ArrayList<MovementNode> nodes, Area obs) {
		for (int i = 0; i < nodes.size() - 1; i++) {
			for (int j = i + 1; j < nodes.size(); j++) {
				if (nodes.get(i).sees(nodes.get(j), obs))
					nodes.get(i).addNode(nodes.get(j));
			}
		}
	}

}

class MovementNode implements Comparable<MovementNode> {
	private static final double EPSILON = 1E-2;
	private final double x, y;
	private double moveAbility = 0;
	private boolean propogated = false;

	private final ArrayList<MovementNode> linkedNodes = new ArrayList<MovementNode>();

	protected MovementNode(double x, double y) {
		this.x = x;
		this.y = y;
	}

	protected boolean isEmpty() {
		return moveAbility <= 0;
	}

	protected MovementNode(double x, double y, double initial) {
		this(x, y);
		moveAbility = initial;
	}

	protected boolean sees(MovementNode other, Area check) {
		final double angle = Math.atan2(other.y - y, other.x - x);
		double x1 = x + Math.cos(angle) * EPSILON;
		double y1 = y + Math.sin(angle) * EPSILON;
		double x2 = other.x - Math.cos(angle) * EPSILON;
		double y2 = other.y - Math.sin(angle) * EPSILON;
		return Util.lineIntersectsArea(x1, y1, x2, y2, check);
	}

	protected void addNode(MovementNode other) {
		if (other != null && !linkedNodes.contains(other)) {
			linkedNodes.add(other);
			other.linkedNodes.add(this);
		}
	}

	protected void propogateMovement() {
		if (isEmpty() || propogated)
			return;

		propogated = true;
		// Propogate true distance movement
		for (MovementNode o : linkedNodes) {
			o.moveAbility = Math.max(o.moveAbility, moveAbility - Math.hypot(o.x - x, o.y - y));
		}

		// Recurse from closest to farthest
		Collections.sort(linkedNodes);
		for (MovementNode o : linkedNodes) {
			if (o.isEmpty())
				break;
			o.propogateMovement();
		}
	}

	protected Area getRaycast(Area obs) {
//		double r = moveAbility / 10;
//		return new Area(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
//		return AreaFunctions
//				.createFlattenedArea(Shadowcaster.getRaycast(null, new Point2D.Double(x, y), obs, moveAbility), 2);
//		return AreaFunctions
//				.createFlattenedArea(Shadowcaster.getRaycast(null, new Point2D.Double(x, y), obs, moveAbility), 2);
		return Raycast.raycast(x, y, obs, moveAbility); // the correct one
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	@Override
	public int compareTo(MovementNode o) {
		return moveAbility < o.moveAbility ? 1 : (moveAbility < o.moveAbility ? 0 : -1);
	}

}
