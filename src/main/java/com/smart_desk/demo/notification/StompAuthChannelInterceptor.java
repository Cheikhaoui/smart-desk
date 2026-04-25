package com.smart_desk.demo.notification;

import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.repositories.UserRepository;
import com.smart_desk.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String header = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("Missing or malformed Authorization header on STOMP CONNECT");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        String email;
        try {
            email = jwtService.extractUsername(token);
        } catch (Exception e) {
            throw new MessagingException("Invalid or expired JWT on STOMP CONNECT");
        }
        if (email == null) {
            throw new MessagingException("Invalid JWT on STOMP CONNECT: no subject");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MessagingException("Unknown user on STOMP CONNECT: " + email));

        if (!jwtService.isValid(token, user)) {
            throw new MessagingException("Invalid or expired JWT on STOMP CONNECT");
        }

        StompPrincipal principal = new StompPrincipal(user.getId().toString(), user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, user.getAuthorities());
        accessor.setUser(auth);
        return message;
    }
}
