package com.oldbook.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private static final String LOAI_TOKEN = "loaiToken";
    private static final String TOKEN_CHON_VAI_TRO = "CHON_VAI_TRO";

    private final SecretKey secretKey;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );

        this.expiration = expiration;
    }

    // JWT chính thức
    public String generateToken(
            Integer maND,
            Integer maTK,
            String vaiTro
    ) {

        Date now = new Date();
        Date expirationDate = new Date(
                now.getTime() + expiration
        );

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(maND.toString())
                .claim("maND", maND)
                .claim("maTK", maTK)
                .claim("vaiTro", vaiTro)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }

    public String extractJti(String token) {
        return extractClaims(token)
                .getId();
    }

    public Date extractExpiration(String token) {
        return extractClaims(token)
                .getExpiration();
    }

    // JWT tạm, chỉ dùng trong bước chọn vai trò
    public String generateRoleSelectionToken(Integer maND) {

        Date now = new Date();

        // Token chọn vai trò chỉ tồn tại 5 phút
        Date expirationDate = new Date(
                now.getTime() + (5 * 60 * 1000)
        );

        return Jwts.builder()
                .subject(maND.toString())
                .claim("maND", maND)
                .claim(LOAI_TOKEN, TOKEN_CHON_VAI_TRO)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }

    public Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Integer extractUserId(String token) {
        return extractClaims(token)
                .get("maND", Integer.class);
    }

    public Integer extractAccountId(String token) {
        return extractClaims(token)
                .get("maTK", Integer.class);
    }

    public String extractRole(String token) {
        return extractClaims(token)
                .get("vaiTro", String.class);
    }

    public String extractTokenType(String token) {
        return extractClaims(token)
                .get(LOAI_TOKEN, String.class);
    }

    public boolean isTokenValid(String token) {

        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRoleSelectionToken(String token) {

        try {
            String loaiToken = extractTokenType(token);

            return TOKEN_CHON_VAI_TRO.equals(loaiToken);

        } catch (Exception e) {
            return false;
        }
    }
}