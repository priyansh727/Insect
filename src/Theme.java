import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Colors, fonts and the custom-painted, animated widgets that give Insect its look.
 * Animations are time-based: a single timer repaints the window ~30 times a second
 * and each widget works out its own state from the current time.
 */
final class Theme {
    static final Color BG = new Color(0x0B0D12);
    static final Color CARD = new Color(22, 25, 32, 215);
    static final Color BORDER = new Color(0x2A2F3A);
    static final Color FIELD = new Color(10, 12, 16, 200);
    static final Color TEXT = new Color(0xE6E8EC);
    static final Color MUTED = new Color(0x8B93A1);
    static final Color ACCENT = new Color(0x3DDC84);
    static final Color TEAL = new Color(0x22D3EE);
    static final Color VIOLET = new Color(0x8B5CF6);
    static final Color DANGER = new Color(0xFF5C5C);
    static final Color PINK = new Color(0xFF7AB6);
    static final Color WARN = new Color(0xFFB547);
    private static final Color LOGO_BG = new Color(0x16241C);

    private Theme() {
    }

    // ---------------------------------------------------------------- helpers

    static Font font(int style, float size) {
        return new Font("Segoe UI", style, 1).deriveFont(size);
    }

    static Font mono(float size) {
        return new Font("Consolas", Font.BOLD, 1).deriveFont(size);
    }

    static double seconds() {
        return System.nanoTime() / 1e9;
    }

    /** Ease-out cubic: fast start, gentle stop. Input is clamped to 0..1. */
    static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (float) Math.pow(1f - t, 3);
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mix(Color a, Color b, float t) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /** Repaints {@code root} about 30 times a second so every widget inside it animates. */
    static void startAnimating(JComponent root) {
        new Timer(33, e -> root.repaint()).start();
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

    // ----------------------------------------------------------------- fields

    static JTextField field(String text) {
        JTextField field = new JTextField(text) {
            @Override
            protected void paintComponent(Graphics g) {
                paintFieldBackground(this, g);
                super.paintComponent(g);
            }
        };
        styleField(field);
        return field;
    }

    static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                paintFieldBackground(this, g);
                super.paintComponent(g);
            }
        };
        field.setEchoChar('•');
        styleField(field);
        return field;
    }

    private static void styleField(JTextField field) {
        field.setOpaque(false);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setSelectionColor(alpha(ACCENT, 120));
        field.setSelectedTextColor(Color.WHITE);
        field.setFont(font(Font.PLAIN, 15f));
        field.setColumns(16);
        field.setBorder(new GlowBorder());
    }

    private static void paintFieldBackground(JComponent c, Graphics g) {
        Graphics2D g2 = smooth(g);
        g2.setColor(FIELD);
        g2.fill(new RoundRectangle2D.Float(0, 0, c.getWidth(), c.getHeight(), 12, 12));
        g2.dispose();
    }

    /** Rounded field outline that fades to a green glow while the field has focus. */
    private static class GlowBorder implements Border {
        private float focus;

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            focus += ((c.hasFocus() ? 1f : 0f) - focus) * 0.25f;
            Graphics2D g2 = smooth(g);
            if (focus > 0.02f) {
                g2.setColor(alpha(ACCENT, (int) (40 * focus)));
                g2.setStroke(new BasicStroke(4f));
                g2.draw(new RoundRectangle2D.Float(x + 2, y + 2, w - 5, h - 5, 12, 12));
            }
            g2.setColor(mix(BORDER, ACCENT, focus));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(x + 0.6f, y + 0.6f, w - 1.2f, h - 1.2f, 12, 12));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(9, 12, 9, 12);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    // ------------------------------------------------------------------- logo

    static List<Image> appIcons() {
        return List.of(logoImage(16), logoImage(32), logoImage(48), logoImage(64), logoImage(128));
    }

    static BufferedImage logoImage(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        paintLogo(g, size, 0);
        g.dispose();
        return image;
    }

    /**
     * Draws the Insect logo (a bug seen from above) in a size x size square.
     * {@code wiggle} (-1..1) bends the antennae for the idle animation.
     */
    static void paintLogo(Graphics2D g0, int size, double wiggle) {
        Graphics2D g = smooth(g0);
        g.scale(size / 100.0, size / 100.0);

        g.setColor(LOGO_BG);
        g.fill(new RoundRectangle2D.Double(0, 0, 100, 100, 26, 26));

        g.setPaint(new GradientPaint(30, 10, ACCENT, 70, 95, TEAL));
        g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double[][] legEnds = {{22, 32}, {17, 52}, {22, 74}};
        for (double[] end : legEnds) {
            g.draw(new Line2D.Double(50, 50, end[0], end[1]));
            g.draw(new Line2D.Double(50, 50, 100 - end[0], end[1]));
        }
        double bend = wiggle * 5;
        g.draw(new QuadCurve2D.Double(46, 26, 40 - bend, 13, 29 - bend, 12 + bend));
        g.draw(new QuadCurve2D.Double(54, 26, 60 + bend, 13, 71 + bend, 12 - bend));

        g.fill(new Ellipse2D.Double(42, 21, 16, 14)); // head
        g.fill(new Ellipse2D.Double(39, 35, 22, 20)); // thorax
        g.fill(new Ellipse2D.Double(36, 51, 28, 37)); // abdomen

        g.setColor(LOGO_BG);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(50, 56, 50, 83)); // wing split
        g.dispose();
    }

    /** The logo, gently floating with wiggling antennae. */
    static class Logo extends JComponent {
        private static final int FLOAT = 3;
        private final int size;

        Logo(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size + FLOAT * 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            double t = seconds();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(0, FLOAT + Math.sin(t * 1.8) * FLOAT);
            paintLogo(g2, size, Math.sin(t * 3.1));
            g2.dispose();
        }
    }

    // ------------------------------------------------------------ backgrounds

    /** Window background: dark base with slowly drifting colored glows and a faint dot grid. */
    static class Backdrop extends JPanel {
        Backdrop(LayoutManager layout) {
            super(layout);
            setBackground(BG);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = smooth(g);
            double t = seconds();
            int w = getWidth();
            int h = getHeight();
            glow(g2, w * (0.18 + 0.10 * Math.sin(t * 0.31)), h * (0.22 + 0.10 * Math.cos(t * 0.23)), w * 0.55, ACCENT, 48);
            glow(g2, w * (0.88 + 0.08 * Math.cos(t * 0.27)), h * (0.35 + 0.15 * Math.sin(t * 0.19)), w * 0.50, TEAL, 36);
            glow(g2, w * (0.55 + 0.15 * Math.sin(t * 0.17)), h * (1.00 + 0.06 * Math.cos(t * 0.29)), w * 0.60, VIOLET, 38);

            g2.setColor(new Color(255, 255, 255, 9));
            for (int x = 11; x < w; x += 22) {
                for (int y = 11; y < h; y += 22) {
                    g2.fillRect(x, y, 2, 2);
                }
            }
            g2.dispose();
        }

        private static void glow(Graphics2D g, double cx, double cy, double r, Color c, int a) {
            g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float) r,
                    new float[]{0f, 1f}, new Color[]{alpha(c, a), alpha(c, 0)}));
            g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        }
    }

    /** Frosted-glass card that stacks its children top to bottom and slides in when revealed. */
    static class Card extends JPanel {
        private final int delayMs;
        private long revealedAt = -1;

        Card(int delayMs) {
            this.delayMs = delayMs;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(new EmptyBorder(22, 22, 22, 22));
        }

        void reveal() {
            revealedAt = System.nanoTime();
        }

        private float progress() {
            if (revealedAt < 0) {
                return 0f;
            }
            float ms = (System.nanoTime() - revealedAt) / 1e6f - delayMs;
            return ease(ms / 700f);
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
        public void paint(Graphics g) {
            float p = progress();
            if (p <= 0f) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(p));
            g2.translate(0, (1f - p) * 30f);
            super.paint(g2);
            g2.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1.5f, h - 1.5f, 20, 20);
            g2.setColor(CARD);
            g2.fill(shape);
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 14), 0, h * 0.4f, new Color(255, 255, 255, 0)));
            g2.fill(shape); // soft top highlight
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(new Color(255, 255, 255, 18));
            g2.draw(shape);
            g2.setPaint(new GradientPaint(0, 0, alpha(ACCENT, 120), w * 0.7f, h, alpha(TEAL, 0)));
            g2.draw(shape); // gradient edge
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------- text

    /** Text filled with a slowly sliding green-to-teal gradient. */
    static class GradientText extends JComponent {
        private String text;

        GradientText(String text, Font font) {
            this.text = text;
            setFont(font);
            updateSize();
        }

        void setText(String text) {
            this.text = text;
            updateSize();
            repaint();
        }

        private void updateSize() {
            FontMetrics fm = getFontMetrics(getFont());
            setPreferredSize(new Dimension(fm.stringWidth(text) + 4, fm.getAscent() + fm.getDescent()));
            setMaximumSize(getPreferredSize());
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setFont(getFont());
            float w = Math.max(1, getWidth());
            float shift = (float) ((seconds() * 40) % (w * 2));
            g2.setPaint(new LinearGradientPaint(shift, 0, shift + w, 0,
                    new float[]{0f, 0.5f, 1f}, new Color[]{ACCENT, TEAL, ACCENT},
                    MultipleGradientPaint.CycleMethod.REPEAT));
            g2.drawString(text, 0, g2.getFontMetrics().getAscent());
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------- buttons

    /** Rounded gradient button with smooth hover, a passing shimmer and a press-in effect. */
    static class Button extends JButton {
        enum Kind { PRIMARY, DANGER, SECONDARY }

        private Kind kind;
        private boolean hovered;
        private float hover;
        private float press;

        Button(String text, Kind kind) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setFont(font(Font.BOLD, 14f));
            setBorder(new EmptyBorder(11, 18, 11, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setKind(kind);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                }
            });
        }

        void setKind(Kind kind) {
            this.kind = kind;
            setForeground(kind == Kind.PRIMARY ? BG : kind == Kind.DANGER ? Color.WHITE : TEXT);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            hover += ((hovered && isEnabled() ? 1f : 0f) - hover) * 0.3f;
            press += ((getModel().isArmed() && getModel().isPressed() ? 1f : 0f) - press) * 0.5f;

            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            double scale = 1 - 0.035 * press;
            g2.translate(w / 2.0, h / 2.0);
            g2.scale(scale, scale);
            g2.translate(-w / 2.0, -h / 2.0);

            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w, h, 14, 14);
            if (!isEnabled()) {
                g2.setColor(alpha(BORDER, 170));
                g2.fill(shape);
            } else if (kind == Kind.SECONDARY) {
                g2.setColor(new Color(255, 255, 255, (int) (14 + 16 * hover)));
                g2.fill(shape);
                g2.setColor(new Color(255, 255, 255, (int) (28 + 50 * hover)));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 14, 14));
            } else {
                Color from = kind == Kind.PRIMARY ? ACCENT : DANGER;
                Color to = kind == Kind.PRIMARY ? TEAL : PINK;
                float lift = 0.18f * hover;
                g2.setPaint(new GradientPaint(0, 0, mix(from, Color.WHITE, lift), w, h, mix(to, Color.WHITE, lift)));
                g2.fill(shape);
                paintShimmer(g2, shape, w, h);
            }
            super.paintComponent(g2);
            g2.dispose();
        }

        /** A light band that sweeps across the button every few seconds. */
        private static void paintShimmer(Graphics2D g2, Shape shape, int w, int h) {
            double phase = (seconds() % 3.5) / 1.1;
            if (phase >= 1) {
                return;
            }
            float x = (float) (-w * 0.5 + phase * w * 2.0);
            Shape oldClip = g2.getClip();
            g2.clip(shape);
            g2.setPaint(new LinearGradientPaint(x, 0, x + w * 0.35f, h,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(255, 255, 255, 0), new Color(255, 255, 255, 80), new Color(255, 255, 255, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.setClip(oldClip);
        }
    }

    // ----------------------------------------------------------------- status

    /** A colored dot (pulsing while something is live) followed by a short status message. */
    static class StatusLine extends JPanel {
        private final JLabel text = label("", font(Font.PLAIN, 13f), MUTED);
        private Color dot = MUTED;

        StatusLine(String initial) {
            super(new BorderLayout());
            setOpaque(false);
            text.setBorder(new EmptyBorder(2, 22, 2, 0));
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
            Graphics2D g2 = smooth(g);
            double cx = 7;
            double cy = getHeight() / 2.0;
            if (dot == ACCENT || dot == WARN) {
                double p = (seconds() % 1.6) / 1.6;
                double r = 4 + 6 * p;
                g2.setColor(alpha(dot, (int) (150 * (1 - p))));
                g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            }
            g2.setColor(dot);
            g2.fill(new Ellipse2D.Double(cx - 4, cy - 4, 8, 8));
            g2.dispose();
        }
    }
}
