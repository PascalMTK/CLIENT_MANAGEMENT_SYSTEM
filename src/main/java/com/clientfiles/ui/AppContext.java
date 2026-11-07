package com.clientfiles.ui;

import com.clientfiles.core.AuthService;
import com.clientfiles.core.User;
import com.clientfiles.data.DocumentRepository;
import com.clientfiles.data.UserRepository;

/** What every screen needs: the data layer and the person signed in. */
public final class AppContext {
    private final UserRepository users;
    private final DocumentRepository documents;
    private final AuthService auth;
    private AppFrame frame;
    private User user;

    public AppContext(UserRepository users, DocumentRepository documents, AuthService auth) {
        this.users = users;
        this.documents = documents;
        this.auth = auth;
    }

    public UserRepository users() {
        return users;
    }

    public DocumentRepository documents() {
        return documents;
    }

    public AuthService auth() {
        return auth;
    }

    public AppFrame frame() {
        return frame;
    }

    void frame(AppFrame frame) {
        this.frame = frame;
    }

    public User user() {
        return user;
    }

    public void user(User user) {
        this.user = user;
    }
}
