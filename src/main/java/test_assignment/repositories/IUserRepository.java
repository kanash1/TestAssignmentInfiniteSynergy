package test_assignment.repositories;

import test_assignment.models.User;
import test_assignment.exceptions.sql.CustomSQLException;

import java.math.BigDecimal;

public interface IUserRepository {
    String findUserPasswordByLogin(final String login) throws CustomSQLException;
    void addUser(final User user) throws CustomSQLException;
    BigDecimal findUserMoneyByLogin(final String login) throws CustomSQLException;
    void sendMoneyFromOneUserToOther(
            final String senderLogin, final String recipientLogin, final BigDecimal amount
    ) throws CustomSQLException;
}
