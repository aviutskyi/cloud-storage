package org.cloud.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldVerifier {
    private static final Pattern name = Pattern.compile("[^a-z0-9_]", Pattern.CASE_INSENSITIVE);
    private static final Pattern passExclude = Pattern.compile("[^a-zA-Z0-9!@#$%&*()_+=|<>?{}\\[\\]~-]");

    private static final int minLength = 3;
    private static final int maxLength = 15;

    public static void verifyUsername(String username) throws FieldVerificationException {
        Matcher matcher = name.matcher(username);
        if (username.length() < minLength || username.length() > maxLength) {
            throw new FieldVerificationException("Username must contain " + minLength + " - " + maxLength + " characters");
        }
        if (matcher.find()) {
            throw new FieldVerificationException("Username must include only Latin letters, numbers, or underscores");
        }
    }

    public static void verifyPassword(String password) throws FieldVerificationException {
        Matcher matcherExc = passExclude.matcher(password);
        if (password.length() < minLength || password.length() > maxLength) {
            throw new FieldVerificationException("Password must contain " + minLength + " - " + maxLength + " characters");
        }
        if (matcherExc.find()) {
            throw new FieldVerificationException("Password must include only Latin letters, numbers, or special characters");
        }
    }

    public static void verifyCredentials(String username, String password) throws FieldVerificationException {
        verifyUsername(username);
        verifyPassword(password);
    }

    public static void verifyPasswordMatching(String password, String confirmPassword) throws FieldVerificationException {
        if (!password.equals(confirmPassword)) {
            throw new FieldVerificationException("Password and confirmation password do not match");
        }
    }

}
