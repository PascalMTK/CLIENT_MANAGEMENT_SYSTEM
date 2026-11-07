package com.clientfiles.ui.pages;

import com.clientfiles.core.ClientSummary;
import com.clientfiles.core.Format;
import com.clientfiles.ui.Shell;
import com.clientfiles.ui.kit.Async;
import com.clientfiles.ui.kit.Avatar;
import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.Card;
import com.clientfiles.ui.kit.DataTable;
import com.clientfiles.ui.kit.DataTable.Column;
import com.clientfiles.ui.kit.Overlay;
import com.clientfiles.ui.kit.Pill;
import com.clientfiles.ui.kit.Text;
import com.clientfiles.ui.kit.Theme;
import com.clientfiles.ui.kit.Toasts;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Client accounts, for administrators. */
public final class ClientsPage extends Page {
    private static final String TABLE = "table";
    private static final String EMPTY = "empty";

    private final SearchBox search = new SearchBox("Search clients", this::applyFilter);
    private final DataTable<ClientSummary> table = new DataTable<>(List.of(
            Columns.clientName(320),
            Column.text("Phone", 170, ClientSummary::phone, s -> s.phone() == null ? "—" : s.phone()),
            Columns.number("Documents", 100, ClientSummary::documents),
            Columns.number("Pending", 90, ClientSummary::pending),
            Column.text("Last submission", 140, ClientSummary::lastSubmission, s -> Format.date(s.lastSubmission())),
            new Column<>("Status", 110, s -> s.active() ? 0 : 1, s -> s.active() ? "Active" : "Inactive",
                    (g, s, w, h) -> Pill.paint(g, Pill.active(s.active()), 0, (h - Pill.height()) / 2))));
    private final CardLayout cards = new CardLayout();
    private final JPanel stack = new JPanel(cards);
    private final JPanel emptyHolder = new JPanel(new BorderLayout());
    private final Card card = new Card(new BorderLayout(), 0);
    private boolean firstLoad = true;
    private int total;

    public ClientsPage(Shell shell) {
        super(shell);
        table.onOpen(this::openClient);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
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
        return "Clients";
    }

    @Override
    public String subtitle() {
        return "Accounts that can submit documents.";
    }

    @Override
    public void reload() {
        load(() -> context.users().clients(context.user().id()), clients -> {
            total = clients.size();
            table.setRows(clients);
            applyFilter();
            if (firstLoad) {
                firstLoad = false;
                card.enter(0, 0, 14);
                table.playEntrance();
            }
        });
    }

    private void applyFilter() {
        String text = search.text();
        table.setFilter(c -> text.isEmpty()
                || (c.fullName() + " " + c.email() + " " + c.phone()).toLowerCase(Locale.ROOT).contains(text));
        if (table.visibleRows() > 0) {
            cards.show(stack, TABLE);
            return;
        }
        emptyHolder.removeAll();
        emptyHolder.add(total == 0
                ? new EmptyState("users", "No clients yet", "Clients appear here once they create an account.", null)
                : new EmptyState("search", "Nothing matches", "Try another search term.", null));
        cards.show(stack, EMPTY);
    }

    private void openClient(ClientSummary client) {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        JPanel content = DrawerParts.vertical();
        content.setBorder(BorderFactory.createEmptyBorder(14, 26, 20, 26));
        Overlay[] drawer = new Overlay[1];

        JPanel close = DrawerParts.closeBar(() -> drawer[0].close());
        close.setAlignmentX(0);
        content.add(close);
        Avatar avatar = new Avatar(64, client::fullName, false);
        avatar.setAlignmentX(0);
        content.add(avatar);
        content.add(DrawerParts.gap(12));
        JComponent title = DrawerParts.wrappedTitle(client.fullName());
        title.setAlignmentX(0);
        content.add(title);
        content.add(DrawerParts.gap(10));
        Pill pill = new Pill(Pill.active(client.active()));
        pill.setAlignmentX(0);
        content.add(pill);
        content.add(DrawerParts.gap(16));
        fact(content, "mail", "Email", client.email());
        fact(content, "phone", "Phone", client.phone() == null ? "—" : client.phone());
        fact(content, "map-pin", "Address", client.address() == null ? "—" : client.address());
        fact(content, "calendar", "Joined", Format.date(client.createdAt()));
        fact(content, "file-text", "Documents", Integer.toString(client.documents()));
        fact(content, "clock", "Pending", Integer.toString(client.pending()));
        root.add(DrawerParts.pinnedToTop(content));

        JPanel bar = DrawerParts.vertical();
        bar.setBorder(BorderFactory.createEmptyBorder(12, 23, 18, 23));
        Btn documents = new Btn("View documents", "file-text", Btn.Kind.SECONDARY, () -> {
            drawer[0].close();
            shell.showDocumentsOf(client.email());
        });
        Btn status = client.active()
                ? new Btn("Deactivate account", "user-x", Btn.Kind.DANGER_GHOST, null)
                : new Btn("Reactivate account", "user-check", Btn.Kind.PRIMARY, null);
        status.addActionListener(e -> askStatusChange(client, drawer[0]));
        documents.setAlignmentX(0);
        status.setAlignmentX(0);
        bar.add(documents);
        bar.add(status);
        root.add(bar, BorderLayout.SOUTH);

        drawer[0] = Overlay.open(shell, Overlay.Kind.DRAWER, DrawerParts.WIDTH, root);
    }

    private static void fact(JPanel content, String icon, String label, String value) {
        JComponent fact = DrawerParts.fact(icon, label, value);
        fact.setAlignmentX(0);
        content.add(fact);
    }

    private void askStatusChange(ClientSummary client, Overlay drawer) {
        boolean activate = !client.active();
        Runnable change = () -> Async.run(() -> {
            context.users().setClientActive(context.user().id(), client.id(), activate);
            return true;
        }, done -> {
            drawer.close();
            Toasts.success(shell, client.fullName() + (activate ? " can sign in again." : " can no longer sign in."));
            reload();
        }, failure -> Toasts.error(shell, failure.getMessage()));
        if (activate) {
            change.run();
        } else {
            Overlay.confirm(shell, "Deactivate this account?",
                    client.fullName() + " won't be able to sign in. Their documents are kept, and you can reactivate the account at any time.",
                    "Deactivate", true, change);
        }
    }
}
