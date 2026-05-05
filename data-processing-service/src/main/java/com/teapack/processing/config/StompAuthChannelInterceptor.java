package com.teapack.processing.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Slf4j
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing Authorization header on STOMP CONNECT");
            }
            try {
                SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(token.substring(7)).getPayload();
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                if (role == null) role = "ROLE_USER";
                var auth = new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority(role))
                );
                accessor.setUser(auth);
                log.debug("STOMP CONNECT authenticated: user={}, role={}", username, role);
            } catch (Exception e) {
                log.warn("STOMP CONNECT rejected: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid JWT on STOMP CONNECT", e);
            }
        }
        return message;
    }
}
