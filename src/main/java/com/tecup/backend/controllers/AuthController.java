package com.tecup.backend.controllers;

import com.tecup.backend.models.Career;
import com.tecup.backend.models.ERole;
import com.tecup.backend.models.Role;
import com.tecup.backend.models.User;
import com.tecup.backend.payload.repository.CareerRepository;
import com.tecup.backend.payload.request.LoginRequest;
import com.tecup.backend.payload.request.SignupRequest;
import com.tecup.backend.payload.response.MessageResponse;
import com.tecup.backend.payload.response.UserInfoResponse;
import com.tecup.backend.payload.repository.RoleRepository;
import com.tecup.backend.payload.repository.UserRepository;
import com.tecup.backend.security.jwt.JwtUtils;
import com.tecup.backend.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  CareerRepository careerRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtUtils jwtUtils;

  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

      Authentication authentication = authenticationManager
              .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

      SecurityContextHolder.getContext().setAuthentication(authentication);

      UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

      ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

      // Obtener el usuario desde el repositorio para acceder a la carrera
      User user = userRepository.findByUsername(userDetails.getUsername())
              .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

      // Obtener el nombre de la carrera (si tiene una asociada)
      String careerName = user.getCareer() != null ? user.getCareer().getName() : null;

      //Obtener Departamento de la carrera asociada al usuario
      String departmentCareer = null;
      if (user.getCareer() != null) {
          departmentCareer = user.getCareer().getDepartment_id().getName();
      }
      List<String> roles = userDetails.getAuthorities().stream()
              .map(item -> item.getAuthority())
              .collect(Collectors.toList());

    // Obtener las inscripciones (nombres de eventos)
    List<String> inscriptions = user.getInscriptions().stream()
            .map(inscription -> inscription.getEvent().getName()) // Obtener el nombre del evento
            .collect(Collectors.toList());

      return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
              .body(new UserInfoResponse(userDetails.getId(),
                      userDetails.getUsername(),
                      userDetails.getEmail(),
                      careerName, // Agregar la carrera al response
                      departmentCareer,
                      inscriptions, // Agregar las inscripciones al response
                      roles));
  }

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
    }

    // Create new user's account
    User user = new User(signUpRequest.getUsername(),
            signUpRequest.getEmail(),
            encoder.encode(signUpRequest.getPassword()));

    Set<String> strRoles = signUpRequest.getRole();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_USER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
          case "admin":
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(adminRole);

            break;
          case "organizador":
            Role organizadorRole = roleRepository.findByName(ERole.ROLE_ORGANIZADOR)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(organizadorRole);

            break;
          case "jurado":
            Role juraRole = roleRepository.findByName(ERole.ROLE_JURADO)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(juraRole);

            break;
          default:
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);

    // Asociar la carrera si se proporciona careerId
    if (signUpRequest.getCareerId() != null) {
      Career career = careerRepository.findById(signUpRequest.getCareerId())
              .orElseThrow(() -> new RuntimeException("Error: Career is not found."));
      user.setCareer(career);
    }
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
  }

  @PostMapping("/signout")
  public ResponseEntity<?> logoutUser() {
    ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(new MessageResponse("You've been signed out!"));
  }

  

}