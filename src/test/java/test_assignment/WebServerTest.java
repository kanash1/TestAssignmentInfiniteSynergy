package test_assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.junit.jupiter.api.*;
import test_assignment.controllers.UserController;
import test_assignment.controllers.UserRouter;
import test_assignment.exceptions.ResourceExtractionException;
import test_assignment.repositories.IUserRepository;
import test_assignment.repositories.MockUserRepository;
import test_assignment.requests.SendMoneyRequest;
import test_assignment.requests.SignInRequest;
import test_assignment.requests.SignUpRequest;
import test_assignment.security.JwtSecurity;
import test_assignment.security.Security;
import test_assignment.utils.HttpUtils;
import test_assignment.utils.JwtUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class WebServerTest {
    private static WebServer server;
    private final ObjectMapper mapper;
    private final String jwtSecret;

    private final static String BASE_URL = "http://localhost:8096";

    public WebServerTest() {
        mapper = new ObjectMapper();
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try(final InputStream is = loader.getResourceAsStream("app.properties")) {
            final Properties props = new Properties();
            props.load(is);
            jwtSecret = props.getProperty("security.jwt.secret");
        } catch (IOException e) {
            throw new ResourceExtractionException("Unable to get jwt secret", e);
        }
    }

    @BeforeAll
    public static void startServer() {
        final JwtUtils jwtUtils = new JwtUtils();
        final HttpUtils httpUtils = new HttpUtils();
        final Security jwtSecurity = new JwtSecurity(jwtUtils, httpUtils);
        final IUserRepository userRepository = new MockUserRepository();
        final UserController userController = new UserController(userRepository, jwtUtils, httpUtils);
        final UserRouter userRouter = new UserRouter(userController, jwtSecurity, httpUtils);

        server = new WebServer();
        server.start(userRouter);
    }

    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    @Nested
    @DisplayName("POST signup")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public class PostSignUp {

        @Test
        @DisplayName("should sign up user")
        public void shouldSignUpUser() throws Exception {
            imitateClient(client -> {
                final ClassicHttpRequest httpRequest = ClassicRequestBuilder.post()
                        .setUri(URI.create(BASE_URL + "/signup"))
                        .setEntity(mapper.writeValueAsString(
                                new SignUpRequest("test", "test")))
                        .build();
                manageResponse(client::execute, httpRequest, response ->
                        Assertions.assertEquals(HttpStatus.SC_OK, response.getCode()));
            });
        }

        @Test
        @DisplayName("should return CONFLICT")
        public void shouldReturnConflictIfLoginIsAlreadyTaken() throws Exception {
            imitateClient(client -> {
                final ClassicHttpRequest httpRequest = ClassicRequestBuilder.post()
                        .setUri(URI.create(BASE_URL + "/signup"))
                        .setEntity(mapper.writeValueAsString(
                                new SignUpRequest("user1", "user1")))
                        .build();
                manageResponse(client::execute, httpRequest, response ->
                        Assertions.assertEquals(HttpStatus.SC_CONFLICT, response.getCode()));
            });
        }
    }

    @Nested
    @DisplayName("POST signin")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public class PostSignIn {

        @Test
        @DisplayName("should return JWT")
        public void shouldReturnJwt() throws Exception {
            imitateClient(client -> {
                final JwtUtils jwtUtils = new JwtUtils();
                final ClassicHttpRequest httpRequest = ClassicRequestBuilder.post()
                        .setUri(URI.create(BASE_URL + "/signin"))
                        .setEntity(mapper.writeValueAsString(
                                new SignInRequest("user1", "user1")))
                        .build();
                manageResponse(client::execute, httpRequest, response -> {
                    Assertions.assertEquals(HttpStatus.SC_OK, response.getCode());
                    Assertions.assertDoesNotThrow(() ->
                            jwtUtils.validateToken(EntityUtils.toString(response.getEntity()), jwtSecret));
                });
            });
        }

        @Test
        @DisplayName("should return UNAUTHORIZED")
        public void shouldReturnUnauthorized() throws Exception {
            imitateClient(client -> {
                final ClassicHttpRequest httpRequest = ClassicRequestBuilder.post()
                        .setUri(URI.create(BASE_URL + "/signin"))
                        .setEntity(mapper.writeValueAsString(
                                new SignInRequest("unknown", "unknown")))
                        .build();
                manageResponse(client::execute, httpRequest, response ->
                        Assertions.assertEquals(response.getCode(), HttpStatus.SC_UNAUTHORIZED));
            });
        }
    }

    @Nested
    @DisplayName("GET money")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public class GetMoney {

        @Test
        @DisplayName("should return user balance")
        public void shouldReturnUserBalance() throws Exception {
            imitateClient(client -> {
                final ClassicHttpRequest signInRequest = ClassicRequestBuilder.post()
                        .setUri(URI.create(BASE_URL + "/signin"))
                        .setEntity(mapper.writeValueAsString(
                                new SignInRequest("user1", "user1")))
                        .build();
                manageResponse(client::execute, signInRequest, signInResponse -> {
                    final String jwt = EntityUtils.toString(signInResponse.getEntity());
                    final ClassicHttpRequest showBalanceRequest = ClassicRequestBuilder.get()
                            .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                            .setUri(URI.create(BASE_URL + "/money"))
                            .build();
                    manageResponse(client::execute, showBalanceRequest, response -> {
                        Assertions.assertEquals(HttpStatus.SC_OK, response.getCode());
                        Assertions.assertEquals(EntityUtils.toString(response.getEntity()), "1000");
                    });
                });
            });
        }

        @Test
        @DisplayName("should return UNAUTHORIZED")
        public void shouldReturnUnauthorized() throws Exception {
            imitateClient(client -> {
                final ClassicHttpRequest request = ClassicRequestBuilder.get()
                        .setHeader(HttpHeaders.AUTHORIZATION, "not jwt")
                        .setUri(URI.create(BASE_URL + "/money"))
                        .build();
                manageResponse(client::execute, request, response ->
                    Assertions.assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getCode()));
            });
        }

        @Test
        @DisplayName("should return NOT FOUND")
        void shouldReturnNotFound() throws Exception {
            final JwtUtils jwtUtils = new JwtUtils();
            final Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "deleted_user");
            final String jwt = jwtUtils.generateToken(jwtSecret, 10000L, claims);

            imitateClient(client -> {
                final ClassicHttpRequest request = ClassicRequestBuilder.get()
                        .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .setUri(URI.create(BASE_URL + "/money"))
                        .build();
                manageResponse(client::execute, request, response ->
                        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, response.getCode()));
            });
        }
    }

    @Nested
    @DisplayName("POST money")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class PostMoney {

        @Test
        @DisplayName("should send money from one user to another")
        public void shouldSendMoneyFromOneUserToAnother() throws Exception {
            imitateClient(client -> {
                final ClassicHttpRequest signInRequest = ClassicRequestBuilder.post()
                        .setUri(URI.create(BASE_URL + "/signin"))
                        .setEntity(mapper.writeValueAsString(
                                new SignInRequest("user1", "user1")))
                        .build();
                manageResponse(client::execute, signInRequest, signInResponse -> {
                    final String jwt = EntityUtils.toString(signInResponse.getEntity());
                    final ClassicHttpRequest sendRequest = ClassicRequestBuilder.post()
                            .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " +  jwt)
                            .setUri(URI.create(BASE_URL + "/money"))
                            .setEntity(mapper.writeValueAsString(
                                    new SendMoneyRequest("user2", new BigDecimal(100))))
                            .build();
                    manageResponse(client::execute, sendRequest, response ->
                        Assertions.assertEquals(HttpStatus.SC_OK, response.getCode()));
                });
            });
        }

        @Test
        @DisplayName("should return UNAUTHORIZED")
        public void shouldReturnUnauthorized() throws Exception {
            imitateClient(client -> {
                final ClassicHttpRequest request = ClassicRequestBuilder.post()
                        .setHeader(HttpHeaders.AUTHORIZATION, "not jwt")
                        .setUri(URI.create(BASE_URL + "/money"))
                        .build();
                manageResponse(client::execute, request, response ->
                    Assertions.assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getCode()));
            });
        }

        @Test
        @DisplayName("should return NOT FOUND")
        public void shouldReturnNotFound() throws Exception {
            imitateClient(client -> {
                final ClassicHttpRequest signInRequest = ClassicRequestBuilder.post()
                        .setUri(URI.create(BASE_URL + "/signin"))
                        .setEntity(mapper.writeValueAsString(
                                new SignInRequest("user1", "user1")))
                        .build();
                manageResponse(client::execute, signInRequest, signInResponse -> {
                    final String jwt = EntityUtils.toString(signInResponse.getEntity());
                    final ClassicHttpRequest sendRequest = ClassicRequestBuilder.post()
                            .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                            .setUri(URI.create(BASE_URL + "/money"))
                            .setEntity(mapper.writeValueAsString(
                                    new SendMoneyRequest("unknow", new BigDecimal(100))))
                            .build();
                    manageResponse(client::execute, sendRequest, response ->
                        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, response.getCode()));
                });
            });
        }
        
        @Test
        @DisplayName("should return UNPROCESSABLE CONTENT")
        void shouldReturnUnprocessableEntity() throws Exception {
            imitateClient(client -> {
                final ClassicHttpRequest signInRequest = ClassicRequestBuilder.post()
                        .setUri(URI.create(BASE_URL + "/signin"))
                        .setEntity(mapper.writeValueAsString(
                                new SignInRequest("user1", "user1")))
                        .build();
                manageResponse(client::execute, signInRequest, signInResponse -> {
                    final String jwt = EntityUtils.toString(signInResponse.getEntity());
                    final ClassicHttpRequest sendRequest = ClassicRequestBuilder.post()
                            .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " +  jwt)
                            .setUri(URI.create(BASE_URL + "/money"))
                            .setEntity(mapper.writeValueAsString(
                                    new SendMoneyRequest("user2", new BigDecimal(10000))))
                            .build();
                    manageResponse(client::execute, sendRequest, response ->
                            Assertions.assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getCode()));
                });
            });
        }
    }

    private void imitateClient(final Handler<CloseableHttpClient> clientHandler) throws Exception {
        try(final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            clientHandler.handle(httpClient);
        }
    }

    private void manageResponse(
            final Executor<ClassicHttpRequest, CloseableHttpResponse> executor,
            final ClassicHttpRequest request,
            final Handler<CloseableHttpResponse> handler) throws Exception {
        try(final CloseableHttpResponse response = executor.execute(request)) {
            handler.handle(response);
        }
    }

    @FunctionalInterface
    private interface Handler<T> {
        void handle(final T arg) throws Exception;
    }

    @FunctionalInterface
    private interface Executor<T, R> {
        R execute(final T arg) throws Exception;
    }
}