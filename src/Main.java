import javax.swing.*;
import java.awt.*;
import java.security.SecureRandom;

/** Entry point: asks whether to share this screen (host) or control another PC (client). */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::showLauncher);
    }

    private static void showLauncher() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        String[] options = {"Share my screen (Host)", "Connect to a PC (Client)"};
        int choice = JOptionPane.showOptionDialog(null,
                "What do you want to do?", "ScreenBridge",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            startHost();
        } else if (choice == 1) {
            startClient();
        }
    }

    private static void startHost() {
        String password = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        try {
            new HostServer(Protocol.DEFAULT_PORT, password).start();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(null, "Screen capture is not supported here: " + e.getMessage());
        }
    }

    private static void startClient() {
        JTextField ipField = new JTextField("127.0.0.1");
        JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT));
        JPasswordField passwordField = new JPasswordField();

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Host IP:"));
        form.add(ipField);
        form.add(new JLabel("Port:"));
        form.add(portField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, form, "Connect to a PC",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                int port = Integer.parseInt(portField.getText().trim());
                ClientViewer.connect(ipField.getText().trim(), port, new String(passwordField.getPassword()));
                return;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Port must be a number.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Could not connect: " + e.getMessage());
            }
        }
    }
}
