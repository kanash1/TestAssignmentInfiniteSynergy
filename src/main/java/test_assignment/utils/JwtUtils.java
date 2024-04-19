package test_assignment.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import rawhttp.core.RawHttpRequest;
import test_assignment.exceptions.HeaderNotFoundException;
import test_assignment.exceptions.JwtNotFoundException;

import javax.crypto.SecretKey;
import java.net.http.HttpRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class JwtUtils {
    public String generateToken(final String secret, final Long expiration, final Map<String, Object> claims) {
        final Date issueDate = new Date();
        final Date expiredDate = new Date(issueDate.getTime() + expiration);
        return Jwts.builder()
                .claims(claims)
                .issuedAt(issueDate)
                .expiration(expiredDate)
                .signWith(key(secret), Jwts.SIG.HS256)
                .compact();
    }

    public Claims getClaimsFromToken(final String token, final String secret) throws RuntimeException {
        return Jwts.parser().verifyWith(key(secret)).build().parseSignedClaims(token).getPayload();
    }

    public void validateToken(final String token, final String secret) throws RuntimeException {
        Jwts.parser().verifyWith(key(secret)).build().parseSignedClaims(token);
    }

    public String extractTokenFromHeader(RawHttpRequest request)
            throws HeaderNotFoundException, JwtNotFoundException {
        final List<String> headersAuth = request.getHeaders().get("Authorization");
        if (headersAuth.isEmpty())
            throw new HeaderNotFoundException("Authorization header not found");
        final String headerAuth = headersAuth.get(0);
        if (headerAuth.isBlank() || !headerAuth.startsWith("Bearer ")) {
            throw new JwtNotFoundException("JWT not found in header");
        }
        return headerAuth.substring(7);
    }

    private SecretKey key(final String secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
