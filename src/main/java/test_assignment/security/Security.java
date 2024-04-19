package test_assignment.security;

import rawhttp.core.RawHttpRequest;
import rawhttp.core.RawHttpResponse;

import java.util.function.Function;

@FunctionalInterface
public interface Security {
    RawHttpResponse<?> secure(
            final RawHttpRequest request,
            final Function<RawHttpRequest, RawHttpResponse<?>> fun
    );
}
