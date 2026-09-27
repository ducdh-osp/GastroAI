package vn.gastroai.be.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.gastroai.be.application.auth.CmsAuthService;

import java.io.IOException;
import java.util.List;

public class AdminSessionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {

            Object userId =
                    session.getAttribute(CmsAuthService.AUTH_USER_ID);

            Object userType =
                    session.getAttribute(CmsAuthService.AUTH_USER_TYPE);

            if (userId != null && userType != null) {

                String role = userType.toString();

                if ("ADMIN".equals(role) || "DOCTOR".equals(role)) {

                    var authority =
                            new SimpleGrantedAuthority("ROLE_" + role);

                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId.toString(),
                                    null,
                                    List.of(authority));

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}