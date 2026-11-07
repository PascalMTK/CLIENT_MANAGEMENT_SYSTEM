package com.clientfiles.ui;

import com.clientfiles.core.Document;
import com.clientfiles.core.Format;
import com.clientfiles.ui.kit.Anim;
import com.clientfiles.ui.kit.Avatar;
import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.Gfx;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Text;
import com.clientfiles.ui.kit.Theme;
import com.clientfiles.ui.kit.Toasts;
import com.clientfiles.ui.kit.Widget;
import com.clientfiles.ui.pages.ClientsPage;
import com.clientfiles.ui.pages.DocumentDrawer;
import com.clientfiles.ui.pages.DocumentsPage;
import com.clientfiles.ui.pages.OverviewPage;
import com.clientfiles.ui.pages.Page;
import com.clientfiles.ui.pages.ProfilePage;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** The workspace: sidebar, top bar, and the current page. */
public final class Shell extends JPanel {
    private static final int SIDEBAR_WIDTH = 248;
    private static final int ITEM_HEIGHT = 44;
    private static final int ITEM_GAP = 4;

    private final AppContext context;
    private final List<NavItem> items = new ArrayList<>();
    private final JPanel host = new JPanel(new BorderLayout());
    private final Text title = new Text(" ", Theme.semibold(22), () -> Theme.c().text());
    private final Text subtitle = Text.muted(" ");
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    private final Sidebar sidebar;
    private Btn refresh;
    private Page current;

    public Shell(AppContext context) {
        super(new BorderLayout());
        this.context = context;
        sidebar = new Sidebar();
        add(sidebar, BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout()) {
            @Override
            public Color getBackground() {
                return Theme.c().bg();
            }
        };
        main.add(topBar(), BorderLayout.NORTH);
        host.setOpaque(false);
        main.add(host);
        add(main);

        addItem("Overview", "layout-dashboard", () -> new OverviewPage(this));
        addItem("Documents", "file-text", () -> new DocumentsPage(this));
        if (context.user().isAdmin()) {
            addItem("Clients", "users", () -> new ClientsPage(this));
        }
        addItem("Account", "user-round", () -> new ProfilePage(this));
        sidebar.fill();
        select(items.get(0));
    }

    private JComponent topBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(22, 28, 14, 22));
        JPanel heading = new JPanel(new BorderLayout(0, 2));
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.CENTER);
        bar.add(heading);
        actions.setOpaque(false);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        right.setOpaque(false);
        right.add(actions);
        refresh = Btn.icon("refresh-cw", "Refresh", Btn.Kind.GHOST, () -> current.reload());
        right.add(refresh);
        right.add(new ThemeToggle(context));
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    public AppContext context() {
        return context;
    }

    private void addItem(String label, String icon, java.util.function.Supplier<Page> page) {
        NavItem item = new NavItem(label, icon, page);
        items.add(item);
    }

    public void go(String label) {
        items.stream().filter(i -> i.label.equals(label)).findFirst().ifPresent(this::select);
    }

    /** Shows the documents page with a search already applied. */
    public void showDocumentsOf(String search) {
        go("Documents");
        ((DocumentsPage) current).search(search);
    }

    public void showPending() {
        go("Documents");
        ((DocumentsPage) current).showStatus(DocumentsPage.PENDING);
    }

    public void showExpiring() {
        go("Documents");
        ((DocumentsPage) current).showStatus(DocumentsPage.EXPIRING);
    }

    private void select(NavItem item) {
        if (current != null && current == item.page) {
            if (current.canRefresh()) {
                current.reload();
            }
            return;
        }
        items.forEach(i -> i.setActive(i == item));
        sidebar.moveIndicator(items.indexOf(item));
        if (item.page == null) {
            item.page = item.pageFactory.get();
        }
        current = item.page;
        host.removeAll();
        host.add(current);
        title.setText(current.title());
        subtitle.setText(current.subtitle());
        refresh.setVisible(current.canRefresh());
        actions.removeAll();
        JComponent action = current.action();
        if (action != null) {
            actions.add(action);
        }
        revalidate();
        repaint();
        current.reload();
    }

    /** Pages off screen are outside the window, so a theme change has to reach them explicitly. */
    public void updateHiddenPages() {
        for (NavItem item : items) {
            if (item.page != null && item.page != current) {
                SwingUtilities.updateComponentTreeUI(item.page);
            }
        }
    }

    public void refreshCurrent() {
        current.reload();
    }

    public void openDocument(Document document) {
        DocumentDrawer.open(this, document);
    }

    public void setPendingBadge(int count) {
        items.stream().filter(i -> i.label.equals("Documents")).findFirst().ifPresent(i -> i.setBadge(count));
    }

    public void userChanged() {
        sidebar.repaint();
        sidebar.refreshUser();
    }

    private void signOut() {
        context.frame().showSignIn();
    }

    /** The dark navigation column, which stays dark in both themes. */
    private final class Sidebar extends JPanel {
        private final Anim.Value indicator = new Anim.Value(this, 0);
        private final JPanel list = new JPanel(null);
        private final Text name = new Text("", Theme.semibold(13), () -> Theme.c().sidebarTextStrong());
        private final Text role = new Text("", Theme.regular(12), () -> Theme.c().sidebarText());

        Sidebar() {
            super(new BorderLayout());
            setPreferredSize(new Dimension(SIDEBAR_WIDTH, 1));
            setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
            JComponent brand = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = Gfx.hq(g);
                    Brand.wordmark(g2, 8, 24, 30, Theme.c().sidebarTextStrong());
                    g2.dispose();
                }
            };
            brand.setPreferredSize(new Dimension(1, 86));
            add(brand, BorderLayout.NORTH);
            list.setOpaque(false);
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            add(list);
            add(userBlock(), BorderLayout.SOUTH);
        }

        void fill() {
            for (NavItem item : items) {
                item.setAlignmentX(LEFT_ALIGNMENT);
                list.add(item);
                list.add(Box.createVerticalStrut(ITEM_GAP));
            }
        }

        private JComponent userBlock() {
            JPanel block = new JPanel(new BorderLayout(10, 0));
            block.setOpaque(false);
            block.setBorder(BorderFactory.createEmptyBorder(14, 6, 0, 0));
            block.add(new Avatar(36, () -> context.user() == null ? "" : context.user().fullName(), true),
                    BorderLayout.WEST);
            JPanel text = new JPanel(new BorderLayout());
            text.setOpaque(false);
            text.add(name, BorderLayout.NORTH);
            text.add(role, BorderLayout.CENTER);
            block.add(text);
            block.add(Btn.icon("log-out", "Sign out", Btn.Kind.SIDEBAR, Shell.this::signOut), BorderLayout.EAST);
            refreshUser();
            return block;
        }

        void refreshUser() {
            FontMetrics fm = getFontMetrics(name.getFont());
            name.setText(Gfx.ellipsize(context.user().fullName(), fm, SIDEBAR_WIDTH - 118));
            role.setText(context.user().role().label());
        }

        void moveIndicator(int index) {
            double y = 86 + index * (ITEM_HEIGHT + ITEM_GAP);
            if (items.stream().noneMatch(NavItem::isShowing)) {
                indicator.set(y);
            } else {
                indicator.to(y, Anim.MEDIUM, Anim.SPRING);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            Theme.Palette c = Theme.c();
            g2.setColor(c.sidebar());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(c.sidebarBorder());
            int line = getHeight() - 14 - 14 - 46 - 7;
            g2.fillRect(14, line, getWidth() - 28, 1);
            int y = (int) Math.round(indicator.get());
            g2.setColor(c.sidebarActive());
            g2.fillRoundRect(14, y, getWidth() - 28, ITEM_HEIGHT, 12, 12);
            g2.setColor(Brand.MINT);
            g2.fillRoundRect(14, y + 12, 3, ITEM_HEIGHT - 24, 3, 3);
            g2.dispose();
        }
    }

    private final class NavItem extends Widget {
        private final String label;
        private final String icon;
        private final java.util.function.Supplier<Page> pageFactory;
        private final Anim.Value hover = new Anim.Value(this, 0);
        private Page page;
        private boolean active;
        private boolean focused;
        private int badge;

        NavItem(String label, String icon, java.util.function.Supplier<Page> pageFactory) {
            this.label = label;
            this.icon = icon;
            this.pageFactory = pageFactory;
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ITEM_HEIGHT));
            setPreferredSize(new Dimension(SIDEBAR_WIDTH - 28, ITEM_HEIGHT));
            getAccessibleContext().setAccessibleName(label);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover.to(1);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover.to(0);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    select(NavItem.this);
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = Gfx.byKeyboard(e);
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    repaint();
                }
            });
            for (int key : new int[] {KeyEvent.VK_ENTER, KeyEvent.VK_SPACE}) {
                getInputMap().put(KeyStroke.getKeyStroke(key, 0), "activate");
            }
            getActionMap().put("activate", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    select(NavItem.this);
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        void setBadge(int count) {
            badge = count;
            getAccessibleContext().setAccessibleDescription(
                    count == 0 ? null : Format.plural(count, "document") + " waiting for review");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            Theme.Palette c = Theme.c();
            if (!active && hover.get() > 0) {
                g2.setColor(Gfx.alpha(c.sidebarHover(), hover.get()));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            Color color = active ? c.sidebarTextStrong() : Gfx.mix(c.sidebarText(), c.sidebarTextStrong(), hover.get());
            Icons.paint(g2, icon, Icons.MEDIUM, color, 16, (getHeight() - Icons.MEDIUM) / 2);
            g2.setFont(active ? Theme.semibold(14) : Theme.regular(14));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(color);
            g2.drawString(label, 46, Gfx.baseline(fm, 0, getHeight()));
            if (badge > 0) {
                String text = badge > 99 ? "99+" : Integer.toString(badge);
                g2.setFont(Theme.semibold(12));
                FontMetrics small = g2.getFontMetrics();
                int width = Math.max(24, small.stringWidth(text) + 12);
                int x = getWidth() - width - 12;
                g2.setColor(Brand.MINT);
                g2.fillRoundRect(x, (getHeight() - 22) / 2, width, 22, 22, 22);
                g2.setColor(Brand.INK);
                g2.drawString(text, x + (width - small.stringWidth(text)) / 2f, Gfx.baseline(small, 0, getHeight()));
            }
            if (focused) {
                g2.setColor(c.sidebarTextStrong());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            }
            g2.dispose();
        }
    }

    public void toast(String message) {
        Toasts.success(this, message);
    }
}
