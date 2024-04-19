package test_assignment.controllers;

import lombok.RequiredArgsConstructor;
import org.apache.hc.core5.http.HttpStatus;
import rawhttp.core.RawHttpRequest;
import rawhttp.core.RawHttpResponse;
import rawhttp.core.server.Router;
import test_assignment.security.Security;
import test_assignment.utils.HttpUtils;

import java.util.Optional;

@RequiredArgsConstructor
public final class UserRouter implements Router {
    private final UserController controller;
    private final Security security;
    private final HttpUtils httpUtils;

    @Override
    public Optional<RawHttpResponse<?>> route(final RawHttpRequest rawHttpRequest) {
        return switch (rawHttpRequest.getUri().getPath()) {
            case "/signin" -> {
                if (rawHttpRequest.getMethod().equals("POST"))
                    yield Optional.of(controller.signIn(rawHttpRequest));
                yield notFound();
            }
            case "/signup" -> {
                if (rawHttpRequest.getMethod().equals("POST"))
                    yield Optional.of(controller.signUp(rawHttpRequest));
                yield notFound();
            }
            case "/money" -> switch (rawHttpRequest.getMethod()) {
                case "GET" -> Optional.of(security.secure(rawHttpRequest, controller::showMoney));
                case "POST" -> Optional.of(security.secure(rawHttpRequest, controller::sendMoney));
                default -> notFound();
            };
            default -> notFound();
        };
    }

    private Optional<RawHttpResponse<?>> notFound() {
        return Optional.of(httpUtils.formEmptyResponse(HttpStatus.SC_NOT_FOUND));
    }
}
