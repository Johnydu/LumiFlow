package br.com.lumiflow.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        boolean podeAcessarDashboard = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_PCP") || role.equals("ROLE_GESTAO") ||
                        role.equals("ROLE_ADMIN")
                        || role.equals("ROLE_SUPERVISOR"));

        String destino = podeAcessarDashboard ? "/dashboard" : "/dashboard/ordens-setor";

        response.sendRedirect(destino);
    }
}