package com.clientfiles.ui;

import com.clientfiles.core.AppException;
import com.clientfiles.core.Validation;
import com.clientfiles.ui.kit.Async;
import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.Field;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Reveal;
import com.clientfiles.ui.kit.Spinner;
import com.clientfiles.ui.kit.Text;
import com.clientfiles.ui.kit.Theme;
import com.clientfiles.ui.kit.Toasts;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Sign-in, account creation, first-time setup and the database-unreachable state. */
final class AuthScreen extends JPanel {
    private static final int FORM_WIDTH = 408;

    private final AppContext context;
    private final JPanel right = new JPanel(new GridBagLayout()) {
        @Override
        public Color getBackground() {
            return Theme.c().bg();
        }
    };

    AuthScreen(AppContext context) {
        super(new GridBagLayout());
        this.context = context;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = .46;
        add(new BrandPanel(), gbc);
        gbc.weightx = .54;
        add(rightSide(), gbc);

        show(centered(new Spinner()), 0);
        checkDatabase();
    }

    private JComponent rightSide() {
        JPanel side = new JPanel(new BorderLayout()) {
            @Override
            public Color getBackground() {
                return Theme.c().bg();
            }
        };
        side.setPreferredSize(new Dimension(1, 1));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        top.setOpaque(false);
        top.add(new ThemeToggle(context));
        side.add(top, BorderLayout.NORTH);
        side.add(right);
        return side;
    }

    private void checkDatabase() {
        Async.run(() -> context.users().hasAdmin(),
                hasAdmin -> {
                    if (hasAdmin) {
                        showSignIn("", 0);
                    } else {
                        showFirstAdmin();
                    }
                },
                e -> showUnreachable(e.getMessage()));
    }

    private void show(JComponent form, int direction) {
        right.removeAll();
        right.add(form);
        right.revalidate();
        right.repaint();
        if (form instanceof Reveal reveal) {
            reveal.enter(0, direction * 28, direction == 0 ? 12 : 0);
        }
        SwingUtilities.invokeLater(() -> focusFirstField(form));
    }

    private static void focusFirstField(Component component) {
        if (component instanceof Field field) {
            field.focus();
        } else if (component instanceof java.awt.Container container) {
            Arrays.stream(container.getComponents()).filter(AuthScreen::holdsField).findFirst()
                    .ifPresent(AuthScreen::focusFirstField);
        }
    }

    private static boolean holdsField(Component component) {
        return component instanceof Field || (component instanceof java.awt.Container container
                && Arrays.stream(container.getComponents()).anyMatch(AuthScreen::holdsField));
    }

    private Reveal centered(JComponent content) {
        Reveal reveal = new Reveal(new GridBagLayout());
        reveal.add(content);
        return reveal;
    }

    private Column column(String title, String subtitle) {
        Column column = new Column();
        column.add(indented(new Text(title, Theme.semibold(28), () -> Theme.c().text())));
        column.add(Box.createVerticalStrut(6));
        column.add(indented(new Text(subtitle, Theme.regular(15), () -> Theme.c().textMuted())));
        column.add(Box.createVerticalStrut(26));
        return column;
    }

    /** Lines text up with the visible edge of the fields, which sit inside their focus ring. */
    private static JComponent indented(JComponent text) {
        text.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
        return text;
    }

    private void showSignIn(String email, int direction) {
        Column form = column("Welcome back", "Sign in to your client file space.");
        Field emailField = new Field("Email", "name@example.com").validator(Validation::email);
        Field passwordField = Field.password("Password")
                .validator(v -> v.isEmpty() ? "Enter your password." : null);
        emailField.setText(email);
        Btn submit = new Btn("Sign in", null, Btn.Kind.PRIMARY, null);
        submit.addActionListener(e -> signIn(emailField, passwordField, submit));
        form.add(emailField);
        form.add(passwordField);
        form.add(Box.createVerticalStrut(6));
        form.add(submit);
        form.add(Box.createVerticalStrut(14));
        form.add(switcher("New here?", "Create an account", () -> showSignUp()));
        show(form, direction);
        getRootPaneSafely(submit);
    }

    private void getRootPaneSafely(Btn submit) {
        SwingUtilities.invokeLater(() -> {
            if (getRootPane() != null) {
                getRootPane().setDefaultButton(submit);
            }
        });
    }

    private void signIn(Field email, Field password, Btn submit) {
        if (!Field.validateAll(email, password)) {
            return;
        }
        submit.setLoading(true);
        char[] secret = password.password();
        Async.run(() -> context.auth().signIn(email.text(), secret),
                user -> {
                    Arrays.fill(secret, ' ');
                    context.user(user);
                    context.frame().showWorkspace();
                },
                failure -> {
                    Arrays.fill(secret, ' ');
                    submit.setLoading(false);
                    showFailure(failure, password);
                });
    }

    private void showSignUp() {
        Column form = column("Create your account", "Clients use it to submit and follow their documents.");
        Field name = new Field("Full name", "Your name").validator(Validation::fullName);
        Field email = new Field("Email", "name@example.com").validator(Validation::email);
        Field phone = new Field("Phone (optional)", "+243 ...").validator(Validation::phone);
        Field address = new Field("Address (optional)", "Street, city").validator(Validation::address);
        Field password = Field.password("Password")
                .validator(v -> Validation.newPassword(v.toCharArray()));
        Btn submit = new Btn("Create account", null, Btn.Kind.PRIMARY, null);
        submit.addActionListener(e -> {
            if (!Field.validateAll(name, email, phone, address, password)) {
                return;
            }
            submit.setLoading(true);
            char[] secret = password.password();
            Async.run(() -> {
                context.auth().signUp(name.text(), email.text(), phone.text(), address.text(), secret);
                return true;
            }, done -> {
                Arrays.fill(secret, ' ');
                Toasts.success(this, "Account created. Sign in to continue.");
                showSignIn(Validation.normalizeEmail(email.text()), -1);
            }, failure -> {
                Arrays.fill(secret, ' ');
                submit.setLoading(false);
                showFailure(failure, email);
            });
        });

        JPanel pair = new JPanel(new GridLayout(1, 2, 10, 0));
        pair.setOpaque(false);
        pair.add(phone);
        pair.add(address);
        form.add(name);
        form.add(email);
        form.add(pair);
        form.add(password);
        form.add(Box.createVerticalStrut(6));
        form.add(submit);
        form.add(Box.createVerticalStrut(14));
        form.add(switcher("Already have an account?", "Sign in", () -> showSignIn("", -1)));
        show(form, 1);
        getRootPaneSafely(submit);
    }

    private void showFirstAdmin() {
        Column form = column("Set up your workspace", "First, create the administrator account.");
        Field name = new Field("Full name", "Your name").validator(Validation::fullName);
        Field email = new Field("Email", "name@example.com").validator(Validation::email);
        Field password = Field.password("Password")
                .validator(v -> Validation.newPassword(v.toCharArray()));
        Field confirm = Field.password("Confirm password")
                .validator(v -> v.equals(password.text()) ? null : "The passwords don't match.");
        Btn submit = new Btn("Create administrator", null, Btn.Kind.PRIMARY, null);
        submit.addActionListener(e -> {
            if (!Field.validateAll(name, email, password, confirm)) {
                return;
            }
            submit.setLoading(true);
            char[] secret = password.password();
            Async.run(() -> {
                context.auth().createFirstAdmin(name.text(), email.text(), secret);
                return true;
            }, done -> {
                Arrays.fill(secret, ' ');
                Toasts.success(this, "Administrator created. Sign in to continue.");
                showSignIn(Validation.normalizeEmail(email.text()), 1);
            }, failure -> {
                Arrays.fill(secret, ' ');
                submit.setLoading(false);
                showFailure(failure, email);
            });
        });
        form.add(name);
        form.add(email);
        form.add(password);
        form.add(confirm);
        form.add(Box.createVerticalStrut(6));
        form.add(submit);
        show(form, 0);
        getRootPaneSafely(submit);
    }

    private void showUnreachable(String reason) {
        Column form = new Column();
        JLabel icon = new JLabel(Icons.of("database", 34, () -> Theme.c().textMuted()));
        icon.setHorizontalAlignment(SwingConstants.LEFT);
        form.add(indented(icon));
        form.add(Box.createVerticalStrut(16));
        form.add(indented(new Text("Can't reach the database", Theme.semibold(26), () -> Theme.c().text())));
        form.add(Box.createVerticalStrut(10));
        form.add(paragraph(reason));
        form.add(Box.createVerticalStrut(6));
        form.add(paragraph("First launch? Run setup-database.bat, and check the connection in config.properties."));
        form.add(Box.createVerticalStrut(20));
        Btn retry = new Btn("Try again", "refresh-cw", Btn.Kind.PRIMARY, null);
        retry.addActionListener(e -> {
            retry.setLoading(true);
            checkDatabase();
        });
        form.add(retry);
        show(form, 0);
    }

    private static JComponent paragraph(String text) {
        JPanel lines = new JPanel();
        lines.setOpaque(false);
        lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
        lines.setAlignmentX(LEFT_ALIGNMENT);
        lines.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
        Text sample = Text.muted("");
        for (String line : com.clientfiles.ui.kit.Gfx.wrap(text, sample.getFontMetrics(Theme.regular(14)), FORM_WIDTH - 6)) {
            lines.add(new Text(line, Theme.regular(14), () -> Theme.c().textMuted()));
            lines.add(Box.createVerticalStrut(3));
        }
        return lines;
    }

    private void showFailure(AppException failure, Field field) {
        if (failure.isConnectionProblem()) {
            Toasts.error(this, failure.getMessage());
        } else {
            field.setError(failure.getMessage());
            field.shake();
            field.focus();
        }
    }

    private static JComponent switcher(String question, String action, Runnable onClick) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        row.setOpaque(false);
        row.add(new Text(question, Theme.regular(14), () -> Theme.c().textMuted()));
        Btn link = new Btn(action, null, Btn.Kind.GHOST, onClick);
        row.add(link);
        return row;
    }

    /** A vertical form of fixed width, in which children keep their natural height. */
    private static final class Column extends Reveal {
        Column() {
            super(null);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        public Component add(Component component) {
            if (component instanceof JComponent child) {
                child.setAlignmentX(LEFT_ALIGNMENT);
                if (!(component instanceof Box.Filler)) {
                    Dimension size = child.getPreferredSize();
                    child.setMaximumSize(new Dimension(FORM_WIDTH, size.height));
                }
            }
            return super.add(component);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(FORM_WIDTH, super.getPreferredSize().height);
        }
    }
}
