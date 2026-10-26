package com.rohith.javaprojects.airbnb.controller;


import com.rohith.javaprojects.airbnb.dto.LoginRequestDTO;
import com.rohith.javaprojects.airbnb.dto.LoginResponseDTO;
import com.rohith.javaprojects.airbnb.dto.SignUpRequestDTO;
import com.rohith.javaprojects.airbnb.dto.UserDTO;
import com.rohith.javaprojects.airbnb.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signUp(
        @Valid @RequestBody SignUpRequestDTO signUpRequestDTO) {
        UserDTO userDTO = authService.signUp(signUpRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(authService.getProfile(userId));
    }
}
