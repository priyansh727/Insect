import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

/** Main window: one card to share this PC, one card to connect to another PC. */
public class InsectApp extends JFrame {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Preferences prefs = Preferences.userNodeForPackage(InsectApp.class);
    private final String address = String.join("  /  ", HostServer.localAddresses());

    // Share side
    private HostServer host;
    private String password = newPassword();
    private final Theme.GradientText passwordLabel = new Theme.GradientText(formatPassword(password), Theme.mono(34f));
    private final Theme.Button shareButton = new Theme.Button("Start sharing", Theme.Button.Kind.PRIMARY);
    private final Theme.Button copyButton = new Theme.Button("Copy details", Theme.Button.Kind.SECONDARY);
    private final Theme.Button newPasswordButton = new Theme.Button("New password", Theme.Button.Kind.SECONDARY);
    private final Theme.StatusLine hostStatus = new Theme.StatusLine("Not sharing");

    // Connect side
    private final JTextField ipField = Theme.field(prefs.get("lastIp", "127.0.0.1"));
    private final JTextField portField = Theme.field(String.valueOf(Protocol.DEFAULT_PORT));
    private final JPasswordField passwordField = Theme.passwordField();
    private final Theme.Button connectButton = new Theme.Button("Connect", Theme.Button.Kind.PRIMARY);
    private final Theme.StatusLine clientStatus = new Theme.StatusLine("Not connected");

    public InsectApp() {
        super("Insect");
        setIconImages(Theme.appIcons());

        Theme.Backdrop root = new Theme.Backdrop(new BorderLayout(0, 20));
        root.setBorder(new EmptyBorder(24, 28, 18, 28));
        root.add(header(), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 2, 20, 0));
        cards.setOpaque(false);
        Theme.Card share = shareCard();
        Theme.Card connect = connectCard();
        cards.add(share);
        cards.add(connect);
        root.add(cards, BorderLayout.CENTER);

        root.add(Theme.label("Insect v1.0   ·   Both computers must be on the same network",
                Theme.font(Font.PLAIN, 12f), Theme.MUTED), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setSize(getWidth() + 60, getHeight()); // slack so text never clips on scaled displays
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Intro: cards slide in one after the other, then the password "rolls" into place
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                share.reveal();
                connect.reveal();
                Theme.startAnimating(root);
                Timer intro = new Timer(500, ev -> rollPassword());
                intro.setRepeats(false);
                intro.start();
            }
        });
    }

    private JComponent header() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);
        header.add(new Theme.Logo(52), BorderLayout.WEST);

        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        text.add(new Theme.GradientText("Insect", Theme.font(Font.BOLD, 28f)));
        text.add(Theme.label("Share your screen. Control any PC. Built in Java.",
                Theme.font(Font.PLAIN, 14f), Theme.MUTED));
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    private Theme.Card shareCard() {
        Theme.Card card = new Theme.Card(0);
        card.item(Theme.label("Share this PC", Theme.font(Font.BOLD, 18f), Theme.TEXT));
        card.item(Theme.label("Let someone see and control this computer.", Theme.font(Font.PLAIN, 13f), Theme.MUTED));
        card.gap(22);
        card.item(Theme.caption("Your address"));
        card.item(Theme.label(address + "  :  " + Protocol.DEFAULT_PORT, Theme.mono(16f), Theme.TEXT));
        card.gap(16);
        card.item(Theme.caption("Password"));
        card.item(passwordLabel);
        card.gap(10);
        card.item(Theme.row(copyButton, newPasswordButton));
        card.fill();
        card.gap(22);
        card.wide(shareButton);
        card.gap(12);
        card.item(hostStatus);

        shareButton.addActionListener(e -> toggleSharing());
        newPasswordButton.addActionListener(e -> {
            password = newPassword();
            rollPassword();
        });
        copyButton.addActionListener(e -> copyDetails());
        return card;
    }

    private Theme.Card connectCard() {
        Theme.Card card = new Theme.Card(150);
        card.item(Theme.label("Connect to a PC", Theme.font(Font.BOLD, 18f), Theme.TEXT));
        card.item(Theme.label("View and control another computer.", Theme.font(Font.PLAIN, 13f), Theme.MUTED));
        card.gap(22);
        card.item(Theme.caption("Host IP address"));
        card.wide(ipField);
        card.gap(14);
        card.item(Theme.caption("Port"));
        card.wide(portField);
        card.gap(14);
        card.item(Theme.caption("Password"));
        card.wide(passwordField);
        card.fill();
        card.gap(22);
        card.wide(connectButton);
        card.gap(12);
        card.item(clientStatus);

        connectButton.addActionListener(e -> connect());
        ipField.addActionListener(e -> connect());
        passwordField.addActionListener(e -> connect());
        return card;
    }

    private void toggleSharing() {
        if (host != null) {
            host.stop();
            host = null;
            shareButton.setText("Start sharing");
            shareButton.setKind(Theme.Button.Kind.PRIMARY);
            newPasswordButton.setEnabled(true);
            hostStatus.set(Theme.MUTED, "Not sharing");
            return;
        }
        try {
            HostServer server = new HostServer(password,
                    (state, detail) -> SwingUtilities.invokeLater(() -> showHostStatus(state, detail)));
            server.start(Protocol.DEFAULT_PORT);
            host = server;
        } catch (AWTException | IOException e) {
            hostStatus.set(Theme.DANGER, "Could not start: " + e.getMessage());
            return;
        }
        shareButton.setText("Stop sharing");
        shareButton.setKind(Theme.Button.Kind.DANGER);
        newPasswordButton.setEnabled(false);
    }

    private void showHostStatus(HostServer.State state, String detail) {
        if (host == null) {
            return; // a late update from a server we already stopped
        }
        switch (state) {
            case WAITING -> hostStatus.set(Theme.WARN,
                    detail == null ? "Waiting for someone to connect..." : detail + ", waiting...");
            case CONNECTED -> hostStatus.set(Theme.ACCENT, "Connected to " + detail);
            case REJECTED -> hostStatus.set(Theme.DANGER, "Wrong password from " + detail);
            case ERROR -> hostStatus.set(Theme.DANGER, detail);
        }
    }

    private void copyDetails() {
        String details = "Insect  |  IP: " + address + "  |  Port: " + Protocol.DEFAULT_PORT + "  |  Password: " + password;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(details), null);
        copyButton.setText("Copied!");
        Timer reset = new Timer(1500, e -> copyButton.setText("Copy details"));
        reset.setRepeats(false);
        reset.start();
    }

    private void connect() {
        String ip = ipField.getText().trim();
        String pw = new String(passwordField.getPassword()).replace(" ", "");
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            clientStatus.set(Theme.DANGER, "Port must be a number");
            return;
        }
        if (ip.isEmpty()) {
            clientStatus.set(Theme.DANGER, "Enter the host's IP address");
            return;
        }

        connectButton.setEnabled(false);
        connectButton.setText("Connecting...");
        clientStatus.set(Theme.WARN, "Connecting to " + ip + "...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ClientViewer.connect(ip, port, pw,
                        () -> SwingUtilities.invokeLater(() -> clientStatus.set(Theme.MUTED, "Disconnected from " + ip)));
                return null;
            }

            @Override
            protected void done() {
                connectButton.setEnabled(true);
                connectButton.setText("Connect");
                try {
                    get();
                    clientStatus.set(Theme.ACCENT, "Connected to " + ip);
                    prefs.put("lastIp", ip);
                } catch (ExecutionException e) {
                    clientStatus.set(Theme.DANGER, friendlyError(e.getCause(), ip));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Slot-machine effect: digits spin, then lock in one by one from the left. */
    private void rollPassword() {
        String target = password;
        long start = System.currentTimeMillis();
        Timer roll = new Timer(40, null);
        roll.addActionListener(e -> {
            int locked = (int) ((System.currentTimeMillis() - start) / 110);
            StringBuilder shown = new StringBuilder();
            for (int i = 0; i < target.length(); i++) {
                shown.append(i < locked ? target.charAt(i) : (char) ('0' + RANDOM.nextInt(10)));
            }
            passwordLabel.setText(formatPassword(shown.toString()));
            if (locked >= target.length() || !target.equals(password)) {
                roll.stop();
            }
        });
        roll.start();
    }

    private static String friendlyError(Throwable error, String ip) {
        if (error instanceof ConnectException || error instanceof SocketTimeoutException) {
            return "Can't reach " + ip + ". Is it sharing?";
        }
        if (error instanceof UnknownHostException) {
            return "Unknown address: " + ip;
        }
        return error.getMessage();
    }

    private static String newPassword() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static String formatPassword(String pw) {
        return pw.substring(0, 3) + " " + pw.substring(3);
    }
}
