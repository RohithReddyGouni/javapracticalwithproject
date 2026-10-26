package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.LoginRequestDTO;
import com.rohith.javaprojects.airbnb.dto.LoginResponseDTO;
import com.rohith.javaprojects.airbnb.dto.SignUpRequestDTO;
import com.rohith.javaprojects.airbnb.dto.UserDTO;
import com.rohith.javaprojects.airbnb.entity.User;
import com.rohith.javaprojects.airbnb.exception.InvalidCredentialsException;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.exception.ResourceNotFoundException;
import com.rohith.javaprojects.airbnb.repository.UserRepository;
import com.rohith.javaprojects.airbnb.security.JwtService;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;

    }

    @Override
    public UserDTO signUp(SignUpRequestDTO request) {

        String email = request.getEmail()
                .strip()
                .toLowerCase(Locale.ROOT);
        if(userRepository.existsByEmail(email)) {
            throw new ResourceConflictException(
                    "Email is already registered"
            );
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
                );

        User userResponse;
        try {
            userResponse = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateEmail(exception)) {
                throw new ResourceConflictException(
                        "Email is already registered"
                );
            }
            throw exception;
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setId(userResponse.getId());
        userDTO.setEmail(userResponse.getEmail());
        userDTO.setName(userResponse.getName());
        return userDTO;

    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        String email = loginRequestDTO.getEmail()
                .strip()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(
                loginRequestDTO.getPassword(), user.getPasswordHash())
        ) {
            throw new InvalidCredentialsException();

        }
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        LoginResponseDTO response = new LoginResponseDTO();
        response.setAccessToken(jwtService.generateAccessToken(user.getId()));
        response.setExpiresIn(jwtService.getExpirationSeconds());
        response.setTokenType("Bearer");
        response.setUserDTO(userDTO);

        return response;
    }

    public UserDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID:: "+ userId
                ));
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());

        return userDTO;

    }

    public boolean isDuplicateEmail(Throwable exception) {
        Throwable cause = exception;

        while(cause != null) {
            if(cause instanceof ConstraintViolationException violation
                    && "uk_users_email".equals(
                            violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();

        }
        return false;
    }
}
