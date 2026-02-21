package com.revpay.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger slf4jLogger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${revpay.app.jwtSecret}")
    private String jwtSecret;

    @Value("${revpay.app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateTokenFromUsername(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            slf4jLogger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            slf4jLogger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            slf4jLogger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            slf4jLogger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            slf4jLogger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}