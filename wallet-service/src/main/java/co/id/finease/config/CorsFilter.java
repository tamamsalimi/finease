package co.id.finease.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

@Component
public class CorsFilter extends OncePerRequestFilter {

    @Value("${cors.allowedOrigins}")
    private List<String> allowedOrigins;

    @Value("${cors.allowedMethods}")
    private List<String> allowedMethods;

    @Value("${cors.allowedHeaders}")
    private List<String> allowedHeaders;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Get the CORS configuration for the current request
        CorsConfiguration corsConfiguration = corsConfigurationSource().getCorsConfiguration(request);

        // If the request is a pre-flight (OPTIONS) request, handle it
        if (CorsUtils.isPreFlightRequest(request)) {
            handlePreFlightRequest(request, response, corsConfiguration);
            return;
        }
        // Validate the request against the CORS configuration (Origin, Methods, Headers)
        if (!isRequestValid(request, corsConfiguration)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // Return 403 if CORS request is invalid
            response.getWriter().write("CORS request is not allowed");
            return;
        }

        // Process the actual request using the CORS processor
        filterChain.doFilter(request, response);
    }

    private void handlePreFlightRequest(HttpServletRequest request, HttpServletResponse response, CorsConfiguration corsConfiguration) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK); // Return 200 OK for OPTIONS request

        // Handle CORS headers for the pre-flight response
        response.addHeader("Access-Control-Allow-Origin", corsConfiguration.getAllowedOrigins().get(0)); // Allowed Origin
        response.addHeader("Access-Control-Allow-Methods", String.join(", ", corsConfiguration.getAllowedMethods())); // Allowed Methods
        response.addHeader("Access-Control-Allow-Headers", String.join(", ", corsConfiguration.getAllowedHeaders())); // Allowed Headers
        response.addHeader("Access-Control-Max-Age", "3600"); // Max Age for CORS pre-flight cache
    }

    private boolean isRequestValid(HttpServletRequest request, CorsConfiguration corsConfiguration) {
        // Validate Method (for non-OPTIONS requests)
        String method = request.getMethod();
        if (!corsConfiguration.getAllowedMethods().contains(method)) {
            return false; // Method not allowed
        }
        /*Enumeration<String> requestHeaderNames = request.getHeaderNames();
        while (requestHeaderNames.hasMoreElements()) {
            String headerName = requestHeaderNames.nextElement();
            if (!corsConfiguration.getAllowedHeaders().contains(headerName)) {
                return false;
            }
        }*/
        return true;
    }

    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Set allowed origins, methods, and headers from properties
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        // Register the configuration for all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
