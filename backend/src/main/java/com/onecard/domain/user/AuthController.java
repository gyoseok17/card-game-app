package com.onecard.domain.user;

import com.onecard.dto.request.LoginRequest;
import com.onecard.dto.request.SignupRequest;
import com.onecard.dto.response.AuthResponse;
import com.onecard.dto.response.UserResponse;
import com.onecard.security.JwtTokenProvider;
import com.onecard.security.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        User user = userService.signup(request);
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, new UserResponse(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        // 기존 토큰이 있으면 블랙리스트 등록
        String oldToken = tokenBlacklistService.getCurrentToken(user.getId());
        if (oldToken != null && jwtTokenProvider.isTokenValid(oldToken)) {
            long remaining = jwtTokenProvider.getRemainingExpiration(oldToken);
            tokenBlacklistService.blacklist(oldToken, remaining);
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId());
        tokenBlacklistService.saveCurrentToken(user.getId(), token, jwtTokenProvider.getRemainingExpiration(token));
        return ResponseEntity.ok(new AuthResponse(token, new UserResponse(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication auth, HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            long remaining = jwtTokenProvider.getRemainingExpiration(token);
            tokenBlacklistService.blacklist(token, remaining);
        }
        User user = userService.findByUsername(auth.getName());
        tokenBlacklistService.removeCurrentToken(user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return ResponseEntity.ok(new UserResponse(user));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
