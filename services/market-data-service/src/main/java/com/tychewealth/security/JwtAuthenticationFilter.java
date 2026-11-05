package com.tychewealth.security;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.tychewealth.security.token.TokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates requests carrying user-service bearer tokens. */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final TokenValidator tokenValidator;
  private final AuthenticationEntryPoint authenticationEntryPoint;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String authorizationHeader = request.getHeader(AUTHORIZATION);
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
    } catch (AuthenticationException exception) {
      SecurityContextHolder.clearContext();
      authenticationEntryPoint.commence(
          request,
          response,
          new InsufficientAuthenticationException(exception.getMessage(), exception));
    }
  }
}
