package com.clientfiles.ui;

import com.clientfiles.ui.kit.Anim;
import com.clientfiles.ui.kit.Gfx;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Theme;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * The left half of the sign-in screen. Stays dark in both themes. Its small scene shows what the
 * product does: a stack of client files, with each one moving from pending to approved.
 */
final class BrandPanel extends JComponent {
    private static final Color TOP = new Color(0x0D2A25);
    private static final Color BOTTOM = new Color(0x123F36);
    private static final Color SHEET = new Color(0xF6F5F1);
    private static final Color LINE = new Color(0xD9D6CC);
    private static final Color SHEET_TEXT = new Color(0x1B1F1E);
    private static final Color MUTED = new Color(0xA9C4BC);
    private static final int CYCLE_MS = 5200;
    private static final int SCENE_WIDTH = 620;
    private static final int SCENE_HEIGHT = 480;
    private static final int SHEET_WIDTH = 232;
    private static final int SHEET_HEIGHT = 296;
    private static final int SHEET_MARGIN = 16;
    /** Per sheet: x, y, rotation, opacity, then the speed, amplitude and phase of its drift. */
    private static final double[][] SHEETS = {
        {-96, -6, -8, .62, .7, 5, 0},
        {92, 6, 7, .8, .8, 5, 2},
        {0, 0, -1, 1, .9, 6, 1},
    };
    private static final int FRONT = 2;
    private static final String HEADLINE = "Every client file, reviewed and accounted for.";
    private static final String SUBLINE = "Submit documents, follow each review and catch expiry dates before they pass.";

    private double clock = 2400;
    private long lastFrame;
    private double deviceScale = 1;
    private BufferedImage backdrop;
    private final BufferedImage[] sheets = new BufferedImage[SHEETS.length];

    @Override
    public void addNotify() {
        super.addNotify();
        Anim.forever(this, ms -> {
            clock = ms + 2400;
            long frame = frameId();
            if (frame != lastFrame) {
                lastFrame = frame;
                repaint(getWidth() / 2 - SCENE_WIDTH / 2, (int) (getHeight() * .42) - SCENE_HEIGHT / 2,
                        SCENE_WIDTH, SCENE_HEIGHT);
            }
        });
    }

    /** Changes only when something would look different, so a still frame costs nothing. */
    private long frameId() {
        long id = 0;
        for (int i = 0; i < SHEETS.length; i++) {
            id = id * 64 + drift(i) + 32;
        }
        double phase = (clock % CYCLE_MS) / CYCLE_MS;
        boolean moving = phase < .05 || phase > .95 || (phase > .45 && phase < .53);
        return id * 100_000 + (moving ? (long) (clock / 33) % 100_000 : phase > .45 ? 1 : 0);
    }

    /** The vertical drift of a sheet, in whole device pixels. */
    private int drift(int sheet) {
        double[] s = SHEETS[sheet];
        return (int) Math.round(Math.sin(clock / 1000 * s[4] + s[6]) * s[5] * deviceScale);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.hq(g);
        int w = getWidth();
        int h = getHeight();
        double scale = g2.getTransform().getScaleX();
        if (scale != deviceScale) {
            deviceScale = scale;
            java.util.Arrays.fill(sheets, null);
        }
        if (backdrop == null || backdrop.getWidth() != (int) (w * scale) || backdrop.getHeight() != (int) (h * scale)) {
            backdrop = renderBackdrop(w, h, scale);
        }
        g2.drawImage(backdrop, 0, 0, w, h, null);
        paintScene(g2, w / 2, (int) (h * .42));
        g2.dispose();
    }

    /** Everything that never moves, so that each animation frame only draws the sheets. */
    private BufferedImage renderBackdrop(int w, int h, double scale) {
        BufferedImage image = new BufferedImage((int) (w * scale), (int) (h * scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = Gfx.hq(image.createGraphics());
        g2.scale(scale, scale);
        g2.setPaint(new GradientPaint(0, 0, TOP, w * .4f, h, BOTTOM));
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(255, 255, 255, 13));
        for (int y = 24; y < h; y += 28) {
            for (int x = 24; x < w; x += 28) {
                g2.fillOval(x, y, 2, 2);
            }
        }
        Brand.wordmark(g2, 44, 40, 32, Brand.PAPER);
        paintCopy(g2, w, h);
        g2.dispose();
        return image;
    }

    private void paintCopy(Graphics2D g, int w, int h) {
        int x = 44;
        int width = Math.min(w - 2 * x, 500);
        g.setFont(Theme.semibold(30));
        FontMetrics big = g.getFontMetrics();
        java.util.List<String> lines = Gfx.wrap(HEADLINE, big, width);
        g.setFont(Theme.regular(15));
        FontMetrics small = g.getFontMetrics();
        java.util.List<String> sub = Gfx.wrap(SUBLINE, small, width);
        int y = h - 48 - sub.size() * 23 - 16 - lines.size() * 38;

        g.setFont(Theme.semibold(30));
        g.setColor(Brand.PAPER);
        for (String line : lines) {
            y += 38;
            g.drawString(line, x, y - 8);
        }
        y += 16;
        g.setFont(Theme.regular(15));
        g.setColor(MUTED);
        for (String line : sub) {
            y += 23;
            g.drawString(line, x, y - 6);
        }
    }

    private void paintScene(Graphics2D g, int cx, int cy) {
        AffineTransform transform = g.getTransform();
        Graphics2D device = (Graphics2D) g.create();
        device.setTransform(AffineTransform.getTranslateInstance(transform.getTranslateX(), transform.getTranslateY()));
        for (int i = 0; i < SHEETS.length; i++) {
            if (sheets[i] == null) {
                sheets[i] = renderSheet(SHEETS[i][2], SHEETS[i][3], deviceScale);
            }
            BufferedImage image = sheets[i];
            int x = (int) Math.round((cx + SHEETS[i][0]) * deviceScale - image.getWidth() / 2.0);
            int y = (int) Math.round((cy + SHEETS[i][1]) * deviceScale - image.getHeight() / 2.0) + drift(i);
            device.drawImage(image, x, y, null);
        }
        device.dispose();

        Graphics2D front = (Graphics2D) g.create();
        front.translate(cx, cy + drift(FRONT) / deviceScale);
        front.rotate(Math.toRadians(SHEETS[FRONT][2]));
        front.translate(-SHEET_WIDTH / 2.0, -SHEET_HEIGHT / 2.0);
        status(front, SHEET_WIDTH, SHEET_HEIGHT, (clock % CYCLE_MS) / CYCLE_MS);
        front.dispose();
    }

    /**
     * Each sheet is drawn once, already rotated, at the screen's resolution. Moving a finished
     * picture by whole pixels is what keeps this scene cheap without graphics acceleration.
     */
    private static BufferedImage renderSheet(double degrees, double opacity, double scale) {
        int w = SHEET_WIDTH;
        int h = SHEET_HEIGHT;
        int side = (int) Math.ceil((Math.hypot(w, h) + 2 * SHEET_MARGIN) * scale);
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D s = Gfx.hq(image.createGraphics());
        s.translate(side / 2.0, side / 2.0);
        s.rotate(Math.toRadians(degrees));
        s.scale(scale, scale);
        s.translate(-w / 2.0, -h / 2.0);
        s.setComposite(AlphaComposite.SrcOver.derive((float) opacity));
        s.setColor(new Color(0, 0, 0, 60));
        s.fillRoundRect(4, 12, w, h, 18, 18);
        s.setColor(SHEET);
        s.fillRoundRect(0, 0, w, h, 18, 18);
        s.setColor(new Color(0xDDEFEA));
        s.fillRoundRect(22, 24, 40, 40, 11, 11);
        Icons.paint(s, "file-text", Icons.LARGE, new Color(0x0E6B5C), 31, 33);
        s.setColor(LINE);
        s.fillRoundRect(74, 30, 110, 10, 5, 5);
        s.fillRoundRect(74, 48, 72, 8, 4, 4);
        for (int i = 0; i < 5; i++) {
            s.fillRoundRect(22, 92 + i * 24, i == 4 ? 120 : 188, 8, 4, 4);
        }
        s.dispose();
        return image;
    }

    private void status(Graphics2D g, int w, int h, double phase) {
        boolean approved = phase > .45;
        double pop = approved ? Math.min(1, (phase - .45) / .08) : 1;
        double appear = Math.min(1, phase / .05);
        double leave = Math.min(1, (1 - phase) / .05);
        float alpha = (float) Math.min(appear, leave);
        double scale = approved ? .8 + .2 * Anim.SPRING.at(pop) : 1;
        String label = approved ? "Approved" : "Pending";
        String icon = approved ? "circle-check" : "clock";
        Color fg = approved ? new Color(0x1B7A4B) : new Color(0x8A5A07);
        Color bg = approved ? new Color(0xDFF3E7) : new Color(0xFBF0D9);

        Graphics2D p = (Graphics2D) g.create();
        p.setComposite(AlphaComposite.SrcOver.derive(alpha));
        p.setFont(Theme.semibold(13));
        FontMetrics fm = p.getFontMetrics();
        int width = fm.stringWidth(label) + 42;
        int x = 22;
        int y = h - 54;
        p.translate(x + width / 2.0, y + 16);
        p.scale(scale, scale);
        p.translate(-width / 2.0, -16);
        p.setColor(bg);
        p.fillRoundRect(0, 0, width, 32, 32, 32);
        Icons.paint(p, icon, Icons.SMALL, fg, 10, 8);
        p.setColor(fg);
        p.drawString(label, 31, Gfx.baseline(fm, 0, 32));
        p.dispose();

        g.setColor(SHEET_TEXT);
        g.setComposite(AlphaComposite.SrcOver.derive(.45f * alpha));
        g.setFont(Theme.regular(12));
        String date = "Client file";
        g.drawString(date, w - 22 - g.getFontMetrics().stringWidth(date), h - 33);
    }
}
