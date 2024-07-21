package com.hcmute.utezbe.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.hcmute.utezbe.auth.request.AuthenticationRequest;
import com.hcmute.utezbe.auth.request.RefreshTokenRequest;
import com.hcmute.utezbe.domain.RequestContext;
import com.hcmute.utezbe.auth.request.IdTokenRequest;
import com.hcmute.utezbe.entity.enumClass.Provider;
import com.hcmute.utezbe.entity.RefreshToken;
import com.hcmute.utezbe.entity.enumClass.Role;
import com.hcmute.utezbe.entity.User;
import com.hcmute.utezbe.exception.ApiException;
import com.hcmute.utezbe.repository.UserRepository;
import com.hcmute.utezbe.response.Response;
import com.hcmute.utezbe.security.jwt.JWTService;
import com.hcmute.utezbe.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public Response authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        Optional<User> opt = repository.findByEmailIgnoreCase(request.getEmail());
        if(opt.isEmpty()) {
            return Response.builder().code(HttpStatus.FORBIDDEN.value()).message("Email or Password wrong!").success(false).build();
        }
        User user = opt.get();
        if(user.getProvider() == Provider.GOOGLE.GOOGLE) {
            return Response.builder().code(HttpStatus.FORBIDDEN.value()).message("Email or Password wrong!").success(false).build();
        }
        if(!user.isEnabled()) {
            return Response.builder()
                    .code(HttpStatus.FORBIDDEN.value())
                    .success(false)
                    .data(user.getEmail())
                    .message("Account Not Confirm!")
                    .build();
        }
//        if(user.getRole() != request.getRole()) {
//            return Response.builder().error(true).success(false).message("You Do Not Have Authorize").build();
//        };
        RequestContext.setUserId(user.getId());
        var jwtToken = jwtService.generateAccessToken(user);
        var jwtRefreshToken = refreshTokenService.createRefreshToken(user.getEmail());
        return Response.builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .data(objectMapper.createObjectNode()
                        .putObject("user")
                        .put("accessToken", jwtToken)
                        .put("refreshToken", jwtRefreshToken.getToken())
                        .put("id", user.getId())
                        .put("fullName", user.getFullName())
                        .put("email", user.getEmail())
                        .put("avatar", user.getAvatarUrl())
                        .put("role", user.getRole().name())
                        .put("provider", user.getProvider().name()))
                .message("Login Successfully!")
                .build();
    }


    @Transactional
    public Response loginWithGoogle(IdTokenRequest idTokenRequest) {
        String googleClientId = "660554863773-c5na5d9dvnogok0i23ekgnpr15to3rvn.apps.googleusercontent.com";
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                .setAudience(Collections.singleton(googleClientId))
                .build();
        GoogleIdToken token;
        try {
            token = verifier.verify(idTokenRequest.getIdToken());
            if (Objects.isNull(token)) {
                throw new ApiException("Invalid id token");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        User user = verifyGoogleIdToken(idTokenRequest.getIdToken());
        if (user == null) {
            throw new ApiException("Invalid id token");
        }
        user = createOrUpdateUser(user);
        RequestContext.setUserId(user.getId());
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());
        return Response.builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .message("Login successfully!")
                .data(objectMapper.createObjectNode()
                        .put("access_token", accessToken)
                        .put("email", user.getEmail())
                        .put("full_name", user.getFullName())
                        .put("avatar", user.getAvatarUrl())
                        .put("role", user.getRole().name())
                )
                .build();
    }

    private User verifyGoogleIdToken(String idToken) {
        String googleClientId = "660554863773-c5na5d9dvnogok0i23ekgnpr15to3rvn.apps.googleusercontent.com";
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                .setAudience(Collections.singleton(googleClientId))
                .build();
        try {
            var token = verifier.verify(idToken);
            if (Objects.isNull(token)) {
                throw new ApiException("Token verification failed!");
            }
            var payload = token.getPayload();
            String email = payload.getEmail();
            String fullName = payload.get("name").toString();
            String avatarUrl = (String) payload.get("picture");
            Role role = (payload.get("hd") == null) ? Role.STUDENT :  (payload.get("hd").equals("hcmute.edu.vn") ? Role.TEACHER : Role.STUDENT);

            return User.builder()
                    .email(email)
                    .fullName(fullName)
                    .avatarUrl(avatarUrl)
                    .provider(Provider.GOOGLE)
                    .role(role)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Transactional
    public User createOrUpdateUser(User user) {
        User existedUser = repository.findByEmailIgnoreCase(user.getEmail()).orElse(null);
        if (existedUser == null) {
            System.out.println("Create new user");
            repository.save(user);
            return user;
        }
        if (existedUser.getProvider() != Provider.GOOGLE) {
            throw new ApiException("Email already registered by another method!");
        }
        System.out.println("Update existed user");
        existedUser.setFullName(user.getFullName());
        existedUser.setAvatarUrl(user.getAvatarUrl());
        existedUser.setProvider(user.getProvider());
        existedUser = repository.save(existedUser);
        return existedUser == null ? user : existedUser;
    }

    public Object refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return refreshTokenService.findByToken(refreshTokenRequest.getToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateAccessToken(user);
                    return Response.builder()
                            .data(objectMapper.createObjectNode()
                                    .put("accessToken", accessToken)
                                    .put("refreshToken", refreshTokenRequest.getToken()))
                            .code(HttpStatus.OK.value())
                            .success(true)
                            .message("Refresh Token Successfully!")
                            .build();
                })
                .orElseThrow(() -> new RuntimeException(
                        "Refresh Token not in database!"));
    }

    public static boolean isUserHaveRole(Role role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role.name())));
    }

    public static boolean checkTeacherRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.TEACHER.name())));
    }

    public static boolean checkAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.ADMIN.name())));
    }
}
