package com.voicecalendar.controller;

import com.voicecalendar.model.User;
import com.voicecalendar.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.getOrDefault("nickname", username);
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名和密码不能为空"));
        }
        if (username.length() < 2 || username.length() > 20) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名需2-20个字符"));
        }
        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "密码至少6位"));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已被使用"));
        }
        User user = User.builder().username(username).password(passwordEncoder.encode(password)).nickname(nickname).build();
        userRepository.save(user);
        session.setAttribute("userId", user.getId());
        return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername(), "nickname", user.getNickname()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名和密码不能为空"));
        }
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误"));
        }
        session.setAttribute("userId", user.getId());
        return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername(), "nickname", user.getNickname()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "已退出"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("message", "用户不存在"));
        return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername(), "nickname", user.getNickname()));
    }
}
