package com.wisp.controller;

import com.wisp.entity.User;
import com.wisp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(
                        User.builder().username(username).online(false).build()
                ));

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}
