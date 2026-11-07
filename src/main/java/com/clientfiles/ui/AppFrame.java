package com.clientfiles.ui;

import com.clientfiles.ui.kit.Anim;
import com.clientfiles.ui.kit.Theme;
import com.formdev.flatlaf.FlatLaf;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;

/** The single application window. Screens replace one another with a crossfade. */
public final class AppFrame extends JFrame {
    private final AppContext context;
    private final Fade fade = new Fade();

    public AppFrame(AppContext context) {
        super("Client Files");
        this.context = context;
        context.frame(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1440, 900);
        setLocationRelativeTo(null);
        setIconImages(List.of(Brand.icon(32), Brand.icon(64), Brand.icon(128)));
        setGlassPane(fade);
    }

    public void open() {
        showScreen(new AuthScreen(context));
        setExtendedState(MAXIMIZED_BOTH);
        setVisible(true);
    }

    public void showSignIn() {
        context.user(null);
        crossfade(() -> showScreen(new AuthScreen(context)));
    }

    public void showWorkspace() {
        crossfade(() -> showScreen(new Shell(context)));
    }

    private void showScreen(JComponent screen) {
        getRootPane().setDefaultButton(null);
        setContentPane(screen);
        revalidate();
    }

    public void setDarkTheme(boolean dark) {
        if (dark == Theme.isDark()) {
            return;
        }
        Settings.darkTheme(dark);
        crossfade(() -> {
            Theme.install(dark);
            FlatLaf.updateUI();
            if (getContentPane() instanceof Shell shell) {
                shell.updateHiddenPages();
            }
        });
    }

    private void crossfade(Runnable change) {
        JComponent content = (JComponent) getContentPane();
        if (!isShowing() || content.getWidth() == 0 || !Anim.motionEnabled()) {
            change.run();
            return;
        }
        fade.start(snapshot(content));
        change.run();
        Anim.run(0, 320, Anim.OUT, v -> fade.alpha(1 - v), fade::finish);
    }

    private BufferedImage snapshot(JComponent content) {
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        AffineTransform scale = configuration.getDefaultTransform();
        int width = (int) Math.ceil(content.getWidth() * scale.getScaleX());
        int height = (int) Math.ceil(content.getHeight() * scale.getScaleY());
        BufferedImage image = configuration.createCompatibleImage(width, height);
        Graphics2D g = image.createGraphics();
        g.transform(scale);
        content.paint(g);
        g.dispose();
        return image;
    }

    /** Sits above everything during a transition and lets all input through. */
    private static final class Fade extends JComponent {
        private BufferedImage image;
        private double alpha;

        void start(BufferedImage snapshot) {
            image = snapshot;
            alpha = 1;
            setVisible(true);
        }

        void alpha(double value) {
            alpha = value;
            repaint();
        }

        void finish() {
            image = null;
            setVisible(false);
        }

        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (image != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
                g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                g2.dispose();
            }
        }
    }
}
