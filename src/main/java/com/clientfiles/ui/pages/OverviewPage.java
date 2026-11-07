package com.clientfiles.ui.pages;

import com.clientfiles.core.ClientSummary;
import com.clientfiles.core.DocStatus;
import com.clientfiles.core.Document;
import com.clientfiles.core.Format;
import com.clientfiles.ui.Shell;
import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.Card;
import com.clientfiles.ui.kit.Chart;
import com.clientfiles.ui.kit.DataTable;
import com.clientfiles.ui.kit.Text;
import com.clientfiles.ui.kit.Theme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/** The first page after signing in: where things stand and what needs attention. */
public final class OverviewPage extends Page {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final int MONTHS = 6;
    private static final int LIST_SIZE = 5;

    private record Data(List<Document> documents, List<ClientSummary> clients) {
    }

    private final Body body = new Body();

    public OverviewPage(Shell shell) {
        super(shell);
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setOpaque(false);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scroll);
        setContent(wrapper);
    }

    @Override
    public String title() {
        return "Hello, " + context.user().fullName().split(" ")[0];
    }

    @Override
    public String subtitle() {
        return context.user().isAdmin() ? "Here is where client documents stand today."
                : "Here is where your documents stand today.";
    }

    @Override
    public JComponent action() {
        return context.user().isAdmin() ? null
                : new Btn("Submit document", "upload", Btn.Kind.PRIMARY, () -> UploadDialog.open(shell));
    }

    @Override
    public void reload() {
        boolean admin = context.user().isAdmin();
        int id = context.user().id();
        load(() -> new Data(context.documents().visibleTo(id), admin ? context.users().clients(id) : List.of()),
                this::render);
    }

    private void render(Data data) {
        body.removeAll();
        LocalDate today = LocalDate.now();
        List<Document> documents = data.documents();
        boolean admin = context.user().isAdmin();
        int pending = count(documents, DocStatus.PENDING);
        if (admin) {
            shell.setPendingBadge(pending);
        }
        List<Document> expiring = documents.stream().filter(d -> d.isExpiringSoon(today))
                .sorted(Comparator.comparing(Document::expires)).toList();

        Theme.Palette c = Theme.c();
        List<StatCard> stats = new ArrayList<>();
        if (admin) {
            long activeClients = data.clients().stream().filter(ClientSummary::active).count();
            stats.add(new StatCard("Clients", data.clients().size(), activeClients + " active", "users",
                    () -> Theme.c().accent(), () -> Theme.c().accentSoft(), () -> shell.go("Clients")));
            stats.add(new StatCard("Documents", documents.size(), "Across all clients", "file-text",
                    () -> Theme.c().info(), () -> Theme.c().infoSoft(), () -> shell.go("Documents")));
        } else {
            stats.add(new StatCard("Documents", documents.size(), "Submitted so far", "file-text",
                    () -> Theme.c().info(), () -> Theme.c().infoSoft(), () -> shell.go("Documents")));
            stats.add(new StatCard("Approved", count(documents, DocStatus.APPROVED), "Accepted by the reviewers",
                    "circle-check", () -> Theme.c().success(), () -> Theme.c().successSoft(), null));
        }
        stats.add(new StatCard(admin ? "Awaiting review" : "In review", pending,
                pending == 0 ? "All caught up" : admin ? "Oldest first in the list below" : "A reviewer will decide soon",
                "clock", () -> Theme.c().warning(), () -> Theme.c().warningSoft(), shell::showPending));
        stats.add(new StatCard("Expiring soon", expiring.size(), "In the next " + Document.EXPIRY_WARNING_DAYS + " days",
                "triangle-alert", () -> Theme.c().danger(), () -> Theme.c().dangerSoft(), shell::showExpiring));

        JPanel statRow = new JPanel(new GridLayout(1, stats.size(), 16, 0));
        statRow.setOpaque(false);
        stats.forEach(statRow::add);

        Chart.Bars bars = new Chart.Bars(monthlyBars(documents));
        Chart.Donut donut = new Chart.Donut(List.of(
                new Chart.Slice("Pending", pending, "clock", () -> Theme.c().warning()),
                new Chart.Slice("Approved", count(documents, DocStatus.APPROVED), "circle-check", () -> Theme.c().success()),
                new Chart.Slice("Rejected", count(documents, DocStatus.REJECTED), "circle-x", () -> Theme.c().danger())));
        Card barCard = chartCard("Documents submitted", "Last " + MONTHS + " months", bars);
        Card donutCard = chartCard("By status", "All documents", donut);

        Card listCard = admin ? pendingList(documents) : recentList(documents);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 16, 0);
        body.add(statRow, gbc);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = .62;
        gbc.insets = new Insets(0, 0, 16, 16);
        body.add(barCard, gbc);
        gbc.gridx = 1;
        gbc.weightx = .38;
        gbc.insets = new Insets(0, 0, 16, 0);
        body.add(donutCard, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(listCard, gbc);
        body.revalidate();
        body.repaint();

        int delay = 0;
        for (StatCard stat : stats) {
            stat.enter(delay, 0, 14);
            stat.countUp(delay + 60);
            delay += 45;
        }
        barCard.enter(delay, 0, 14);
        bars.play(delay + 80);
        donutCard.enter(delay + 45, 0, 14);
        donut.play(delay + 120);
        listCard.enter(delay + 90, 0, 14);
    }

    private static int count(List<Document> documents, DocStatus status) {
        return (int) documents.stream().filter(d -> d.status() == status).count();
    }

    private static List<Chart.Bar> monthlyBars(List<Document> documents) {
        YearMonth now = YearMonth.now();
        List<Chart.Bar> bars = new ArrayList<>();
        for (int i = MONTHS - 1; i >= 0; i--) {
            YearMonth month = now.minusMonths(i);
            int total = (int) documents.stream().filter(d -> YearMonth.from(d.submittedAt()).equals(month)).count();
            bars.add(new Chart.Bar(MONTH.format(month), MONTH_YEAR.format(month), total));
        }
        return bars;
    }

    private Card chartCard(String title, String caption, JComponent chart) {
        Card card = new Card(new BorderLayout(0, 12), 20);
        card.add(heading(title, caption), BorderLayout.NORTH);
        card.add(chart);
        return card;
    }

    private static JComponent heading(String title, String caption) {
        JPanel heading = new JPanel(new BorderLayout(0, 2));
        heading.setOpaque(false);
        heading.add(Text.title(title), BorderLayout.NORTH);
        heading.add(Text.muted(caption), BorderLayout.CENTER);
        return heading;
    }

    private Card pendingList(List<Document> documents) {
        List<Document> oldestFirst = documents.stream().filter(d -> d.status() == DocStatus.PENDING)
                .sorted(Comparator.comparing(Document::submittedAt)).limit(LIST_SIZE).toList();
        return listCard("Awaiting review", "Oldest submissions first", oldestFirst,
                List.of(Columns.document(340), Columns.client(200), Columns.submitted(120), Columns.expires(130)),
                new EmptyState("circle-check", "Nothing to review", "New submissions will appear here.", null));
    }

    private Card recentList(List<Document> documents) {
        Btn upload = new Btn("Submit your first document", "upload", Btn.Kind.PRIMARY, () -> UploadDialog.open(shell));
        return listCard("Recent documents", Format.plural(documents.size(), "document") + " in total",
                documents.stream().limit(LIST_SIZE).toList(),
                List.of(Columns.document(340), Columns.submitted(120), Columns.expires(130), Columns.status(120)),
                new EmptyState("file-up", "No documents yet", "Submit a document and follow its review here.", upload));
    }

    private Card listCard(String title, String caption, List<Document> rows,
                          List<DataTable.Column<Document>> columns, JComponent empty) {
        Card card = new Card(new BorderLayout(0, 8), 0);
        card.setBorder(BorderFactory.createEmptyBorder(18, 0, 8, 0));
        JComponent head = heading(title, caption);
        head.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        card.add(head, BorderLayout.NORTH);
        if (rows.isEmpty()) {
            empty.setPreferredSize(new Dimension(1, 260));
            card.add(empty);
            return card;
        }
        DataTable<Document> table = new DataTable<>(columns);
        table.setRows(rows);
        table.onOpen(shell::openDocument);
        JScrollPane scroll = table.inScrollPane();
        scroll.setPreferredSize(new Dimension(1, 40 + rows.size() * 58 + 2));
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        JPanel inset = new JPanel(new BorderLayout());
        inset.setOpaque(false);
        inset.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        inset.add(scroll);
        card.add(inset);
        return card;
    }

    /** Follows the viewport width, so only the height ever scrolls. */
    private static final class Body extends JPanel implements Scrollable {
        Body() {
            super(new GridBagLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 4));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visible.height - 48 : visible.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
