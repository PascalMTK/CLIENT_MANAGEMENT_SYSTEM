package com.clientfiles.ui.pages;

import com.clientfiles.core.AppException;
import com.clientfiles.core.Format;
import com.clientfiles.core.User;
import com.clientfiles.core.Validation;
import com.clientfiles.ui.Settings;
import com.clientfiles.ui.Shell;
import com.clientfiles.ui.kit.Anim;
import com.clientfiles.ui.kit.Async;
import com.clientfiles.ui.kit.Avatar;
import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.Card;
import com.clientfiles.ui.kit.Field;
import com.clientfiles.ui.kit.Segmented;
import com.clientfiles.ui.kit.Text;
import com.clientfiles.ui.kit.Theme;
import com.clientfiles.ui.kit.Toasts;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** The person's own details, password and interface preferences. */
public final class ProfilePage extends Page {
    private final Field name = new Field("Full name", null).validator(Validation::fullName);
    private final Field email = new Field("Email", null).validator(Validation::email);
    private final Field phone = new Field("Phone (optional)", "+243 ...").validator(Validation::phone);
    private final Field address = new Field("Address (optional)", "Street, city").validator(Validation::address);
    private final Field current = Field.password("Current password")
            .validator(v -> v.isEmpty() ? "Enter your current password." : null);
    private final Field replacement = Field.password("New password")
            .validator(v -> Validation.newPassword(v.toCharArray()));
    private final Field repeat = Field.password("Repeat new password");
    private final Btn saveDetails = new Btn("Save changes", null, Btn.Kind.PRIMARY, this::saveDetails);
    private final Btn savePassword = new Btn("Change password", null, Btn.Kind.SECONDARY, this::savePassword);
    private final Card detailsCard = new Card(new BorderLayout(0, 16), 24);
    private final Card securityCard = new Card(new BorderLayout(0, 12), 24);
    private final Card appearanceCard = new Card(new BorderLayout(0, 12), 24);

    public ProfilePage(Shell shell) {
        super(shell);
        repeat.validator(v -> v.equals(replacement.text()) ? null : "The passwords don't match.");

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = .5;
        gbc.gridheight = 2;
        gbc.insets = new Insets(0, 0, 0, 16);
        buildDetails();
        grid.add(detailsCard, gbc);
        gbc.gridx = 1;
        gbc.gridheight = 1;
        gbc.insets = new Insets(0, 0, 16, 0);
        buildSecurity();
        grid.add(securityCard, gbc);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        buildAppearance();
        grid.add(appearanceCard, gbc);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(grid, BorderLayout.NORTH);
        setContent(content);
    }

    @Override
    public String title() {
        return "Account";
    }

    @Override
    public String subtitle() {
        return "Your details, password and preferences.";
    }

    @Override
    public void reload() {
        User user = context.user();
        name.setText(user.fullName());
        email.setText(user.email());
        phone.setText(user.phone());
        address.setText(user.address());
        current.setText("");
        replacement.setText("");
        repeat.setText("");
        detailsCard.enter(0, 0, 14);
        securityCard.enter(45, 0, 14);
        appearanceCard.enter(90, 0, 14);
    }

    @Override
    public boolean canRefresh() {
        return false;
    }

    private void buildDetails() {
        JPanel head = new JPanel(new BorderLayout(14, 0));
        head.setOpaque(false);
        head.add(new Avatar(56, () -> context.user().fullName(), false), BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        text.add(Text.title("Personal details"));
        text.add(Text.muted(context.user().role().label() + " since " + Format.date(context.user().createdAt())));
        head.add(text);
        detailsCard.add(head, BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        for (Field field : new Field[] {name, email, phone, address}) {
            field.setAlignmentX(0);
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
            fields.add(field);
        }
        detailsCard.add(fields);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);
        row.add(saveDetails);
        detailsCard.add(row, BorderLayout.SOUTH);
    }

    private void buildSecurity() {
        securityCard.add(Text.title("Password"), BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridLayout(0, 1));
        fields.setOpaque(false);
        fields.add(current);
        JPanel pair = new JPanel(new GridLayout(1, 2, 8, 0));
        pair.setOpaque(false);
        pair.add(replacement);
        pair.add(repeat);
        fields.add(pair);
        securityCard.add(fields);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);
        row.add(savePassword);
        securityCard.add(row, BorderLayout.SOUTH);
    }

    private void buildAppearance() {
        appearanceCard.add(Text.title("Appearance"), BorderLayout.NORTH);
        Segmented theme = new Segmented("Light", "Dark");
        theme.getAccessibleContext().setAccessibleName("Theme");
        if (Theme.isDark()) {
            theme.select(1);
        }
        theme.onChange(i -> context.frame().setDarkTheme(i == 1));
        JCheckBox motion = new JCheckBox("Reduce motion", Settings.reducedMotion());
        motion.setFont(Theme.regular(14));
        motion.setOpaque(false);
        motion.addActionListener(e -> {
            Settings.reducedMotion(motion.isSelected());
            Anim.setMotionEnabled(!motion.isSelected());
        });
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(theme);
        row.add(Box.createHorizontalStrut(20));
        row.add(motion);
        appearanceCard.add(row);
    }

    private void saveDetails() {
        if (!Field.validateAll(name, email, phone, address)) {
            return;
        }
        User user = context.user();
        String newName = name.text().trim();
        String newEmail = Validation.normalizeEmail(email.text());
        String newPhone = phone.text().trim();
        String newAddress = address.text().trim();
        saveDetails.setLoading(true);
        Async.run(() -> {
            context.users().updateProfile(user.id(), newName, newEmail, newPhone, newAddress);
            return true;
        }, done -> {
            saveDetails.setLoading(false);
            context.user(user.withDetails(newName, newEmail, newPhone.isEmpty() ? null : newPhone,
                    newAddress.isEmpty() ? null : newAddress));
            shell.userChanged();
            Toasts.success(shell, "Your details have been saved.");
        }, failure -> {
            saveDetails.setLoading(false);
            showFailure(failure, email);
        });
    }

    private void savePassword() {
        if (!Field.validateAll(current, replacement, repeat)) {
            return;
        }
        char[] oldPassword = current.password();
        char[] newPassword = replacement.password();
        savePassword.setLoading(true);
        Async.run(() -> {
            context.auth().changePassword(context.user().id(), oldPassword, newPassword);
            return true;
        }, done -> {
            Arrays.fill(oldPassword, ' ');
            Arrays.fill(newPassword, ' ');
            savePassword.setLoading(false);
            current.setText("");
            replacement.setText("");
            repeat.setText("");
            Toasts.success(shell, "Your password has been changed.");
        }, failure -> {
            Arrays.fill(oldPassword, ' ');
            Arrays.fill(newPassword, ' ');
            savePassword.setLoading(false);
            showFailure(failure, current);
        });
    }

    private void showFailure(AppException failure, Field field) {
        if (failure.isConnectionProblem()) {
            Toasts.error(shell, failure.getMessage());
        } else {
            field.setError(failure.getMessage());
            field.shake();
            field.focus();
        }
    }
}
