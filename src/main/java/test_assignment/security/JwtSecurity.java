package test_assignment.security;

import io.jsonwebtoken.JwtException;
import lombok.Getter;
import org.apache.hc.core5.http.HttpStatus;
import rawhttp.core.RawHttpRequest;
import rawhttp.core.RawHttpResponse;
import test_assignment.exceptions.HeaderNotFoundException;
import test_assignment.exceptions.JwtNotFoundException;
import test_assignment.exceptions.ResourceExtractionException;
import test_assignment.utils.HttpUtils;
import test_assignment.utils.JwtUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Function;

@Getter
public final class JwtSecurity implements Security {
    private final HttpUtils httpUtils;
    private final JwtUtils jwtUtils;
    private final String jwtSecret;

    public JwtSecurity(final JwtUtils jwtUtils, final HttpUtils httpUtils) {
        this.httpUtils = httpUtils;
        this.jwtUtils = jwtUtils;

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try(final InputStream is = loader.getResourceAsStream("app.properties")) {
            final Properties props = new Properties();
            props.load(is);
            jwtSecret = getJwtSecret(props);
        } catch (IOException e) {
            throw new ResourceExtractionException("Unable to configure security", e);
        }
    }

    private String getJwtSecret(final Properties props) {
        return props.getProperty("security.jwt.secret");
    }

    @Override
    public RawHttpResponse<?> secure(
            final RawHttpRequest request,
            final Function<RawHttpRequest, RawHttpResponse<?>> fun
    ) {
        try {
            final String token = jwtUtils.extractTokenFromHeader(request);
            jwtUtils.validateToken(token, jwtSecret);
            return fun.apply(request);
        } catch (final HeaderNotFoundException | JwtNotFoundException | JwtException e) {
            return httpUtils.formEmptyResponse(HttpStatus.SC_UNAUTHORIZED);
        }
    }
}
