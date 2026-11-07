package com.clientfiles.ui.pages;

import com.clientfiles.core.ClientSummary;
import com.clientfiles.core.Document;
import com.clientfiles.core.Format;
import com.clientfiles.ui.kit.Avatar;
import com.clientfiles.ui.kit.DataTable;
import com.clientfiles.ui.kit.DataTable.Column;
import com.clientfiles.ui.kit.Gfx;
import com.clientfiles.ui.kit.Icons;
import com.clientfiles.ui.kit.Pill;
import com.clientfiles.ui.kit.Theme;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.time.LocalDate;
import java.util.function.Function;

/** The table columns shared by the pages. */
final class Columns {
    private Columns() {
    }

    static Column<Document> document(int width) {
        return new Column<>("Document", width, Document::name, Document::name, (g, d, w, h) ->
                twoLines(g, d.type(), d.name(), Format.size(d.size()) + "  ·  " + d.category(), w, h));
    }

    static Column<Document> client(int width) {
        return new Column<>("Client", width, Document::clientName, Document::clientName, (g, d, w, h) -> {
            Theme.Palette c = Theme.c();
            Avatar.paint(g, d.clientName(), 0, (h - 32) / 2, 32, c.accentSoft(), c.accent());
            g.setFont(Theme.regular(14));
            DataTable.paintText(g, d.clientName(), c.text(), 42, 0, w - 42, h);
        });
    }

    static Column<Document> category(int width) {
        return Column.text("Category", width, Document::category, Document::category);
    }

    static Column<Document> submitted(int width) {
        return Column.text("Submitted", width, Document::submittedAt, d -> Format.date(d.submittedAt()));
    }

    static Column<Document> supervisor(int width) {
        return Column.text("Supervisor", width, Document::supervisor,
                d -> d.supervisor() == null ? "—" : d.supervisor());
    }

    static Column<Document> expires(int width) {
        return new Column<>("Expires", width, Document::expires, d -> Format.date(d.expires()), (g, d, w, h) -> {
            Theme.Palette c = Theme.c();
            LocalDate today = LocalDate.now();
            g.setFont(Theme.regular(14));
            if (d.expires() == null || !(d.isExpired(today) || d.isExpiringSoon(today))) {
                DataTable.paintText(g, Format.date(d.expires()), c.text(), 0, 0, w, h);
                return;
            }
            boolean expired = d.isExpired(today);
            Color tone = expired ? c.danger() : c.warning();
            FontMetrics fm = g.getFontMetrics();
            int top = (h - 2 * fm.getHeight() + 2) / 2;
            g.setColor(c.text());
            g.drawString(Gfx.ellipsize(Format.date(d.expires()), fm, w), 0, top + fm.getAscent());
            Icons.paint(g, "triangle-alert", 13, tone, 0, top + fm.getHeight() + 1);
            g.setFont(Theme.semibold(12));
            g.setColor(tone);
            g.drawString(Format.daysUntil(d.expires(), today), 18, top + fm.getHeight() + 12);
        });
    }

    static Column<Document> status(int width) {
        return new Column<>("Status", width, d -> d.status().ordinal(), d -> d.status().label(),
                (g, d, w, h) -> Pill.paint(g, Pill.of(d.status()), 0, (h - Pill.height()) / 2));
    }

    static Column<ClientSummary> clientName(int width) {
        return new Column<>("Client", width, ClientSummary::fullName, ClientSummary::fullName, (g, s, w, h) -> {
            Theme.Palette c = Theme.c();
            Avatar.paint(g, s.fullName(), 0, (h - 36) / 2, 36, c.accentSoft(), c.accent());
            g.translate(48, 0);
            stacked(g, s.fullName(), s.email(), w - 48, h);
            g.translate(-48, 0);
        });
    }

    static <T> Column<T> number(String title, int width, Function<T, Integer> value) {
        return Column.text(title, width, value::apply, row -> value.apply(row).toString());
    }

    private static void twoLines(Graphics2D g, String tile, String top, String bottom, int w, int h) {
        Theme.Palette c = Theme.c();
        int size = 38;
        int y = (h - size) / 2;
        g.setColor(c.surfaceAlt());
        g.fillRoundRect(0, y, size, size, 10, 10);
        g.setFont(Theme.semibold(tile.length() > 3 ? 10 : 11));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(c.textMuted());
        g.drawString(tile, (size - fm.stringWidth(tile)) / 2f, Gfx.baseline(fm, y, size));
        g.translate(size + 12, 0);
        stacked(g, top, bottom, w - size - 12, h);
        g.translate(-size - 12, 0);
    }

    private static void stacked(Graphics2D g, String top, String bottom, int w, int h) {
        Theme.Palette c = Theme.c();
        g.setFont(Theme.semibold(14));
        FontMetrics strong = g.getFontMetrics();
        int y = (h - 2 * strong.getHeight() + 2) / 2;
        g.setColor(c.text());
        g.drawString(Gfx.ellipsize(top, strong, w), 0, y + strong.getAscent());
        g.setFont(Theme.regular(12));
        FontMetrics small = g.getFontMetrics();
        g.setColor(c.textMuted());
        g.drawString(Gfx.ellipsize(bottom, small, w), 0, y + strong.getHeight() + small.getAscent() - 1);
    }
}
