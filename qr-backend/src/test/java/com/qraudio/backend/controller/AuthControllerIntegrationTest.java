package com.qraudio.backend.controller;

import com.qraudio.backend.entity.User;
import com.qraudio.backend.repository.UserRepository;
import com.qraudio.backend.security.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = new User();
        user.setUsername("testadmin");
        user.setPasswordHash(passwordEncoder.encode("TestPass123!"));
        user.setRole("ADMIN");
        user.setActive(true);
        userRepository.saveAndFlush(user);
    }

    @Test
    void login_WithValidCredentials_ReturnsToken() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"testadmin","password":"TestPass123!"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.token");
        User user = userRepository.findByUsername("testadmin").orElseThrow();
        assertEquals(user.getId(), jwtTokenUtil.getClaimsFromToken(token).get("uid", Long.class));
        assertEquals(0L, jwtTokenUtil.getClaimsFromToken(token).get("ver", Long.class));
    }

    @Test
    void wrongPassword_ReturnsHttp401() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"testadmin","password":"wrong"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.data.token").doesNotExist());
    }

    @Test
    void unknownUsername_ReturnsHttp401() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"missing","password":"TestPass123!"}
                        """)).andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAccount_CannotLogin() throws Exception {
        User user = userRepository.findByUsername("testadmin").orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"testadmin","password":"TestPass123!"}
                        """)).andExpect(status().isUnauthorized());
    }

    @Test
    void missingCredentials_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void malformedJson_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }
}
