
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import helpers.Util;

@SuppressWarnings("serial")
public class Main extends JPanel {
	private static final int SIZE = 768;

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
		mouseRad += 1 * dir * INCREMENT;
		repaint();
	}

	private static final boolean SIGHT_ENABLED = true;
	private static final boolean MOVE_ENABLED = false;
	private static final boolean MOVE_OUTER_ENABLED = false;

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
			Area sight = Raycast.raycast(mx + 0.5, my + 0.5, blockage, 2 * mouseRad);
			System.out.println("Sight in " + (System.nanoTime() - before) / 1e9 + " sec" + "(Obs complexity " + Util.areaComplexity(blockage) + ", Post complexity " + Util.areaComplexity(sight) + ")");
			g.setColor(Color.LIGHT_GRAY);
			g.fill(sight);
			g.setColor(Color.GRAY);
			g.draw(sight);

		}
		if (MOVE_ENABLED) {
			long before = System.nanoTime();
			Area move = TrueDist.getMovement(mx, my, new Area(blockage), mouseRad, 32);
//			PlacementArea move = Shadowcaster.getMovement(null, new Point2D.Double(mx, my), new Area(blockage),
//					mouseRad / Game.MeterToPixel, 32);
			Area outer = null;
			if (MOVE_OUTER_ENABLED) {
				outer = new Area(move);
				outer.add(new Area(Util.extendArea(outer, 16)));
			}
			System.out.println("Move in " + (System.nanoTime() - before) / 1e9 + " sec");
			System.out.println("Obs complexity of " + Util.areaComplexity(blockage) + "; Move complexity of "
					+ Util.areaComplexity(move));
			if (MOVE_OUTER_ENABLED) {
				g.setColor(Color.BLUE);
				g.fill(outer);
				g.setColor(Color.BLUE);
				g.draw(outer);
			} else {
				g.setColor(Color.BLUE);
				g.draw(move);
			}
			g.setColor(Color.CYAN);
			g.fill(move);
		}

		// Mouse
		g.setColor(Color.BLACK);
		g.drawLine(mx, my, mx, my);

	}

	private static final int BOR = 32;

	private void generateBlocks() {
		Random r = new Random();

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

}
