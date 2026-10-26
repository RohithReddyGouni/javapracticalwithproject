package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.LoginRequestDTO;
import com.rohith.javaprojects.airbnb.dto.LoginResponseDTO;
import com.rohith.javaprojects.airbnb.dto.SignUpRequestDTO;
import com.rohith.javaprojects.airbnb.dto.UserDTO;

public interface AuthService {
    UserDTO signUp(SignUpRequestDTO request);
    LoginResponseDTO login(LoginRequestDTO request);
    UserDTO getProfile(Long userId);
}
