package test_assignment.repositories;

import com.google.common.hash.Hashing;
import lombok.Getter;
import test_assignment.exceptions.sql.ConstraintViolationSQLException;
import test_assignment.exceptions.sql.CustomSQLException;
import test_assignment.exceptions.sql.NotFoundSQLException;
import test_assignment.models.User;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Getter
public final class MockUserRepository implements IUserRepository {
    private List<User> getUsers() {
        final List<User> users = new ArrayList<>();
        users.add(new User(1, "user1", encrypt("user1"), new BigDecimal(1000)));
        users.add(new User(2, "user2", encrypt("user2"), new BigDecimal(500)));
        return users;
    }

    @Override
    public String findUserPasswordByLogin(final String login) throws CustomSQLException {
        return getUsers().stream()
                .filter(u -> login.equals(u.getLogin()))
                .findFirst()
                .map(User::getPassword)
                .orElseThrow(() -> new NotFoundSQLException("Not found user with login " + login));
    }

    @Override
    public void addUser(final User user) throws CustomSQLException {
        final List<User> users = getUsers();
        if (users.stream()
                .filter(u -> u.getLogin().equals(user.getLogin()))
                .findFirst()
                .isEmpty()) {
            user.setId(users.stream()
                    .map(User::getId)
                    .max(Integer::compare)
                    .orElse(0) + 1);
            users.add(user);
        } else
            throw new ConstraintViolationSQLException("User with login " + user.getLogin() + " already exist");
    }

    @Override
    public BigDecimal findUserMoneyByLogin(final String login) throws CustomSQLException {
        return getUsers().stream()
                .filter(u -> login.equals(u.getLogin()))
                .findFirst()
                .map(User::getMoney)
                .orElseThrow(() -> new NotFoundSQLException("Not found user with login " + login));
    }

    @Override
    public void sendMoneyFromOneUserToOther(
            final String senderLogin,
            final String recipientLogin,
            final BigDecimal amount
    ) throws CustomSQLException {
        final List<User> users = getUsers();
        final User sender = users.stream()
                .filter(u -> u.getLogin().equals(senderLogin))
                .findFirst()
                .orElseThrow(() -> new NotFoundSQLException("Not found user with login " + senderLogin));
        sender.setMoney(sender.getMoney().subtract(amount));

        final User recipient = users.stream()
                .filter(u -> u.getLogin().equals(recipientLogin))
                .findFirst()
                .orElseThrow(() -> new NotFoundSQLException("Not found user with login " + recipientLogin));

        if (sender.getMoney().compareTo(amount) < 0)
            throw new ConstraintViolationSQLException("Cannot send money");

        recipient.setMoney(recipient.getMoney().add(amount));
    }

    private String encrypt(String str) {
        return Hashing.sha256().hashString(str, StandardCharsets.UTF_8).toString();
    }
}
