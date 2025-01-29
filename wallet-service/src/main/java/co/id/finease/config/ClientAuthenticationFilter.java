package co.id.finease.config;

import co.id.finease.entity.Client;
import co.id.finease.service.ClientService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Component
public class ClientAuthenticationFilter extends OncePerRequestFilter {

    private final ClientService clientService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public ClientAuthenticationFilter(ClientService clientService, HandlerExceptionResolver handlerExceptionResolver) {
        this.clientService = clientService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String applicationId = request.getHeader("application-id");
        final String apiKey = request.getHeader("api-key");

        // Check if credentials are missing or invalid
        if (isInvalidClientCredentials(applicationId, apiKey)) {
            SecurityContextHolder.getContext().setAuthentication(null);
            filterChain.doFilter(request, response);  // Continue with the filter chain without authentication
            return;
        }

        try {
            Client client = clientService.findByApplicationIdAndApiKey(applicationId, apiKey);
            if (client != null) {
                setAuthenticationForClient(request, client);
            } else {
                SecurityContextHolder.getContext().setAuthentication(null);
            }
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }

    private boolean isInvalidClientCredentials(String applicationId, String apiKey) {
        return applicationId == null || applicationId.isEmpty() || apiKey == null || apiKey.isEmpty();
    }


    private void setAuthenticationForClient(HttpServletRequest request, Client client) {
        request.setAttribute("client_id", client.getClientId());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                client, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
