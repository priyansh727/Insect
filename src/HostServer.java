import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The machine whose screen is shared. Captures the screen, streams it as JPEG
 * frames, and replays mouse/keyboard events received from the client.
 */
public class HostServer {
    private static final int FPS = 15;
    private static final float JPEG_QUALITY = 0.6f;
    private static final int MAX_WIDTH = 1280; // frames are downscaled to save bandwidth

    private final int port;
    private final String password;
    private final Robot robot;
    private final Rectangle screen;
    private final JLabel status = new JLabel("Waiting for a client...");

    public HostServer(int port, String password) throws AWTException {
        this.port = port;
        this.password = password;
        this.robot = new Robot();
        this.screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    }

    public void start() {
        showWindow();
        new Thread(this::acceptLoop, "accept").start();
    }

    private void showWindow() {
        JFrame frame = new JFrame("ScreenBridge - Host");
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        panel.add(new JLabel("Share these details with the person connecting:"));
        panel.add(new JLabel("IP address: " + String.join(", ", localAddresses())));
        panel.add(new JLabel("Port: " + port));
        JLabel pw = new JLabel("Password: " + password);
        pw.setFont(pw.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(pw);
        panel.add(status);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> status.setText(text));
    }

    /** Accepts one client at a time; when it leaves, waits for the next one. */
    private void acceptLoop() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                try (Socket socket = server.accept()) {
                    socket.setTcpNoDelay(true);
                    handleClient(socket);
                } catch (IOException e) {
                    // client dropped; fall through and wait for the next one
                }
                setStatus("Client disconnected. Waiting for a client...");
            }
        } catch (IOException e) {
            setStatus("Error: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) throws IOException {
        String who = socket.getInetAddress().getHostAddress();
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        if (!password.equals(in.readUTF())) {
            out.writeBoolean(false);
            out.flush();
            setStatus("Rejected wrong password from " + who);
            return;
        }
        out.writeBoolean(true);
        out.writeInt(screen.width);
        out.writeInt(screen.height);
        out.flush();
        setStatus("Connected: " + who);

        new Thread(() -> sendFrames(socket, out), "frames").start();
        receiveInput(in); // blocks until the client disconnects
    }

    private void sendFrames(Socket socket, DataOutputStream out) {
        long frameMs = 1000 / FPS;
        try {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            while (!socket.isClosed()) {
                long start = System.currentTimeMillis();
                buffer.reset();
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(buffer)) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(captureFrame(), null, null), param);
                }
                out.writeInt(buffer.size());
                buffer.writeTo(out);
                out.flush();

                long sleep = frameMs - (System.currentTimeMillis() - start);
                if (sleep > 0) {
                    Thread.sleep(sleep);
                }
            }
        } catch (IOException | InterruptedException e) {
            try {
                socket.close(); // unblocks receiveInput so the host can accept again
            } catch (IOException ignored) {
            }
        }
    }

    private BufferedImage captureFrame() {
        BufferedImage shot = robot.createScreenCapture(screen);
        int w = Math.min(screen.width, MAX_WIDTH);
        int h = screen.height * w / screen.width;
        BufferedImage frame = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(shot, 0, 0, w, h, null);

        // Robot screenshots don't include the mouse pointer, so draw one ourselves
        PointerInfo pointer = MouseInfo.getPointerInfo();
        if (pointer != null) {
            Point p = pointer.getLocation();
            drawCursor(g, p.x * w / screen.width, p.y * h / screen.height);
        }
        g.dispose();
        return frame;
    }

    private static void drawCursor(Graphics2D g, int x, int y) {
        Polygon arrow = new Polygon(
                new int[]{x, x, x + 4, x + 7, x + 9, x + 6, x + 11},
                new int[]{y, y + 16, y + 12, y + 18, y + 17, y + 11, y + 11}, 7);
        g.setColor(Color.WHITE);
        g.fill(arrow);
        g.setColor(Color.BLACK);
        g.draw(arrow);
    }

    private void receiveInput(DataInputStream in) throws IOException {
        while (true) {
            int type = in.readInt();
            try {
                switch (type) {
                    case Protocol.MOUSE_MOVE -> moveMouse(in.readInt(), in.readInt());
                    case Protocol.MOUSE_PRESS -> robot.mousePress(in.readInt());
                    case Protocol.MOUSE_RELEASE -> robot.mouseRelease(in.readInt());
                    case Protocol.MOUSE_WHEEL -> robot.mouseWheel(in.readInt());
                    case Protocol.KEY_PRESS -> robot.keyPress(in.readInt());
                    case Protocol.KEY_RELEASE -> robot.keyRelease(in.readInt());
                    default -> throw new IOException("Unknown event type " + type);
                }
            } catch (IllegalArgumentException e) {
                // key or button that doesn't exist on this machine; skip it
            }
        }
    }

    /**
     * Robot.mouseMove can land in the wrong spot when Windows display scaling
     * is above 100%, so retry a few times until the pointer is where we asked.
     */
    private void moveMouse(int x, int y) {
        for (int attempt = 0; attempt < 5; attempt++) {
            robot.mouseMove(x, y);
            PointerInfo pointer = MouseInfo.getPointerInfo();
            if (pointer == null || pointer.getLocation().equals(new Point(x, y))) {
                return;
            }
        }
    }

    private static java.util.List<String> localAddresses() {
        java.util.List<String> list = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(ni.getInetAddresses())) {
                    if (address instanceof Inet4Address) {
                        list.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        if (list.isEmpty()) {
            list.add("127.0.0.1");
        }
        return list;
    }
}
