package com.tychewealth.config;

import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;

import com.tychewealth.error.exception.AuthException;
import com.tychewealth.service.token.TokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that turns a valid Bearer token into a Spring authentication.
 *
 * <p>Runs once per request, delegates validation to {@code TokenValidator}, and stores the user
 * identifier in the security context before continuing the chain. Missing credentials are passed
 * through so the security chain can apply its rules, while invalid tokens are translated into an
 * authentication failure response.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final TokenValidator tokenValidator;
  private final AuthenticationEntryPoint authenticationEntryPoint;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      Long userId = tokenValidator.validateAndExtractUserId(authorizationHeader);
      UsernamePasswordAuthenticationToken authenticatedUser =
          new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(authenticatedUser);
      filterChain.doFilter(request, response);
    } catch (AuthException ex) {
      SecurityContextHolder.clearContext();
      authenticationEntryPoint.commence(
          request, response, new InsufficientAuthenticationException(ex.getMessage(), ex));
    }
  }
}
