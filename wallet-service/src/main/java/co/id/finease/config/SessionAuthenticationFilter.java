package co.id.finease.config;

import co.id.finease.entity.Account;
import co.id.finease.entity.Session;
import co.id.finease.service.AccountService;
import co.id.finease.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionService sessionService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    private final AccountService accountService;

    public SessionAuthenticationFilter(SessionService sessionService, HandlerExceptionResolver handlerExceptionResolver, AccountService accountService) {
        this.sessionService = sessionService;
        this.handlerExceptionResolver = handlerExceptionResolver;
        this.accountService = accountService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String applicationId = request.getHeader("application-id");
        final String apiKey = request.getHeader("api-key");
        final String accountId = request.getHeader("account-id");

        // Check if credentials are invalid or missing for "v2" requests
        if (isInvalidSessionCredentials(applicationId, apiKey) ||
                (request.getRequestURI().contains("/v2") && (accountId == null || accountId.isEmpty()))) {
            // Remove authentication from the security context
            SecurityContextHolder.getContext().setAuthentication(null);
            // Continue with the filter chain without authentication
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Session session = sessionService.findByApplicationIdAndApiKey(applicationId, apiKey);
            if (null != session) {
                Collection<GrantedAuthority> authorities = Collections.emptyList();
                SessionAuthenticationToken authentication = new SessionAuthenticationToken(session, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (!Objects.equals(accountId, "") && accountId != null){
                    Optional<Account> account = accountService.getAccountRef(accountId);
                    if (!account.isPresent()){
                        SecurityContextHolder.getContext().setAuthentication(null);
                        filterChain.doFilter(request, response);
                        return;
                    }
                    authentication.setDetails(account.get());
                }


            }
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            handlerExceptionResolver.resolveException(request, response, new Object(), ex);
        }
    }

    private boolean isInvalidSessionCredentials(String applicationId, String apiKey) {
        return applicationId == null || applicationId.isEmpty() || apiKey == null || apiKey.isEmpty();
    }
}
