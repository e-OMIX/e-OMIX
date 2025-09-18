package com.example.eomix.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * The type Cors config.
 */
@WebFilter("/*")
@Configuration
public class CorsConfig implements WebMvcConfigurer, Filter {
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * The constant CORS_ALLOWED_ORIGINS.
     * * This constant defines the allowed origins for CORS requests.
     * * It is used to specify which domains are allowed to access the resources of this application.
     *
     * @implNote The allowed origins are set to "<a href="http://localhost:4200">Localhost</a>".
     * @implSpec This means that requests from these origins will be allowed to access the resources of this application.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedHeaders("*").allowCredentials(true).maxAge(3600);

    }

    /**
     * Cors filter returns cors filter.
     * * This method creates a CorsFilter bean that is used to handle CORS requests.
     * * It configures the allowed origins, methods, headers, and credentials for CORS requests.
     *
     * @return the cors filter
     * @implNote The CorsFilter is registered for all paths ("/**") and allows requests from the specified origins with the specified methods and headers.
     * @implSpec This allows the frontend application running on these origins to access the resources of this application without CORS issues.
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * Configure the CORS filter.
     * * This method configures the CORS filter to allow requests from specific origins and methods.
     * * It sets the allowed origins, methods, headers, and credentials for CORS requests.
     *
     * @param servletRequest  the servlet request
     * @param servletResponse the servlet response
     * @param filterChain     the filter chain
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     * @implNote This method is called for every request to the application and sets the CORS headers in the response.
     * @implSpec The CORS headers are set to allow requests from "<a href="http://localhost:4200">Localhost</a>" with the specified methods and headers.
     * * * This allows the frontend application running on these origins to access the resources of this application without CORS issues.
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * Rest template rest template.
     *
     * @return the rest template
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
