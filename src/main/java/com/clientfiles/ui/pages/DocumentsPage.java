package com.clientfiles.ui.pages;

import com.clientfiles.core.DocStatus;
import com.clientfiles.core.Document;
import com.clientfiles.ui.Shell;
import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.Card;
import com.clientfiles.ui.kit.DataTable;
import com.clientfiles.ui.kit.Segmented;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SortOrder;

/** Every document the person may see, filtered by status and free text. */
public final class DocumentsPage extends Page {
    public static final int PENDING = 1;
    public static final int EXPIRING = 4;
    private static final int APPROVED = 2;
    private static final int REJECTED = 3;
    private static final String TABLE = "table";
    private static final String EMPTY = "empty";

    private final Segmented filter = new Segmented("All", "Pending", "Approved", "Rejected", "Expiring");
    private final SearchBox search = new SearchBox("Search documents", this::applyFilter);
    private final DataTable<Document> table;
    private final CardLayout cards = new CardLayout();
    private final JPanel stack = new JPanel(cards);
    private final JPanel emptyHolder = new JPanel(new BorderLayout());
    private final Card card = new Card(new BorderLayout(), 0);
    private boolean firstLoad = true;
    private int total;

    public DocumentsPage(Shell shell) {
        super(shell);
        boolean admin = context.user().isAdmin();
        table = new DataTable<>(admin
                ? List.of(Columns.document(360), Columns.client(200), Columns.submitted(110), Columns.expires(120), Columns.status(110))
                : List.of(Columns.document(360), Columns.submitted(110), Columns.expires(120), Columns.supervisor(160), Columns.status(110)));
        table.onOpen(shell::openDocument);
        table.sortBy(admin ? 2 : 1, SortOrder.DESCENDING);
        filter.onChange(i -> {
            applyFilter();
            table.playEntrance();
        });

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(filter, BorderLayout.WEST);
        toolbar.add(left);
        toolbar.add(search, BorderLayout.EAST);

        stack.setOpaque(false);
        emptyHolder.setOpaque(false);
        JPanel inset = new JPanel(new BorderLayout());
        inset.setOpaque(false);
        inset.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        inset.add(table.inScrollPane());
        stack.add(inset, TABLE);
        stack.add(emptyHolder, EMPTY);
        card.add(stack);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(toolbar, BorderLayout.NORTH);
        content.add(card);
        setContent(content);
    }

    @Override
    public String title() {
        return "Documents";
    }

    @Override
    public String subtitle() {
        return context.user().isAdmin() ? "Review what clients have submitted." : "Everything you have submitted.";
    }

    @Override
    public JComponent action() {
        return context.user().isAdmin() ? null
                : new Btn("Submit document", "upload", Btn.Kind.PRIMARY, () -> UploadDialog.open(shell));
    }

    @Override
    public void reload() {
        load(() -> context.documents().visibleTo(context.user().id()), this::render);
    }

    private void render(List<Document> documents) {
        LocalDate today = LocalDate.now();
        total = documents.size();
        table.setRows(documents);
        filter.setCount(0, total);
        filter.setCount(PENDING, count(documents, d -> d.status() == DocStatus.PENDING));
        filter.setCount(APPROVED, count(documents, d -> d.status() == DocStatus.APPROVED));
        filter.setCount(REJECTED, count(documents, d -> d.status() == DocStatus.REJECTED));
        filter.setCount(EXPIRING, count(documents, d -> d.isExpiringSoon(today)));
        if (context.user().isAdmin()) {
            shell.setPendingBadge(count(documents, d -> d.status() == DocStatus.PENDING));
        }
        applyFilter();
        if (firstLoad) {
            firstLoad = false;
            card.enter(0, 0, 14);
            table.playEntrance();
        }
    }

    private static int count(List<Document> documents, Predicate<Document> test) {
        return (int) documents.stream().filter(test).count();
    }

    public void search(String text) {
        filter.select(0);
        search.setText(text);
    }

    public void showStatus(int filterIndex) {
        search.setText("");
        if (filter.selected() == filterIndex) {
            applyFilter();
        } else {
            filter.select(filterIndex);
        }
    }

    private void applyFilter() {
        LocalDate today = LocalDate.now();
        String text = search.text();
        int selected = filter.selected();
        table.setFilter(d -> matchesStatus(d, selected, today) && matchesText(d, text));
        showTableOrEmpty();
    }

    private static boolean matchesStatus(Document d, int selected, LocalDate today) {
        return switch (selected) {
            case PENDING -> d.status() == DocStatus.PENDING;
            case APPROVED -> d.status() == DocStatus.APPROVED;
            case REJECTED -> d.status() == DocStatus.REJECTED;
            case EXPIRING -> d.isExpiringSoon(today);
            default -> true;
        };
    }

    private static boolean matchesText(Document d, String text) {
        if (text.isEmpty()) {
            return true;
        }
        return (d.name() + " " + d.category() + " " + d.clientName() + " " + d.clientMail() + " " + d.supervisor())
                .toLowerCase(Locale.ROOT).contains(text);
    }

    private void showTableOrEmpty() {
        if (table.visibleRows() > 0) {
            cards.show(stack, TABLE);
            return;
        }
        emptyHolder.removeAll();
        if (total == 0 && !context.user().isAdmin()) {
            emptyHolder.add(new EmptyState("file-up", "No documents yet", "Submit a document and follow its review here.",
                    new Btn("Submit your first document", "upload", Btn.Kind.PRIMARY, () -> UploadDialog.open(shell))));
        } else if (total == 0) {
            emptyHolder.add(new EmptyState("inbox", "No documents yet", "Client submissions will appear here.", null));
        } else {
            emptyHolder.add(new EmptyState("search", "Nothing matches", "Try another filter or search term.",
                    new Btn("Clear filters", null, Btn.Kind.SECONDARY, () -> {
                        search.setText("");
                        filter.select(0);
                    })));
        }
        cards.show(stack, EMPTY);
    }
}
