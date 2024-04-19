package test_assignment.repositories;

import test_assignment.exceptions.sql.ConstraintViolationSQLException;
import test_assignment.exceptions.sql.NotFoundSQLException;
import test_assignment.jdbc.Database;
import test_assignment.models.User;
import test_assignment.exceptions.sql.CustomSQLException;

import java.math.BigDecimal;
import java.sql.*;

public final class UserRepository implements IUserRepository {
    private final Database db;

    public UserRepository(final Database db) {
        this.db = db;
    }

    public String findUserPasswordByLogin(final String login) throws CustomSQLException {
        final String query = "select password from users where login = ?";
        try {
            return db.transaction((Connection con) -> {
                try (final PreparedStatement ps = con.prepareStatement(query)) {
                    con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    ps.setString(1, login);
                    final ResultSet rs = ps.executeQuery();
                    if (rs.next())
                        return rs.getString(1);
                    else
                        throw new NotFoundSQLException("Not found user with login " + login);
                } catch (final SQLException e) {
                    throw new NotFoundSQLException("Not found password field", e);
                }
            });
        } catch (final SQLException e) {
            throw new CustomSQLException("Unexpected sql exception", e);
        }
    }

    public void addUser(final User user) throws CustomSQLException {
        final String query = "insert into users(login, password) values(?, ?)";
        try {
            db.transaction((Connection con) -> {
                try (final PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    ps.setString(1, user.getLogin());
                    ps.setString(2, user.getPassword());
                    ps.executeUpdate();
                    final ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    user.setId(rs.getInt(1));
                } catch (final SQLException e) {
                    throw new ConstraintViolationSQLException("User with login " + user.getLogin() + " already exist", e);
                }
            });
        } catch (final SQLException e) {
            throw new CustomSQLException("Unexpected sql exception", e);
        }
    }

    public BigDecimal findUserMoneyByLogin(final String login) throws CustomSQLException {
        final String query = "select money from users where login = ?";
        try {
            return db.transaction((Connection con) -> {
                try (final PreparedStatement ps = con.prepareStatement(query)) {
                    con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    ps.setString(1, login);
                    final ResultSet rs = ps.executeQuery();
                    if (rs.next())
                        return rs.getBigDecimal(1);
                    else
                        throw new NotFoundSQLException("Not found user with login " + login);
                } catch (final SQLException e) {
                    throw new NotFoundSQLException("Cannot find money field", e);
                }
            });
        } catch (final SQLException e) {
            throw new CustomSQLException("Unexpected sql exception", e);
        }
    }

    public void sendMoneyFromOneUserToOther(
            final String senderLogin, final String recipientLogin, final BigDecimal amount
    ) throws CustomSQLException {
        final String senderQuery = "update users set money = money - ? where login = ?";
        final String recipientQuery = "update users set money = money + ? where login = ?";
        try {
            db.transaction((Connection con) -> {
                try (final PreparedStatement psSender = con.prepareStatement(senderQuery);
                     final PreparedStatement psRecipient = con.prepareStatement(recipientQuery)
                ) {
                    con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                    psSender.setBigDecimal(1, amount);
                    psSender.setString(2, senderLogin);
                    if (psSender.executeUpdate() == 0)
                        throw new NotFoundSQLException("Not found user with login " + senderLogin);

                    psRecipient.setBigDecimal(1, amount);
                    psRecipient.setString(2, recipientLogin);
                    if (psRecipient.executeUpdate() == 0)
                        throw new NotFoundSQLException("Not found user with login " + recipientLogin);

                } catch (final SQLException e) {
                    throw new ConstraintViolationSQLException("Cannot send money", e);
                }
            });
        } catch (final SQLException e) {
            throw new CustomSQLException("Unexpected sql exception");
        }
    }
}
