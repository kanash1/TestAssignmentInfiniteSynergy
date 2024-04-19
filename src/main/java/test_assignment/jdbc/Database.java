package test_assignment.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import test_assignment.exceptions.DatabaseConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Database {
    private final HikariDataSource dataSource;

    public Database() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try(final InputStream is = loader.getResourceAsStream("app.properties")) {
            final Properties props = new Properties();
            props.load(is);

            Class.forName(getDriver(props));

            final HikariConfig config = new HikariConfig();
            config.setJdbcUrl(getUrl(props));
            config.setUsername(getUsername(props));
            config.setPassword(getPassword(props));

            dataSource = new HikariDataSource(config);

        } catch (final IOException | ClassNotFoundException | NullPointerException e) {
            throw new DatabaseConfigurationException("Unable to configure database connection", e);
        }
    }

    public <R> R transaction(Function<Connection, R> statementFun) throws SQLException, RuntimeException {
        R result;
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            result = statementFun.apply(connection);
            connection.commit();
            return result;
        } catch (final SQLException | RuntimeException e) {
            if (connection != null)
                connection.rollback();
            throw e;
        } finally {
            if (connection != null)
                connection.close();
        }
    }

    public void transaction(Consumer<Connection> statementFun) throws SQLException, RuntimeException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            statementFun.accept(connection);
            connection.commit();
        } catch (final SQLException | RuntimeException e) {
            if (connection != null)
                connection.rollback();
            throw e;
        } finally {
            if (connection != null)
                connection.close();
        }
    }

    private String getDriver(final Properties props) {
        return props.getProperty("database.connection.driver");
    }

    private String getUrl(final Properties props) {
        return props.getProperty("database.connection.url");
    }

    private String getUsername(final Properties props) {
        return props.getProperty("database.connection.username");
    }

    private String getPassword(final Properties props) {
        return props.getProperty("database.connection.password");
    }
}
