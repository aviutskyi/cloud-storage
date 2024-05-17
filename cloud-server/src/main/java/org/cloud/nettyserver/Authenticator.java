package org.cloud.nettyserver;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;

@Slf4j
public class Authenticator {
    private static Connection connection;
    private static PreparedStatement checkPass;
    private static PreparedStatement addUser;
    private static PreparedStatement checkDuplicate;

    private static final String URL = "jdbc:mysql://localhost/cloud_users";
    private static final String USERNAME = "clouduser";
    private static final String PASS = "clouduser";

    public static void connect() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASS);
            log.debug("Connected to DB");
            checkDuplicate = connection.prepareStatement("SELECT user_name FROM cloud_users.users WHERE " +
                    "lower(user_name) LIKE lower(?)");
            checkPass = connection.prepareStatement("SELECT user_name FROM cloud_users.users WHERE " +
                    "lower(user_name) LIKE lower(?) AND password = ?;");
            addUser = connection.prepareStatement("INSERT INTO cloud_users.users (user_name, password) " +
                    "VALUES (?, ?);");
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            log.error("Unable to connect to database", e);
            throw new RuntimeException(e);
        }
    }

    public static void disconnect() {
        closePreparedStatements();
        try {
            if (connection != null) {
                connection.close();
                log.debug("Disconnected from DB");
            }
        } catch (SQLException e) {
            log.error("Unable to close database connection", e);
        }
    }

    public static synchronized boolean loginMatchesPass(String username, String pass) {
        ResultSet rs = null;
        try {
            checkPass.setString(1, username);
            checkPass.setString(2, pass);
            rs = checkPass.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.error("Failed to execute query", e);
        } finally {
            closeResultSet(rs);
        }
        return false;
    }

    public static synchronized boolean addNewUser(String username, String pass) {
        try {
            addUser.setString(1, username);
            addUser.setString(2, pass);
            int result = addUser.executeUpdate();
            if (result == 1) {
                connection.commit();
                log.debug("User {} was added", username);
                return true;
            } else {
                connection.rollback();
                log.debug("Failed to add user {}", username);
                return false;
            }
        } catch (SQLException e) {
            log.error("Failed to execute query", e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                log.error("Failed to rollback", e);
            }
        }
        return false;
    }

    public static synchronized boolean checkUser(String username) {
        ResultSet rs = null;
        try {
            checkDuplicate.setString(1, username);
            rs = checkDuplicate.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.error("Failed to execute query", e);
        } finally {
            closeResultSet(rs);
        }
        return false;
    }

    private static void closeResultSet(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            log.error("Unable to close ResultSet", e);
        }
    }

    private static void closePreparedStatements() {
        try {
            if (checkPass != null) {
                checkPass.close();
            }
        } catch (SQLException e) {
            log.error("Unable to close PreparedStatement", e);
        }
        try {
            if (addUser != null) {
                addUser.close();
            }
        } catch (SQLException e) {
            log.error("Unable to close PreparedStatement", e);
        }
        try {
            if (checkDuplicate != null) {
                checkDuplicate.close();
            }
        } catch (SQLException e) {
            log.error("Unable to close PreparedStatement", e);
        }
    }
}
