package com.onecard.config;

import com.onecard.domain.user.User;
import com.onecard.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 초기 사용자 데이터
        createUserIfNotExists("qwe", "qwe@onecard.local", "qweqwe");
        createUserIfNotExists("asd", "asd@onecard.local", "asdasd");
        createUserIfNotExists("zxc", "zxc@onecard.local", "zxczxc");
    }

    private void createUserIfNotExists(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            log.info("User {} already exists", username);
            return;
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();

        userRepository.save(user);
        log.info("Created user: {}", username);
    }
}
