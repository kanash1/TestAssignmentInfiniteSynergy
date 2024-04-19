package test_assignment.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;
import rawhttp.core.*;
import rawhttp.core.body.BodyReader;
import test_assignment.exceptions.ResourceExtractionException;
import test_assignment.exceptions.sql.ConstraintViolationSQLException;
import test_assignment.exceptions.sql.NotFoundSQLException;
import test_assignment.requests.SendMoneyRequest;
import test_assignment.models.User;
import test_assignment.exceptions.sql.CustomSQLException;
import test_assignment.repositories.IUserRepository;
import test_assignment.requests.SignInRequest;
import test_assignment.requests.SignUpRequest;
import test_assignment.utils.HttpUtils;
import test_assignment.utils.JwtUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public final class UserController {
    private final ObjectMapper mapper;
    private final IUserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final String jwtSecret;
    private final Long jwtExpiration;
    private final HttpUtils httpUtils;

    public UserController(
            final IUserRepository userRepository,
            final JwtUtils jwtUtils,
            final HttpUtils httpUtils
    ) {
        mapper = new ObjectMapper();
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.httpUtils = httpUtils;

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try(final InputStream is = loader.getResourceAsStream("app.properties")) {
            final Properties props = new Properties();
            props.load(is);
            jwtSecret = getJwtSecret(props);
            jwtExpiration = getJwtExpiration(props);
        } catch (IOException | NumberFormatException e) {
            throw new ResourceExtractionException("Unable to configure controller", e);
        }
    }

    public RawHttpResponse<?> signIn(final RawHttpRequest httpRequest) {
        try {
            final BodyReader bodyReader = httpRequest.getBody()
                    .orElseThrow(() -> new IllegalArgumentException("Unexpected body"));
            final SignInRequest signInRequest = mapper.readValue(
                    bodyReader.asRawString(StandardCharsets.UTF_8),
                    SignInRequest.class);

            final String encryptedPassword = userRepository.findUserPasswordByLogin(signInRequest.getLogin());
            if (!encryptedPassword.equals(encrypt(signInRequest.getPassword())))
                throw new IllegalArgumentException("Wrong password");

            final HashMap<String, List<String>> headers = new HashMap<>();
            final List<String> values = new ArrayList<>();
            values.add("text/plain");
            headers.put("Content-Type", values);

            final HashMap<String, Object> claims = new HashMap<>();
            claims.put("sub", signInRequest.getLogin());

            log.info("User {} is logged in", signInRequest.getLogin());

            return httpUtils.formResponse(
                    HttpStatus.SC_OK,
                    headers,
                    jwtUtils.generateToken(jwtSecret, jwtExpiration, claims)
            );
        } catch (final NotFoundSQLException | IllegalArgumentException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_UNAUTHORIZED);
        } catch (final IOException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        } catch (final CustomSQLException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public RawHttpResponse<?> signUp(final RawHttpRequest httpRequest) {
        try {
            final BodyReader bodyReader = httpRequest.getBody()
                    .orElseThrow(() -> new IllegalArgumentException("Unexpected body"));
            final SignUpRequest signUpRequest = mapper.readValue(
                    bodyReader.asRawString(StandardCharsets.UTF_8),
                    SignUpRequest.class);

            final User user = User.builder()
                    .login(signUpRequest.getLogin())
                    .password(encrypt(signUpRequest.getPassword()))
                    .build();
            userRepository.addUser(user);

            log.info("User {} registered", signUpRequest.getLogin());

            return httpUtils.formEmptyResponse(HttpStatus.SC_OK);

        } catch (final ConstraintViolationSQLException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_CONFLICT);
        } catch (final IOException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        }catch (final IllegalArgumentException | CustomSQLException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public RawHttpResponse<?> showMoney(final RawHttpRequest httpRequest) {
        try {
            final String login = extractUserLogin(httpRequest);
            final BigDecimal money = userRepository.findUserMoneyByLogin(login);

            final HashMap<String, List<String>> headers = new HashMap<>();
            final List<String> values = new ArrayList<>();
            values.add("text/plain");
            headers.put("Content-Type", values);

            log.info("User {} balance is {}$", login, money);

            return httpUtils.formResponse(
                    HttpStatus.SC_OK,
                    headers,
                    money.toString()
            );

        } catch (final NotFoundSQLException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_NOT_FOUND);
        } catch (final CustomSQLException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (final RuntimeException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_UNAUTHORIZED);
        }
    }

    public RawHttpResponse<?> sendMoney(final RawHttpRequest httpRequest) {
        try {
            final BodyReader bodyReader = httpRequest.getBody()
                    .orElseThrow(() -> new IllegalArgumentException("Unexpected body"));
            final SendMoneyRequest sendMoneyRequest = mapper.readValue(
                    bodyReader.asRawString(StandardCharsets.UTF_8),
                    SendMoneyRequest.class);

            final String senderLogin = extractUserLogin(httpRequest);

            userRepository.sendMoneyFromOneUserToOther(
                    senderLogin,
                    sendMoneyRequest.getRecipientLogin(),
                    sendMoneyRequest.getAmount()
            );

            log.info("User {} was send {}$ to the user {}",
                    senderLogin,
                    sendMoneyRequest.getAmount(),
                    sendMoneyRequest.getRecipientLogin());

            return httpUtils.formEmptyResponse(HttpStatus.SC_OK);

        } catch (final NotFoundSQLException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_NOT_FOUND);
        } catch (final IOException | ConstraintViolationSQLException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        } catch (final IllegalArgumentException | CustomSQLException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (final RuntimeException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_UNAUTHORIZED);
        }
    }

    private String extractUserLogin(final RawHttpRequest httpRequest) {
        final String token = jwtUtils.extractTokenFromHeader(httpRequest);
        return jwtUtils.getClaimsFromToken(token, jwtSecret).getSubject();
    }

    private String getJwtSecret(final Properties props) {
        return props.getProperty("security.jwt.secret");
    }

    private Long getJwtExpiration(final Properties props) throws NumberFormatException {
        return Long.parseLong(props.getProperty("security.jwt.expiration"));
    }

    private String encrypt(String str) {
        return Hashing.sha256().hashString(str, StandardCharsets.UTF_8).toString();
    }
}
