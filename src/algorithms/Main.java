package algorithms;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import helpers.PlacementShape;
import helpers.Util;

@SuppressWarnings("serial")
public class Main extends JPanel {
	private static final int SIZE = 768;

	private static final boolean SIGHT_ENABLED = true;
	private static final boolean DISP_SIGHT = true;
	private static final boolean MOVE_ENABLED = true;
	private static final boolean DISP_MOVE = true;
	private static final boolean MOVE_OUTER_ENABLED = true;
	private static final double MOVE_RAD = 16;

	public static void main(String[] args) {
		JFrame app = new JFrame();
		app.setTitle("My app");
		app.setSize(SIZE, SIZE);
		app.setResizable(false);
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		app.add(new Main());

		// Center on screen
		app.setLocationRelativeTo(null);
		app.setVisible(true);
	}

	private static final int INCREMENT = 16;

	private Area blockage;
	private int mx, my;
	private int mouseRad = (int) (12 * INCREMENT);

	private Main() {
		generateBlocks();
		addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				generateBlocks();
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}
		});
		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				mouseInput(e.getX(), e.getY());
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				mouseInput(e.getX(), e.getY());
			}
		});
		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				mouseWheel(e.getWheelRotation());
			}
		});
	}

	private void mouseInput(int x, int y) {
		if (!blockage.contains(x, y)) {
			mx = x;
			my = y;
			repaint();
		}
	}

	private void mouseWheel(int dir) {
		mouseRad -= 1 * dir * INCREMENT;
		repaint();
	}


	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, SIZE, SIZE);

		// Terrain
		g.setColor(Color.GRAY);
		g.fill(blockage);
		g.setColor(Color.DARK_GRAY);
		g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(blockage);

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		if (SIGHT_ENABLED) {
			long before = System.nanoTime();
			Area sight = new Raycast(mx + 0.5, my + 0.5, blockage, 2 * mouseRad).get();
			if (DISP_SIGHT)
				System.out.println("Sight in " + (System.nanoTime() - before) / 1e9 + " sec" + "(Obs complexity "
						+ Util.areaComplexity(blockage) + ", Post complexity " + Util.areaComplexity(sight) + ")");
			g.setColor(Color.LIGHT_GRAY);
			g.fill(sight);
			g.setColor(Color.GRAY);
			g.draw(sight);

		}
		if (MOVE_ENABLED) {
			long before = System.nanoTime();
			Raycast.ON_MOVE = true; //TODO delete this
			Movement b = new Movement(new Area(blockage), mx, my, mouseRad, MOVE_RAD * 2);
			PlacementShape shape = new PlacementShape(b.getResult(), MOVE_RAD, true);

			if (DISP_MOVE) {
				System.out.println("Move in " + (System.nanoTime() - before) / 1e9 + " sec");
				System.out.println("Obs complexity of " + Util.areaComplexity(blockage) + "; Move complexity of "
						+ Util.areaComplexity(shape.getInner()));
			}
//			b.drawNodes(g);

			if (MOVE_OUTER_ENABLED) {
				before = System.nanoTime();
				shape.getOuter();
				if (DISP_MOVE) {
					System.out.println("Move extended in " + (System.nanoTime() - before) / 1e9 + " sec");
					System.out.println("Complexity of " + Util.areaComplexity(shape.getOuter()));
				}
			}
			if (MOVE_OUTER_ENABLED) {
				g.setColor(Color.BLUE);
				g.fill(shape.getOuter());
				g.setColor(Color.CYAN);
				g.fill(shape.getInner());
				g.setColor(Color.BLUE);
				g.draw(shape.getOuter());
			} else {
				g.setColor(Color.CYAN);
				g.fill(shape.getInner());
				g.setColor(Color.BLUE);
				g.draw(shape.getInner());
			}

		}

		// Mouse
		g.setColor(Color.BLACK);
		g.drawLine(mx, my, mx, my);

	}

	private static final int BOR = 32;

	private void generateBlocks() {
//		generateBlocksStatic();
		generateBlocksRandom();

//		blockage.add(new Area(Util.extendArea(blockage, 16)));
//		blockage = new Area(Util.extendArea(blockage, 16));
	}

	@SuppressWarnings("unused")
	private void generateBlocksRandom() {
		Random r = new Random(5);

		blockage = new Area();

		final int circleCount = 5 + r.nextInt(5);
		for (int i = 0; i < circleCount; i++) {
			int rad = 16 + r.nextInt(128);
			int x = BOR + r.nextInt(SIZE - 2 * BOR);
			int y = BOR + r.nextInt(SIZE - 2 * BOR);
			blockage.add(new Area(new Ellipse2D.Double(x - rad, y - rad, 2 * rad, 2 * rad)));
		}
		final int subCount = 2 + r.nextInt(2);
		for (int i = 0; i < subCount; i++) {
			int rad = 128 + r.nextInt(64);
			int x = BOR + r.nextInt(SIZE - 2 * BOR);
			int y = BOR + r.nextInt(SIZE - 2 * BOR);
			blockage.subtract(new Area(new Ellipse2D.Double(x - rad, y - rad, 2 * rad, 2 * rad)));
		}
		final int squareCount = 4 + r.nextInt(5);
		for (int i = 0; i < squareCount; i++) {
			int w = 16 + r.nextInt(128);
			int h = 16 + r.nextInt(128);
			int x = BOR + r.nextInt(SIZE - 2 * BOR);
			int y = BOR + r.nextInt(SIZE - 2 * BOR);
			blockage.add(new Area(new Rectangle(x - w / 2, y - h / 2, w, h)));
		}

	}

	@SuppressWarnings("unused")
	private void generateBlocksStatic() {
		blockage = new Area();
		int x = SIZE / 2, y = SIZE / 2;
		int w = SIZE / 2, h = SIZE / 2;
		blockage.add(new Area(new Rectangle(x - w / 2, y - h / 2, w, h)));
//		blockage.add(new Area(new Ellipse2D.Double(x - w / 2, y - h / 2, w, h)));

		x *= 1.5;
		y *= 1.5;
//		blockage.subtract(new Area(new Ellipse2D.Double(x - w / 2, y - h / 2, w, h)));
	}

}
