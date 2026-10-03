package com.oldbook.filter.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.oldbook.constant.auth.TrangThaiTaiKhoan;
import com.oldbook.constant.auth.VaiTro;
import com.oldbook.entity.identity.TaiKhoan;
import com.oldbook.repository.identity.TaiKhoanRepository;
import com.oldbook.service.auth.JwtService;
import com.oldbook.service.auth.RevokedTokenService;
import com.oldbook.service.auth.TaiKhoanTokenService;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RevokedTokenService revokedTokenService;
    private final TaiKhoanTokenService taiKhoanTokenService;
    private final TaiKhoanRepository taiKhoanRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {

            // 1. JWT còn hợp lệ về chữ ký + thời hạn
            if (!jwtService.isTokenValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtService.extractClaims(token);

            Integer maND = claims.get("maND", Integer.class);
            Integer maTK = claims.get("maTK", Integer.class);

            if (maND == null || maTK == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Token có bị logout hay chưa?
            String jti = claims.getId();

            if (jti == null
                    || revokedTokenService.isRevoked(jti)) {

                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // 3. Lấy thời điểm tạo token
            if (claims.getIssuedAt() == null) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            Instant issuedAt =
                    claims.getIssuedAt().toInstant();

            // 4. Token có bị vô hiệu do khóa tài khoản không?
            if (taiKhoanTokenService.isTokenInvalid(
                    maTK,
                    issuedAt
            )) {

                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // 5. Tìm tài khoản hiện tại trong DB
            TaiKhoan taiKhoan =
                    taiKhoanRepository.findById(maTK)
                            .orElse(null);

            if (taiKhoan == null) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // 6. Đảm bảo tài khoản thuộc đúng người dùng
            if (taiKhoan.getNguoiDung() == null
                    || !maND.equals(
                    taiKhoan.getNguoiDung().getMaND()
            )) {

                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // 7. Tài khoản phải đang hoạt động
            if (!TrangThaiTaiKhoan.HOAT_DONG.name()
                    .equals(taiKhoan.getTrangThai())) {

                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // 8. QUAN TRỌNG:
            // Không lấy vai trò từ JWT làm nguồn chính.
            // Lấy vai trò hiện tại từ database.
            String vaiTroHienTai =
                    taiKhoan.getVaiTro();

            VaiTro role =
                    VaiTro.valueOf(vaiTroHienTai);

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            role.getAuthority()
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            maND,
                            null,
                            List.of(authority)
                    );

            authentication.setDetails(
                    new AuthenticatedUserDetails(
                            maND,
                            maTK,
                            vaiTroHienTai
                    )
            );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

        } catch (Exception e) {

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    public record AuthenticatedUserDetails(
            Integer maND,
            Integer maTK,
            String vaiTro
    ) {
    }
}