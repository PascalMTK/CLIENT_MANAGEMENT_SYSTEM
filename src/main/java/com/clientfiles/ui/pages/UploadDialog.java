package com.clientfiles.ui.pages;

import com.clientfiles.core.Format;
import com.clientfiles.core.Uploads;
import com.clientfiles.core.Validation;
import com.clientfiles.ui.Shell;
import com.clientfiles.ui.kit.Anim;
import com.clientfiles.ui.kit.Async;
import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.DateField;
import com.clientfiles.ui.kit.Field;
import com.clientfiles.ui.kit.Gfx;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Overlay;
import com.clientfiles.ui.kit.Text;
import com.clientfiles.ui.kit.Theme;
import com.clientfiles.ui.kit.Toasts;
import com.clientfiles.ui.kit.Widget;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

/** Submits one document: drop or choose a file, then describe it. */
final class UploadDialog {
    private final Shell shell;
    private final DropZone zone = new DropZone();
    private final JComboBox<String> category = new JComboBox<>(Uploads.CATEGORIES.toArray(new String[0]));
    private final DateField expires = new DateField("Expiry date (optional)");
    private final Field supervisor = new Field("Supervisor (optional)", "Who is following this file?");
    private final Btn submit = new Btn("Submit document", "upload", Btn.Kind.PRIMARY, null);
    private final Btn cancel;
    private Overlay overlay;

    private UploadDialog(Shell shell) {
        this.shell = shell;
        this.cancel = new Btn("Cancel", null, Btn.Kind.SECONDARY, () -> overlay.close());
    }

    static void open(Shell shell) {
        UploadDialog dialog = new UploadDialog(shell);
        dialog.overlay = Overlay.open(shell, Overlay.Kind.DIALOG, 540, dialog.build());
    }

    private JComponent build() {
        expires.validator(v -> Validation.expiryDate(v, LocalDate.now()));
        supervisor.validator(Validation::supervisor);
        category.setPreferredSize(new Dimension(1, 42));
        submit.addActionListener(e -> submit());

        JPanel pair = new JPanel(new GridLayout(1, 2, 8, 0));
        pair.setOpaque(false);
        pair.add(Field.labelled("Category", category));
        pair.add(expires);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(zone, BorderLayout.NORTH);
        body.add(pair, BorderLayout.CENTER);
        body.add(supervisor, BorderLayout.SOUTH);
        return Overlay.dialogBody("Submit a document", "It stays pending until a reviewer decides.", body, cancel, submit);
    }

    private void submit() {
        Path file = zone.file;
        String problem = file == null ? "Choose a file to submit." : Uploads.problemWith(file);
        zone.setProblem(problem);
        boolean details = Field.validateAll(expires, supervisor);
        if (problem != null || !details) {
            return;
        }
        submit.setLoading(true);
        cancel.setEnabled(false);
        LocalDate expiry = expires.text().isBlank() ? null : LocalDate.parse(expires.text().trim());
        String chosenCategory = (String) category.getSelectedItem();
        int clientId = shell.context().user().id();
        Async.run(() -> shell.context().documents().create(clientId, file, chosenCategory, expiry, supervisor.text().trim()),
                id -> {
                    overlay.close();
                    Toasts.success(shell, file.getFileName() + " submitted for review.");
                    shell.refreshCurrent();
                }, failure -> {
                    submit.setLoading(false);
                    cancel.setEnabled(true);
                    Toasts.error(shell, failure.getMessage());
                });
    }

    /** Takes a dropped file; clicking it or pressing Enter opens the file chooser instead. */
    private final class DropZone extends Widget {
        private final Anim.Value over = new Anim.Value(this, 0);
        private Path file;
        private String problem;
        private boolean focused;

        DropZone() {
            setPreferredSize(new Dimension(1, 118));
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName("Choose a file to submit");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    over.to(.5);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    over.to(0);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    choose();
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
                getInputMap().put(KeyStroke.getKeyStroke(key, 0), "choose");
            }
            getActionMap().put("choose", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    choose();
                }
            });
            setTransferHandler(new TransferHandler() {
                @Override
                public boolean canImport(TransferSupport support) {
                    boolean accepted = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
                    over.to(accepted ? 1 : 0);
                    return accepted;
                }

                @Override
                @SuppressWarnings("unchecked")
                public boolean importData(TransferSupport support) {
                    over.to(0);
                    try {
                        List<File> files = (List<File>) support.getTransferable()
                                .getTransferData(DataFlavor.javaFileListFlavor);
                        if (files.isEmpty()) {
                            return false;
                        }
                        select(files.get(0).toPath());
                        return true;
                    } catch (java.awt.datatransfer.UnsupportedFlavorException | IOException e) {
                        setProblem("This file could not be read.");
                        return false;
                    }
                }
            });
        }

        private void choose() {
            Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
            FileDialog dialog = new FileDialog(frame, "Choose a document", FileDialog.LOAD);
            dialog.setVisible(true);
            if (dialog.getFile() != null) {
                select(Path.of(dialog.getDirectory(), dialog.getFile()));
            }
        }

        void select(Path chosen) {
            String issue = Uploads.problemWith(chosen);
            file = issue == null ? chosen : null;
            setProblem(issue);
            repaint();
        }

        void setProblem(String message) {
            problem = message;
            getAccessibleContext().setAccessibleDescription(message);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.hq(g);
            Theme.Palette c = Theme.c();
            int w = getWidth();
            int h = getHeight();
            double active = over.get();
            g2.setColor(Gfx.alpha(c.accentSoft(), active));
            g2.fillRoundRect(1, 1, w - 3, h - 3, 14, 14);
            float[] dash = file == null ? new float[] {6, 5} : null;
            g2.setStroke(new BasicStroke(focused ? 2f : 1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, dash, 0));
            g2.setColor(problem != null ? c.danger() : focused ? c.accent() : Gfx.mix(c.borderStrong(), c.accent(), active));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);

            int iconY = 18;
            Icons.paint(g2, file == null ? "file-up" : "file-text", Icons.LARGE, file == null ? c.textMuted() : c.accent(),
                    (w - Icons.LARGE) / 2, iconY);
            g2.setFont(Theme.semibold(14));
            FontMetrics strong = g2.getFontMetrics();
            String title = file == null ? "Drop a file here, or click to browse" : file.getFileName().toString();
            title = Gfx.ellipsize(title, strong, w - 40);
            g2.setColor(c.text());
            g2.drawString(title, (w - strong.stringWidth(title)) / 2f, iconY + 44);
            g2.setFont(Theme.regular(12));
            FontMetrics small = g2.getFontMetrics();
            String detail;
            if (problem != null) {
                detail = problem;
                g2.setColor(c.danger());
            } else {
                detail = file == null ? "PDF, Word, Excel, PowerPoint, CSV, TXT or image, up to 25 MB"
                        : fileSize() + "  ·  Click to replace";
                g2.setColor(c.textMuted());
            }
            detail = Gfx.ellipsize(detail, small, w - 24);
            g2.drawString(detail, (w - small.stringWidth(detail)) / 2f, iconY + 66);
            g2.dispose();
        }

        private String fileSize() {
            try {
                return Format.size(Files.size(file));
            } catch (IOException e) {
                return "";
            }
        }
    }
}
