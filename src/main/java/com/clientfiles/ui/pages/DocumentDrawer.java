package com.clientfiles.ui.pages;

import com.clientfiles.core.DocStatus;
import com.clientfiles.core.Document;
import com.clientfiles.core.Format;
import com.clientfiles.core.Uploads;
import com.clientfiles.core.User;
import com.clientfiles.ui.AppContext;
import com.clientfiles.ui.Shell;
import com.clientfiles.ui.kit.Async;
import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.Field;
import com.clientfiles.ui.kit.Gfx;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Overlay;
import com.clientfiles.ui.kit.Pill;
import com.clientfiles.ui.kit.Text;
import com.clientfiles.ui.kit.Theme;
import com.clientfiles.ui.kit.Toasts;
import com.clientfiles.ui.kit.Widget;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** One document in detail, with everything that can be done to it. */
public final class DocumentDrawer {
    private static final int MAX_NOTE = 500;

    private final Shell shell;
    private final AppContext context;
    private final Document document;
    private final User user;
    private Overlay overlay;

    private DocumentDrawer(Shell shell, Document document) {
        this.shell = shell;
        this.context = shell.context();
        this.document = document;
        this.user = context.user();
    }

    public static void open(Shell shell, Document document) {
        DocumentDrawer drawer = new DocumentDrawer(shell, document);
        drawer.overlay = Overlay.open(shell, Overlay.Kind.DRAWER, DrawerParts.WIDTH, drawer.build());
    }

    private JComponent build() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);

        JPanel content = DrawerParts.vertical();
        content.setBorder(BorderFactory.createEmptyBorder(14, 26, 20, 26));
        JPanel close = DrawerParts.closeBar(() -> overlay.close());
        close.setAlignmentX(0);
        content.add(close);
        JComponent title = DrawerParts.wrappedTitle(document.name());
        title.setAlignmentX(0);
        content.add(title);
        content.add(DrawerParts.gap(10));
        JComponent pill = new Pill(Pill.of(document.status()));
        pill.setAlignmentX(0);
        content.add(pill);
        content.add(DrawerParts.gap(16));
        if (document.reviewNote() != null) {
            Note note = new Note(document.reviewNote());
            note.setAlignmentX(0);
            content.add(note);
            content.add(DrawerParts.gap(12));
        }
        LocalDate today = LocalDate.now();
        String expires = Format.date(document.expires());
        if (document.expires() != null && (document.isExpired(today) || document.isExpiringSoon(today))) {
            expires += " (" + Format.daysUntil(document.expires(), today) + ")";
        }
        if (user.isAdmin()) {
            addFact(content, "user-round", "Client", document.clientName());
            addFact(content, "mail", "Email", document.clientMail());
        }
        addFact(content, "folder-open", "Category", document.category());
        addFact(content, "file-text", "Type and size", document.type() + ", " + Format.size(document.size()));
        addFact(content, "clock", "Submitted", Format.dateTime(document.submittedAt()));
        addFact(content, "calendar", "Expires", expires);
        addFact(content, "user-check", "Supervisor", document.supervisor() == null ? "—" : document.supervisor());
        if (document.reviewedAt() != null) {
            addFact(content, "shield-check", "Reviewed by", document.reviewerName());
            addFact(content, "calendar", "Reviewed on", Format.dateTime(document.reviewedAt()));
        }

        JScrollPane scroll = new JScrollPane(DrawerParts.pinnedToTop(content));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        root.add(scroll);
        root.add(actions(), BorderLayout.SOUTH);
        return root;
    }

    private static void addFact(JPanel content, String icon, String label, String value) {
        JComponent fact = DrawerParts.fact(icon, label, value);
        fact.setAlignmentX(0);
        content.add(fact);
    }

    private JComponent actions() {
        JPanel bar = DrawerParts.vertical();
        bar.setBorder(BorderFactory.createEmptyBorder(12, 23, 18, 23));

        JPanel files = new JPanel(new GridLayout(1, 2, 4, 0));
        files.setOpaque(false);
        files.add(new Btn("Open", "eye", Btn.Kind.SECONDARY, this::openFile));
        files.add(new Btn("Download", "download", Btn.Kind.SECONDARY, this::download));
        files.setAlignmentX(0);
        files.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        bar.add(files);

        if (user.isAdmin()) {
            JPanel review = new JPanel(new GridLayout(1, 0, 4, 0));
            review.setOpaque(false);
            if (document.status() != DocStatus.REJECTED) {
                review.add(new Btn("Reject", "x", Btn.Kind.DANGER_GHOST, this::askRejection));
            }
            if (document.status() != DocStatus.APPROVED) {
                Btn approve = new Btn("Approve", "check", Btn.Kind.PRIMARY, null);
                approve.addActionListener(e -> review(approve, DocStatus.APPROVED, null));
                review.add(approve);
            }
            review.setAlignmentX(0);
            review.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            bar.add(review);
        } else if (document.canBeDeletedByOwner()) {
            Btn delete = new Btn("Delete document", "trash-2", Btn.Kind.DANGER_GHOST, this::askDeletion);
            delete.setAlignmentX(0);
            bar.add(delete);
        }
        return bar;
    }

    private void askRejection() {
        Field note = Field.multiline("Reason", "Tell the client what to correct");
        note.validator(v -> {
            if (v.isBlank()) {
                return "Explain why, so the client can correct it.";
            }
            return v.length() > MAX_NOTE ? "Use " + MAX_NOTE + " characters or fewer." : null;
        });
        Overlay[] dialog = new Overlay[1];
        Btn cancel = new Btn("Cancel", null, Btn.Kind.SECONDARY, () -> dialog[0].close());
        Btn reject = new Btn("Reject document", null, Btn.Kind.DANGER, null);
        reject.addActionListener(e -> {
            if (Field.validateAll(note)) {
                dialog[0].close();
                review(reject, DocStatus.REJECTED, note.text().trim());
            }
        });
        dialog[0] = Overlay.open(shell, Overlay.Kind.DIALOG, 480, Overlay.dialogBody(
                "Reject this document?", "The client sees your reason and can submit a corrected version.",
                note, cancel, reject));
    }

    private void review(Btn button, DocStatus decision, String note) {
        button.setLoading(true);
        Async.run(() -> {
            context.documents().review(document.id(), user.id(), decision, note);
            return true;
        }, done -> {
            button.setLoading(false);
            overlay.close();
            Toasts.success(shell, document.name() + " " + decision.label().toLowerCase(java.util.Locale.ROOT) + ".");
            shell.refreshCurrent();
        }, failure -> {
            button.setLoading(false);
            Toasts.error(shell, failure.getMessage());
        });
    }

    private void askDeletion() {
        Overlay.confirm(shell, "Delete this document?",
                document.name() + " will be removed for good, along with its review history.",
                "Delete document", true, () -> Async.run(() -> {
                    context.documents().delete(document.id(), user.id());
                    return true;
                }, done -> {
                    overlay.close();
                    Toasts.success(shell, document.name() + " deleted.");
                    shell.refreshCurrent();
                }, failure -> Toasts.error(shell, failure.getMessage())));
    }

    /** Characters Windows forbids are replaced; the stored name came from another computer. */
    private String safeFileName() {
        return document.name().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void openFile() {
        if (!Uploads.isAllowedType(document.name())) {
            Toasts.error(shell, "This file type can't be opened here. Download it instead.");
            return;
        }
        Async.run(() -> {
            Path folder = Files.createTempDirectory("client-files");
            Path file = folder.resolve(safeFileName());
            context.documents().saveContent(document.id(), user.id(), file);
            file.toFile().deleteOnExit();
            folder.toFile().deleteOnExit();
            Desktop.getDesktop().open(file.toFile());
            return true;
        }, done -> { }, failure -> Toasts.error(shell, failure.getMessage()));
    }

    private void download() {
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(shell);
        FileDialog dialog = new FileDialog(frame, "Save document", FileDialog.SAVE);
        dialog.setFile(safeFileName());
        dialog.setVisible(true);
        if (dialog.getFile() == null) {
            return;
        }
        Path target = Path.of(dialog.getDirectory(), dialog.getFile());
        Async.run(() -> {
            context.documents().saveContent(document.id(), user.id(), target);
            return true;
        }, done -> Toasts.success(shell, "Saved to " + target.getFileName() + "."),
                failure -> Toasts.error(shell, failure.getMessage()));
    }

    /** The reviewer's message, shown in a tinted box above the facts. */
    private final class Note extends Widget {
        private final java.util.List<String> lines;

        Note(String text) {
            lines = Gfx.wrap(text, getFontMetrics(Theme.regular(14)), DrawerParts.WIDTH - 52 - 46);
            int height = 44 + lines.size() * 21;
            setPreferredSize(new Dimension(DrawerParts.WIDTH - 52, height));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            getAccessibleContext().setAccessibleName("Review note: " + text);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            Theme.Palette c = Theme.c();
            boolean rejected = document.status() == DocStatus.REJECTED;
            g2.setColor(rejected ? c.dangerSoft() : c.surfaceAlt());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            Icons.paint(g2, "info", Icons.MEDIUM, rejected ? c.danger() : c.textMuted(), 13, 13);
            g2.setFont(Theme.semibold(13));
            g2.setColor(c.text());
            g2.drawString("Reviewer's note", 40, 27);
            g2.setFont(Theme.regular(14));
            int y = 49;
            for (String line : lines) {
                g2.drawString(line, 40, y);
                y += 21;
            }
            g2.dispose();
        }
    }
}
