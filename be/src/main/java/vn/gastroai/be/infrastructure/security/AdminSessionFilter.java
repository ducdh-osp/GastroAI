package vn.gastroai.be.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class AdminSessionFilter extends OncePerRequestFilter {

    private static final Set<String> CMS_ROLES = Set.of("ADMIN", "CONTENT_EDITOR");

    private final ObjectProvider<AdminRepository> admins;

    public AdminSessionFilter(ObjectProvider<AdminRepository> admins) {
        this.admins = admins;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Long accountId = CmsAuthService.sessionUserId(session);

        if (accountId != null) {
            String role = currentRole(accountId, session);

            if (role != null) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        accountId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } else {
                // Phiên CMS không còn hiệu lực: bỏ mọi quyền còn sót lại, kể cả quyền phiên bản cũ
                // của bộ lọc này từng lưu vào phiên.
                SecurityContextHolder.clearContext();
                session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String currentRole(Long accountId, HttpSession session) {
        AdminRepository repository = admins.getIfAvailable();
        if (repository == null) {
            return null;
        }
        return repository.findById(accountId)
                .filter(account -> CmsAuthService.isCurrentSession(account, session))
                .map(account -> account.getRole().name())
                .filter(CMS_ROLES::contains)
                .orElse(null);
    }
}