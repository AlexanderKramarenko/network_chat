package ru.alexander_kramarenko.march.chat.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DbAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LogManager.getLogger(DbAuthenticationProvider.class);

    private DbConnection dbConnection;

    @Override
    public void init() {
        dbConnection = new DbConnection();
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        String query = String.format("select nickname from users where login = '%s' and password = '%s';", login, password);
        try (ResultSet rs = dbConnection.getStmt().executeQuery(query)) {
            if (rs.next()) {
                return rs.getString("nickname");
            }
        } catch (SQLException e) {
            log.error("Ошибка в SQL выражении select nickname from users where login =" + login + " and password =" + password + ";");
            log.throwing(Level.ERROR, e);
        }
        return null;
    }

    @Override
    public void changeNickname(String oldNickname, String newNickname) {
        String query = String.format("update users set nickname = '%s' where nickname = '%s';", newNickname, oldNickname);
        try {
            // todo есть опасность наткнуться на не уникальный ник
            dbConnection.getStmt().executeUpdate(query);
        } catch (SQLException e) {
            log.error("Ошибка в SQL выражении update users set nickname = " + newNickname + " where nickname = " + oldNickname + ";");
            log.throwing(Level.ERROR, e);
        }
    }

    @Override
    public boolean isNickBusy(String nickname) {
        String query = String.format("select id from users where nickname = '%s';", nickname);
        try (ResultSet rs = dbConnection.getStmt().executeQuery(query)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
        }
        return false;
    }

    @Override
    public void shutdown() {
        dbConnection.close();
    }
}
