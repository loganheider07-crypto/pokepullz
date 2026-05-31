import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class RouletteSim extends JPanel implements ActionListener {

    // wheel config
    private final int[] numbers = {
            0,32,15,19,4,21,2,25,17,34,6,27,13,36,11,30,8,23,10,
            5,24,16,33,1,20,14,31,9,22,18,29,7,28,12,35,3,26
    };

    private final double TWO_PI = Math.PI * 2;
    private final double arc = TWO_PI / numbers.length;

    // physics state
    private double wheelAngle = 0;
    private double wheelVel = 0;

    private double ballAngle = 0;
    private double ballRadius = 220;

    private double ballVelA = 0;
    private double ballVelR = 0;

    private boolean spinning = false;

    private int resultIndex = -1;

    private final Random rand = new Random();

    private final Timer timer;

    public RouletteSim() {
        setPreferredSize(new Dimension(600, 600));
        setBackground(new Color(10, 12, 18));

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    public void spin() {
        if (spinning) return;

        spinning = true;
        resultIndex = -1;

        wheelVel = 0.25 + rand.nextDouble() * 0.1;

        ballVelA = -(0.55 + rand.nextDouble() * 0.25);
        ballVelR = -(0.9 + rand.nextDouble() * 0.3);

        ballRadius = 220;
        ballAngle = 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updatePhysics();
        repaint();
    }

    private void updatePhysics() {
        if (!spinning) return;

        // friction constants
        double WHEEL_FRICTION = 0.992;
        double BALL_FRICTION = 0.988;
        double RADIAL_FRICTION = 0.985;

        // wheel motion
        wheelVel *= WHEEL_FRICTION;
        wheelAngle += wheelVel;

        // ball motion
        ballAngle += ballVelA;
        ballRadius += ballVelR;

        ballVelA *= BALL_FRICTION;
        ballVelR *= RADIAL_FRICTION;

        // boundaries (outer rim)
        if (ballRadius > 220) {
            ballRadius = 220;
            ballVelR *= -0.6;
            ballVelA *= 0.9;
        }

        // inner rim
        if (ballRadius < 70) {
            ballRadius = 70;
            ballVelR *= -0.4;
        }

        // frets (impact zones)
        double rel = (ballAngle - wheelAngle) % TWO_PI;
        if (rel < 0) rel += TWO_PI;

        double fretPos = rel % arc;
        double nearFret = Math.abs(fretPos - arc / 2);

        if (nearFret < 0.02 && rand.nextDouble() < 0.25) {
            ballVelA *= 0.6;
            ballVelR += (rand.nextDouble() - 0.5) * 0.15;
        }

        // stop condition
        double speed = Math.abs(ballVelA) + Math.abs(ballVelR);

        if (speed < 0.0012) {
            spinning = false;

            rel = (ballAngle - wheelAngle) % TWO_PI;
            if (rel < 0) rel += TWO_PI;

            resultIndex = (int)Math.round(rel / arc) % numbers.length;

            System.out.println("RESULT: " + numbers[resultIndex]);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = 300;
        int cy = 300;
        int R = 220;

        // draw wheel
        for (int i = 0; i < numbers.length; i++) {
            double a = wheelAngle + i * arc;

            int x1 = cx + (int)(Math.cos(a) * R);
            int y1 = cy + (int)(Math.sin(a) * R);

            int x2 = cx + (int)(Math.cos(a + arc) * R);
            int y2 = cy + (int)(Math.sin(a + arc) * R);

            g2.setColor((i % 2 == 0) ? Color.BLACK : Color.RED);
            if (numbers[i] == 0) g2.setColor(new Color(0, 160, 80));

            Polygon p = new Polygon();
            p.addPoint(cx, cy);
            p.addPoint(x1, y1);
            p.addPoint(x2, y2);

            g2.fillPolygon(p);
        }

        // draw ball
        int bx = cx + (int)(Math.cos(ballAngle) * ballRadius);
        int by = cy + (int)(Math.sin(ballAngle) * ballRadius);

        g2.setColor(Color.WHITE);
        g2.fillOval(bx - 6, by - 6, 12, 12);

        // result text
        g2.setColor(Color.WHITE);
        g2.drawString(
                spinning ? "Spinning..." :
                (resultIndex >= 0 ? "Result: " + numbers[resultIndex] : "Ready"),
                20, 20
        );
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Roulette Physics Simulator");
        RouletteSim sim = new RouletteSim();

        JButton button = new JButton("SPIN");
        button.addActionListener(e -> sim.spin());

        frame.setLayout(new BorderLayout());
        frame.add(sim, BorderLayout.CENTER);
        frame.add(button, BorderLayout.SOUTH);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
