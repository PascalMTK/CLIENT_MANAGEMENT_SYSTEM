package com.clientfiles.ui.kit;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** A date input: type YYYY-MM-DD, or pick from the calendar. */
public class DateField extends Field {
    private static final DateTimeFormatter TITLE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    public DateField(String label) {
        super(label, "YYYY-MM-DD", new JTextField(), null);
        Btn open = Btn.icon("calendar", "Choose a date", Btn.Kind.GHOST, this::showCalendar);
        open.setPreferredSize(new Dimension(34, 34));
        setTrailing(new Fixed(open));
    }

    private void showCalendar() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        popup.add(new Month(popup, initialMonth()));
        popup.show(this, Math.max(0, getWidth() - 300), getHeight() - 18);
    }

    private YearMonth initialMonth() {
        try {
            return YearMonth.from(LocalDate.parse(text().trim()));
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }

    private static final class Fixed extends JPanel {
        Fixed(Btn button) {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(40, 40));
            add(button);
        }
    }

    private final class Month extends JPanel {
        private final JPopupMenu popup;
        private YearMonth month;

        Month(JPopupMenu popup, YearMonth month) {
            super(new BorderLayout(0, 8));
            this.popup = popup;
            this.month = month;
            setOpaque(false);
            rebuild();
        }

        private void rebuild() {
            removeAll();
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(Btn.icon("chevron-left", "Previous month", Btn.Kind.GHOST, () -> shift(-1)), BorderLayout.WEST);
            Text title = new Text(TITLE.format(month), Theme.semibold(14), () -> Theme.c().text());
            title.setHorizontalAlignment(SwingConstants.CENTER);
            header.add(title);
            header.add(Btn.icon("chevron-right", "Next month", Btn.Kind.GHOST, () -> shift(1)), BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            JPanel days = new JPanel(new GridLayout(0, 7, 2, 2));
            days.setOpaque(false);
            for (DayOfWeek day : DayOfWeek.values()) {
                Text name = new Text(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), Theme.regular(12),
                        () -> Theme.c().textMuted());
                name.setHorizontalAlignment(SwingConstants.CENTER);
                days.add(name);
            }
            for (int i = 1; i < month.atDay(1).getDayOfWeek().getValue(); i++) {
                days.add(new JPanel() {{ setOpaque(false); }});
            }
            LocalDate today = LocalDate.now();
            for (int d = 1; d <= month.lengthOfMonth(); d++) {
                LocalDate date = month.atDay(d);
                days.add(dayButton(date, date.isAfter(today)));
            }
            add(days);
            revalidate();
            repaint();
            popup.pack();
        }

        private JButton dayButton(LocalDate date, boolean selectable) {
            JButton button = new JButton(Integer.toString(date.getDayOfMonth()));
            button.setPreferredSize(new Dimension(38, 34));
            button.setFont(Theme.regular(13));
            button.setBorderPainted(false);
            button.setContentAreaFilled(selectable);
            button.setEnabled(selectable);
            button.setMargin(new java.awt.Insets(0, 0, 0, 0));
            button.setCursor(Cursor.getPredefinedCursor(selectable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            button.addActionListener(e -> {
                setText(date.toString());
                popup.setVisible(false);
                focus();
            });
            return button;
        }

        private void shift(int months) {
            month = month.plusMonths(months);
            rebuild();
        }
    }
}
