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
import vn.gastroai.be.domain.admin.CmsAccountType;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;
import java.util.List;

@Component
public class AdminSessionFilter extends OncePerRequestFilter {
        private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
        private final ObjectProvider<AdminRepository> admins;
        private final ObjectProvider<DoctorRepository> doctors;

        public AdminSessionFilter(
                        ObjectProvider<AdminRepository> admins,
                        ObjectProvider<DoctorRepository> doctors) {
                this.admins = admins;
                this.doctors = doctors;
        }

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

                        Object accountType = session.getAttribute(CmsAuthService.AUTH_ACCOUNT_TYPE);

                        if (userId != null && userType != null) {

                                String role = currentRole(userId, accountType, userType);

                                if (isCmsRole(role)) {

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

        private boolean isCmsRole(String role) {
                return role != null && ("ADMIN".equals(role) || "DOCTOR".equals(role)
                                || "CONTENT_EDITOR".equals(role));
        }

        /** Role duoc doc lai tu DB de thay doi RBAC co hieu luc o request ke tiep theo. */
        private String currentRole(Object userId, Object accountType, Object legacyRole) {
                if (accountType == null) {
                        return isCmsRole(legacyRole.toString()) ? legacyRole.toString() : null;
                }
                Long id;
                try {
                        id = Long.valueOf(userId.toString());
                } catch (NumberFormatException exception) {
                        return null;
                }
                if (CmsAccountType.ADMIN.name().equals(accountType.toString())) {
                        AdminRepository repository = admins.getIfAvailable();
                        return repository == null ? null : repository.findById(id)
                                        .map(admin -> admin.getRole().name()).orElse(null);
                }
                if (CmsAccountType.DOCTOR.name().equals(accountType.toString())) {
                        DoctorRepository repository = doctors.getIfAvailable();
                        return repository == null ? null : repository.findById(id)
                                        .map(doctor -> doctor.getRole().name()).orElse(null);
                }
                return null;
        }
}
