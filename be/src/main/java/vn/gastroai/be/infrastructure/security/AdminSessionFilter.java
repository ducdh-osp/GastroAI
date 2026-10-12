package vn.gastroai.be.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.gastroai.be.application.auth.CmsAuthService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;
import java.util.List;

@Component
public class AdminSessionFilter extends OncePerRequestFilter {
        private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain)
                        throws ServletException, IOException {

                HttpSession session = request.getSession(false);

                if (session != null) {

                        Object userId = session.getAttribute(CmsAuthService.AUTH_USER_ID);

                        Object userType = session.getAttribute(CmsAuthService.AUTH_USER_TYPE);

                        if (userId != null && userType != null) {

                                String role = userType.toString();

                                if ("ADMIN".equals(role) || "DOCTOR".equals(role)) {

                                        var authority = new SimpleGrantedAuthority("ROLE_" + role);

                                        var authentication = new UsernamePasswordAuthenticationToken(
                                                        userId.toString(),
                                                        null,
                                                        List.of(authority));
                                        SecurityContext context = SecurityContextHolder.createEmptyContext();

                                        context.setAuthentication(authentication);

                                        SecurityContextHolder.setContext(context);

                                        if (!securityContextRepository.containsContext(request)) {
                                                securityContextRepository.saveContext(
                                                                context,
                                                                request,
                                                                response);
                                        }
                                }
                        }
                }

                filterChain.doFilter(request, response);
        }
}