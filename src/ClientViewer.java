import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * The machine that views and controls the remote screen. Shows incoming
 * frames in a window and forwards local mouse/keyboard events to the host.
 */
public class ClientViewer extends JPanel {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final int remoteWidth;
    private final int remoteHeight;

    private final Runnable onClosed;

    private volatile BufferedImage frame;
    private final Timer spinner = new Timer(30, e -> repaint()); // animates the loading ring
    private JFrame window;
    private int framesThisSecond;
    private long secondStart = System.currentTimeMillis();

    /**
     * Connects and logs in; throws if the host is unreachable or the password is wrong.
     * {@code onClosed} runs once the viewer window closes or the connection drops.
     */
    public static void connect(String host, int port, String password, Runnable onClosed) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setTcpNoDelay(true);
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        out.writeUTF(password);
        out.flush();
        if (!in.readBoolean()) {
            socket.close();
            throw new IOException("Wrong password");
        }
        ClientViewer viewer = new ClientViewer(socket, in, out, in.readInt(), in.readInt(), onClosed);
        SwingUtilities.invokeLater(viewer::showWindow);
        new Thread(viewer::receiveFrames, "frames").start();
    }

    private ClientViewer(Socket socket, DataInputStream in, DataOutputStream out,
                         int remoteWidth, int remoteHeight, Runnable onClosed) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.remoteWidth = remoteWidth;
        this.remoteHeight = remoteHeight;
        this.onClosed = onClosed;
        setBackground(Theme.BG);
        setPreferredSize(new Dimension(1280, 1280 * remoteHeight / remoteWidth));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false); // let Tab go to the remote machine
        installInputListeners();
        spinner.start();
    }

    private void showWindow() {
        window = new JFrame(title(-1));
        window.setIconImages(Theme.appIcons());
        window.add(this);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                spinner.stop();
                closeQuietly();
                onClosed.run();
            }
        });
        window.setVisible(true);
        requestFocusInWindow();
    }

    private void receiveFrames() {
        try {
            while (true) {
                byte[] data = new byte[in.readInt()];
                in.readFully(data);
                frame = ImageIO.read(new ByteArrayInputStream(data));
                updateFps();
                repaint();
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                if (window != null && window.isDisplayable()) {
                    JOptionPane.showMessageDialog(window, "Disconnected from host.");
                    window.dispose();
                }
            });
        }
    }

    private void updateFps() {
        framesThisSecond++;
        long now = System.currentTimeMillis();
        if (now - secondStart >= 1000) {
            int fps = framesThisSecond;
            framesThisSecond = 0;
            secondStart = now;
            SwingUtilities.invokeLater(() -> {
                if (window != null) {
                    window.setTitle(title(fps));
                }
            });
        }
    }

    private String title(int fps) {
        String title = "Insect - " + socket.getInetAddress().getHostAddress();
        return fps < 0 ? title : title + " - " + fps + " FPS";
    }

    /** Where the remote screen is drawn inside this panel (scaled to fit, aspect ratio kept). */
    private Rectangle viewRect() {
        double scale = Math.min((double) getWidth() / remoteWidth, (double) getHeight() / remoteHeight);
        int w = (int) (remoteWidth * scale);
        int h = (int) (remoteHeight * scale);
        return new Rectangle((getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage current = frame;
        if (current == null) {
            paintLoading((Graphics2D) g);
            return;
        }
        spinner.stop();
        Rectangle r = viewRect();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(current, r.x, r.y, r.width, r.height, null);
    }

    /** Spinning gradient ring shown until the first frame arrives. */
    private void paintLoading(Graphics2D g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int cx = getWidth() / 2;
        int cy = getHeight() / 2 - 16;
        int angle = (int) (Theme.seconds() * 360) % 360;
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255, 25));
        g.drawOval(cx - 22, cy - 22, 44, 44);
        g.setPaint(new GradientPaint(cx - 22, cy - 22, Theme.ACCENT, cx + 22, cy + 22, Theme.TEAL));
        g.drawArc(cx - 22, cy - 22, 44, 44, -angle, 100);
        g.setFont(Theme.font(Font.PLAIN, 14f));
        g.setColor(Theme.MUTED);
        String text = "Connecting to the screen...";
        g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, cy + 52);
        g.dispose();
    }

    private void installInputListeners() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouseMove(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sendMouseMove(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                send(Protocol.MOUSE_PRESS, InputEvent.getMaskForButton(e.getButton()));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                send(Protocol.MOUSE_RELEASE, InputEvent.getMaskForButton(e.getButton()));
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                send(Protocol.MOUSE_WHEEL, e.getWheelRotation());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_UNDEFINED) {
                    send(Protocol.KEY_PRESS, e.getKeyCode());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_UNDEFINED) {
                    send(Protocol.KEY_RELEASE, e.getKeyCode());
                }
            }
        });
    }

    private void sendMouseMove(MouseEvent e) {
        Rectangle r = viewRect();
        if (r.width == 0 || r.height == 0 || !r.contains(e.getPoint())) {
            return;
        }
        int x = (e.getX() - r.x) * remoteWidth / r.width;
        int y = (e.getY() - r.y) * remoteHeight / r.height;
        send(Protocol.MOUSE_MOVE, x, y);
    }

    private synchronized void send(int type, int... args) {
        try {
            out.writeInt(type);
            for (int arg : args) {
                out.writeInt(arg);
            }
            out.flush();
        } catch (IOException e) {
            closeQuietly();
        }
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
