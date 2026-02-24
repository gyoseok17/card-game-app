package com.onecard.domain.user;

import com.onecard.dto.request.LoginRequest;
import com.onecard.dto.request.SignupRequest;
import com.onecard.dto.response.AuthResponse;
import com.onecard.dto.response.UserResponse;
import com.onecard.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

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
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, new UserResponse(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return ResponseEntity.ok(new UserResponse(user));
    }
}
