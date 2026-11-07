package com.clientfiles.ui.pages;

import com.clientfiles.core.AppException;
import com.clientfiles.ui.AppContext;
import com.clientfiles.ui.Shell;
import com.clientfiles.ui.kit.Async;
import com.clientfiles.ui.kit.Btn;
import com.clientfiles.ui.kit.Spinner;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagLayout;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/** A workspace page. Handles the loading and failure states so pages only describe content. */
public abstract class Page extends JPanel {
    private static final String CONTENT = "content";
    private static final String LOADING = "loading";
    private static final String STATE = "state";
    /** Fast answers should not make a spinner flash. */
    private static final int SPINNER_DELAY_MS = 220;

    protected final Shell shell;
    protected final AppContext context;
    private final CardLayout cards = new CardLayout();
    private final JPanel stack = new JPanel(cards);
    private final JPanel statePanel = new JPanel(new BorderLayout());
    private int loadGeneration;

    protected Page(Shell shell) {
        super(new BorderLayout());
        this.shell = shell;
        this.context = shell.context();
        setOpaque(false);
        stack.setOpaque(false);
        statePanel.setOpaque(false);
        JPanel spinner = new JPanel(new GridBagLayout());
        spinner.setOpaque(false);
        spinner.add(new Spinner());
        stack.add(spinner, LOADING);
        stack.add(statePanel, STATE);
        add(stack);
    }

    /** Sets the permanent content; call once from the page's constructor. */
    protected final void setContent(JComponent content) {
        content.setBorder(BorderFactory.createEmptyBorder(6, 28, 26, 28));
        stack.add(content, CONTENT);
        cards.show(stack, CONTENT);
    }

    public abstract String title();

    public abstract String subtitle();

    public JComponent action() {
        return null;
    }

    public abstract void reload();

    /** False when reloading would discard what the person is typing. */
    public boolean canRefresh() {
        return true;
    }

    protected final <T> void load(Callable<T> work, Consumer<T> render) {
        int generation = ++loadGeneration;
        Timer spinnerDelay = new Timer(SPINNER_DELAY_MS, e -> cards.show(stack, LOADING));
        spinnerDelay.setRepeats(false);
        spinnerDelay.start();
        Async.run(work, result -> {
            spinnerDelay.stop();
            if (generation == loadGeneration) {
                cards.show(stack, CONTENT);
                render.accept(result);
            }
        }, failure -> {
            spinnerDelay.stop();
            if (generation == loadGeneration) {
                showFailure(failure);
            }
        });
    }

    private void showFailure(AppException failure) {
        statePanel.removeAll();
        Btn retry = new Btn("Try again", "refresh-cw", Btn.Kind.PRIMARY, this::reload);
        statePanel.add(new EmptyState("circle-alert", "This page couldn't load", failure.getMessage(), retry));
        cards.show(stack, STATE);
    }
}
