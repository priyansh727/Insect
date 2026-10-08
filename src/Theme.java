import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** Colors, fonts and the custom-painted widgets that give Insect its dark look. */
final class Theme {
    static final Color BG = new Color(0x0F1115);
    static final Color CARD = new Color(0x181B22);
    static final Color BORDER = new Color(0x2A2F3A);
    static final Color FIELD = new Color(0x11141A);
    static final Color TEXT = new Color(0xE6E8EC);
    static final Color MUTED = new Color(0x8B93A1);
    static final Color ACCENT = new Color(0x3DDC84);
    static final Color ACCENT_DARK = new Color(0x2FB56B);
    static final Color DANGER = new Color(0xFF5C5C);
    static final Color WARN = new Color(0xFFB547);
    private static final Color LOGO_BG = new Color(0x16241C);

    private Theme() {
    }

    static Font font(int style, float size) {
        return new Font("Segoe UI", style, 1).deriveFont(size);
    }

    static Font mono(float size) {
        return new Font("Consolas", Font.BOLD, 1).deriveFont(size);
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /** Small uppercase heading shown above a value or input. */
    static JLabel caption(String text) {
        JLabel label = label(text.toUpperCase(), font(Font.BOLD, 11f), MUTED);
        label.setBorder(new EmptyBorder(0, 0, 6, 0));
        return label;
    }

    static JTextField field(String text) {
        JTextField field = new JTextField(text);
        styleField(field);
        return field;
    }

    static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        field.setEchoChar('•');
        styleField(field);
        return field;
    }

    private static void styleField(JTextField field) {
        field.setBackground(FIELD);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setSelectionColor(ACCENT_DARK);
        field.setSelectedTextColor(BG);
        field.setFont(font(Font.PLAIN, 15f));
        field.setColumns(16);
        field.setBorder(fieldBorder(BORDER));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(fieldBorder(ACCENT));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(fieldBorder(BORDER));
            }
        });
    }

    private static CompoundBorder fieldBorder(Color color) {
        return new CompoundBorder(new LineBorder(color, 1, true), new EmptyBorder(8, 10, 8, 10));
    }

    /** Lays buttons out side by side with a small gap. */
    static JPanel row(JComponent... items) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                row.add(Box.createHorizontalStrut(8));
            }
            row.add(items[i]);
        }
        row.setMaximumSize(row.getPreferredSize());
        return row;
    }

    static List<Image> appIcons() {
        return List.of(logoImage(16), logoImage(32), logoImage(48), logoImage(64), logoImage(128));
    }

    static BufferedImage logoImage(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        paintLogo(g, size);
        g.dispose();
        return image;
    }

    /** Draws the Insect logo (a bug seen from above) in a size x size square. */
    static void paintLogo(Graphics2D g0, int size) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 100.0, size / 100.0);

        g.setColor(LOGO_BG);
        g.fill(new RoundRectangle2D.Double(0, 0, 100, 100, 26, 26));

        g.setColor(ACCENT);
        g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double[][] legEnds = {{22, 32}, {17, 52}, {22, 74}};
        for (double[] end : legEnds) {
            g.draw(new Line2D.Double(50, 50, end[0], end[1]));
            g.draw(new Line2D.Double(50, 50, 100 - end[0], end[1]));
        }
        g.draw(new QuadCurve2D.Double(46, 26, 40, 13, 29, 12));
        g.draw(new QuadCurve2D.Double(54, 26, 60, 13, 71, 12));

        g.fill(new Ellipse2D.Double(42, 21, 16, 14)); // head
        g.fill(new Ellipse2D.Double(39, 35, 22, 20)); // thorax
        g.fill(new Ellipse2D.Double(36, 51, 28, 37)); // abdomen

        g.setColor(LOGO_BG);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(50, 56, 50, 83)); // wing split
        g.dispose();
    }

    static class Logo extends JComponent {
        private final int size;

        Logo(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintLogo((Graphics2D) g, size);
        }
    }

    /** Rounded dark panel that stacks its children top to bottom. */
    static class Card extends JPanel {
        Card() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(new EmptyBorder(22, 22, 22, 22));
        }

        void item(JComponent c) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            add(c);
        }

        /** Adds a component stretched to the card's full width. */
        void wide(JComponent c) {
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            item(c);
        }

        void gap(int height) {
            item((JComponent) Box.createVerticalStrut(height));
        }

        /** Pushes everything added after it to the bottom of the card. */
        void fill() {
            item((JComponent) Box.createVerticalGlue());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(CARD);
            g2.fill(shape);
            g2.setColor(BORDER);
            g2.draw(shape);
            g2.dispose();
        }
    }

    /** Flat rounded button that lightens on hover. */
    static class Button extends JButton {
        private Color background;
        private boolean hovered;

        Button(String text, Color background, Color foreground) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setFont(font(Font.BOLD, 14f));
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setStyle(background, foreground);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        void setStyle(Color background, Color foreground) {
            this.background = background;
            setForeground(foreground);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(!isEnabled() ? BORDER : hovered ? background.brighter() : background);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A colored dot followed by a short status message. */
    static class StatusLine extends JPanel {
        private final JLabel text = label("", font(Font.PLAIN, 13f), MUTED);
        private Color dot = MUTED;

        StatusLine(String initial) {
            super(new BorderLayout());
            setOpaque(false);
            text.setBorder(new EmptyBorder(0, 16, 0, 0));
            text.setText(initial);
            add(text);
        }

        void set(Color color, String message) {
            dot = color;
            text.setText(message);
            text.setForeground(color == MUTED ? MUTED : TEXT);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(dot);
            g2.fillOval(0, getHeight() / 2 - 4, 8, 8);
            g2.dispose();
        }
    }
}
