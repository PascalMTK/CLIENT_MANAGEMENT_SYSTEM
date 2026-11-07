package com.clientfiles.core;

import com.clientfiles.data.UserRepository;
import com.clientfiles.data.UserRepository.Credentials;
import java.util.Optional;

public final class AuthService {
    /** Compared against when the email is unknown, so both failures take the same time. */
    private static final String DECOY_HASH = PasswordHasher.hash("decoy".toCharArray());

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public User signIn(String email, char[] password) {
        Optional<Credentials> found = users.findByEmail(Validation.normalizeEmail(email));
        boolean matches = PasswordHasher.matches(password, found.map(Credentials::passwordHash).orElse(DECOY_HASH));
        if (found.isEmpty() || !matches) {
            throw new AppException("Incorrect email or password.");
        }
        User user = found.get().user();
        if (!user.active()) {
            throw new AppException("This account has been deactivated. Contact your administrator.");
        }
        return user;
    }

    public void signUp(String fullName, String email, String phone, String address, char[] password) {
        users.createClient(fullName.trim(), Validation.normalizeEmail(email), phone.trim(), address.trim(),
                PasswordHasher.hash(password));
    }

    public void createFirstAdmin(String fullName, String email, char[] password) {
        users.createFirstAdmin(fullName.trim(), Validation.normalizeEmail(email), PasswordHasher.hash(password));
    }

    public void changePassword(int userId, char[] current, char[] replacement) {
        if (!PasswordHasher.matches(current, users.passwordHash(userId))) {
            throw new AppException("Your current password is incorrect.");
        }
        users.changePassword(userId, PasswordHasher.hash(replacement));
    }
}
