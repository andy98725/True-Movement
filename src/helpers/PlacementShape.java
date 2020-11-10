package helpers;

import java.awt.geom.Area;
import java.util.LinkedList;
import java.util.Queue;

public class PlacementShape {
	private final double extend;
	private Area inner, outer;

	public PlacementShape(Area inner, double ext, boolean eagerGen) {
		this.extend = ext;
		this.inner = inner;
		if (eagerGen) {
			eagerGen(this);
		}
	}

	public PlacementShape(Area outer, double ext) {
		this.extend = ext;
		this.outer = outer;
		// Outer always eager gens inner
		eagerGen(this);
	}

	public synchronized Area getInner() {
		if (inner == null) {
			inner = new Area(outer);
			inner.subtract(new Area(Util.extendArea(outer, extend)));
		}
		return inner;
	}

	public synchronized Area getOuter() {
		if (outer == null) {
			outer = new Area(inner);
			outer.add(new Area(Util.extendArea(inner, extend)));
		}
		return outer;

	}

	// Load in shape eagerly (on separate thread)
	private static void eagerGen(PlacementShape shape) {
		synchronized (shapeGens) {
			boolean shouldGen = shapeGens.isEmpty();
			shapeGens.add(shape);

			if (shouldGen) {
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						generateAll();
					}
				}, "Placement Generation");
				t.setDaemon(true);
				t.start();
			}

		}
	}

	// Generations to load
	private static final Queue<PlacementShape> shapeGens = new LinkedList<PlacementShape>();

	private static void generateAll() {
		PlacementShape gen;
		boolean cont;

		do {
			// Get next from synchronized list
			synchronized (shapeGens) {
				gen = shapeGens.poll();
				cont = !shapeGens.isEmpty();
			}

			if (gen != null) {
				gen.getInner();
				gen.getOuter();
			}
		} while (cont);
	}

}
