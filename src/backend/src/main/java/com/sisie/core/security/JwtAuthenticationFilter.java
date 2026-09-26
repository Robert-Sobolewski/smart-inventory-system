package com.sisie.core.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider tokenProvider;

	/**
	 * Hilfe funktion um token auf Header zu extraktieren.
	 * 
	 * @param request
	 * @return JWT-Token
	 */
	private String resolveToken(HttpServletRequest request) {

		String bearerToken = request.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 1. Versuche den Token aus dem Header "Authorization: Bearer <token>" zu
		// lesen.
		String token = resolveToken(request);

		// 2. Wenn token ist da und ist gueltig
		if (token != null && tokenProvider.validateToken(token)) {

			String username = tokenProvider.getUsernameFromJWT(token);
                    String role = tokenProvider.getRoleFromJWT(token);
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

			// Wir müssen Spring Security sagen, wer der Nutzer ist.
			// Hier erstellen wir einen "Authentication"-Objekt für den aktuellen
			// Request-Kontext.
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null,
                            authorities); // In einer vollen Version würden wir hier die Rollen hinzufügen

			SecurityContextHolder.getContext().setAuthentication(authentication);

		}

		// 3. Gib den Request an den naechsten Filter in der Kette weiter
		filterChain.doFilter(request, response);
	}

}
