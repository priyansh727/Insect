import javax.swing.*;

/** Entry point for Insect. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InsectApp().setVisible(true));
    }
}
