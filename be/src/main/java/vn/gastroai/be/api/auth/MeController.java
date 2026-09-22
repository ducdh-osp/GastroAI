package vn.gastroai.be.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.auth.*;
import vn.gastroai.be.application.readmodel.LoginHistoryItem;

@RestController @RequestMapping("/api/v1/me")
public class MeController {
 private final AuthService auth; private final AdminLoginService admins;
 public MeController(AuthService a,AdminLoginService ad){auth=a;admins=ad;}
 @GetMapping("/login-history") public LoginHistoryResponse history(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request,java.security.Principal principal, Authentication authentication){if(page<0||size<1||size>100)throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");Pageable pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"attemptedAt"));Object adminId=request.getSession(false)==null?null:request.getSession(false).getAttribute("ADMIN_ID");boolean adminAuthentication=authentication!=null&&authentication.getAuthorities().stream().anyMatch(a->"ROLE_ADMIN".equals(a.getAuthority()));if(adminId!=null&&adminAuthentication){var result=admins.history(Long.valueOf(adminId.toString()),pageable).map(x->new LoginHistoryItem(x.getId(),x.getAttemptedAt(),x.getOutcome().name(),x.getFailureReason(),x.getIpAddress(),x.getUserAgent(),x.getDeviceLabel()));return response(result);}var result=auth.history(Long.valueOf(principal.getName()),pageable).map(x->new LoginHistoryItem(x.getId(),x.getAttemptedAt(),x.getOutcome().name(),x.getFailureReason(),x.getIpAddress(),x.getUserAgent(),x.getDeviceLabel()));return response(result);}
 private LoginHistoryResponse response(Page<LoginHistoryItem> p){return new LoginHistoryResponse(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());}
}
