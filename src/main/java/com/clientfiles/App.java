package com.clientfiles;

import com.clientfiles.core.AppConfig;
import com.clientfiles.core.AuthService;
import com.clientfiles.data.Database;
import com.clientfiles.data.DocumentRepository;
import com.clientfiles.data.UserRepository;
import com.clientfiles.ui.AppContext;
import com.clientfiles.ui.AppFrame;
import com.clientfiles.ui.Settings;
import com.clientfiles.ui.kit.Anim;
import com.clientfiles.ui.kit.Theme;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class App {
    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private App() {
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                LOG.log(Level.SEVERE, "Uncaught error on " + thread.getName(), error));
        SwingUtilities.invokeLater(App::start);
    }

    private static void start() {
        Theme.install(Settings.darkTheme());
        if (Settings.reducedMotion()) {
            Anim.setMotionEnabled(false);
        }
        try {
            Database database = new Database(AppConfig.load());
            UserRepository users = new UserRepository(database);
            AppContext context = new AppContext(users, new DocumentRepository(database), new AuthService(users));
            new AppFrame(context).open();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "The application could not start", e);
            JOptionPane.showMessageDialog(null, e.getMessage(), "Client Files", JOptionPane.ERROR_MESSAGE);
        }
    }
}
