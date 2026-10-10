package com.qraudio.backend.controller;

import com.qraudio.backend.entity.User;
import com.qraudio.backend.exception.ApiException;
import com.qraudio.backend.repository.UserRepository;
import com.qraudio.backend.security.JwtTokenUtil;
import com.qraudio.backend.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerIntegrationTest {
    private static final String PASSWORD = "TestPass123!";
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtTokenUtil jwt;
    @Autowired private UserService userService;
    @Value("${security.jwt.secret}") private String testSecret;
    private User admin;
    private String bearer;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        admin = saveUser("admin", "ADMIN");
        bearer = token(admin);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "DELETE"})
    void allAdminOperations_RequireAuthentication(String method) throws Exception {
        String path = method.equals("GET") || method.equals("POST")
                ? "/api/admin/users" : "/api/admin/users/" + admin.getId();
        mvc.perform(request(HttpMethod.valueOf(method), path)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "DELETE"})
    void nonAdmin_CannotUseAccountOperations(String method) throws Exception {
        User viewer = saveUser("viewer", "USER");
        String path = method.equals("GET") || method.equals("POST")
                ? "/api/admin/users" : "/api/admin/users/" + admin.getId();
        mvc.perform(request(HttpMethod.valueOf(method), path)
                .header("Authorization", token(viewer))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void invalidToken_IsRejectedWithoutExposingIt() throws Exception {
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("invalid.token.value"))));
    }

    @Test
    void expiredToken_IsRejected() throws Exception {
        String expired = Jwts.builder().setSubject(admin.getUsername())
                .claim("uid", admin.getId()).claim("ver", 0)
                .setExpiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecret)), SignatureAlgorithm.HS256)
                .compact();
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signedTokenWithInvalidClaimType_IsRejected() throws Exception {
        String invalid = Jwts.builder().setSubject("admin").claim("uid", "not-a-number").claim("ver", 0)
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecret)), SignatureAlgorithm.HS256)
                .compact();
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + invalid))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyTokenWithoutAccountId_IsRejected() throws Exception {
        String legacy = Jwts.builder().setSubject("admin")
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecret)), SignatureAlgorithm.HS256)
                .compact();
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + legacy))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listAndGet_NeverReturnPasswordOrHash() throws Exception {
        mvc.perform(get("/api/admin/users").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].username").value("admin"))
                .andExpect(jsonPath("$.data[0].password").doesNotExist())
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/admin/users/{id}", admin.getId()).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void create_PersistsHashedPasswordAndReturns201() throws Exception {
        mvc.perform(post("/api/admin/users").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"second","password":"TestPass123!","role":"ADMIN"}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        User created = users.findByUsername("second").orElseThrow();
        assertNotEquals(PASSWORD, created.getPasswordHash());
        assertTrue(encoder.matches(PASSWORD, created.getPasswordHash()));
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void duplicateUsernameIgnoringCase_Returns409() throws Exception {
        mvc.perform(post("/api/admin/users").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"ADMIN","password":"TestPass123!"}
                        """)).andExpect(status().isConflict());
    }

    @Test
    void updateUsernameConflict_Returns409() throws Exception {
        User second = saveUser("second", "ADMIN");
        mvc.perform(patch("/api/admin/users/{id}", second.getId()).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidAccountInput_Returns400() throws Exception {
        mvc.perform(post("/api/admin/users").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bad name\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/users").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"second\",\"password\":\"TestPass123!\",\"role\":\"SUPERADMIN\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/users/{id}", admin.getId()).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/users/not-a-number").header("Authorization", bearer))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bcryptByteLimit_IsValidatedBeforeEncoding() throws Exception {
        String password = "ế".repeat(30);
        mvc.perform(post("/api/admin/users").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"second\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingAccount_Returns404ForReadUpdateAndDelete() throws Exception {
        mvc.perform(get("/api/admin/users/999999").header("Authorization", bearer))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/admin/users/999999").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/users/999999").header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    void lastActiveAdmin_CannotBeDisabledOrDeleted() throws Exception {
        mvc.perform(patch("/api/admin/users/{id}", admin.getId()).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/users/{id}", admin.getId()).header("Authorization", bearer))
                .andExpect(status().isConflict());
        assertTrue(users.findById(admin.getId()).orElseThrow().isActive());
    }

    @Test
    void disableAndReactivate_NeverRestoreOldToken() throws Exception {
        User second = saveUser("second", "ADMIN");
        String oldToken = token(second);
        mvc.perform(patch("/api/admin/users/{id}", second.getId()).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").header("Authorization", oldToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/users/{id}", second.getId()).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").header("Authorization", oldToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").header("Authorization",
                        token(users.findById(second.getId()).orElseThrow())))
                .andExpect(status().isOk());
    }

    @Test
    void delete_DeactivatesWithoutRemovingRecord() throws Exception {
        User second = saveUser("second", "ADMIN");
        String oldToken = token(second);
        mvc.perform(delete("/api/admin/users/{id}", second.getId()).header("Authorization", bearer))
                .andExpect(status().isNoContent());
        assertFalse(users.findById(second.getId()).orElseThrow().isActive());
        mvc.perform(get("/api/admin/users").header("Authorization", oldToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPassword_RevokesOldTokenAndChangesLogin() throws Exception {
        User second = saveUser("second", "ADMIN");
        String oldToken = token(second);
        mvc.perform(patch("/api/admin/users/{id}", second.getId()).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"NewPass123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.password").doesNotExist());
        mvc.perform(get("/api/admin/users").header("Authorization", oldToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"second\",\"password\":\"TestPass123!\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"second\",\"password\":\"NewPass123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    void renameAndReuseUsername_CannotTransferOldTokenToNewAccount() throws Exception {
        User second = saveUser("second", "ADMIN");
        String oldToken = token(second);
        mvc.perform(patch("/api/admin/users/{id}", second.getId()).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"renamed\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/users").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"second\",\"password\":\"TestPass123!\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/admin/users").header("Authorization", oldToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleIsReadFromDatabaseOnEveryRequest() throws Exception {
        User second = saveUser("second", "ADMIN");
        String oldToken = token(second);
        second.setRole("USER");
        users.saveAndFlush(second);
        mvc.perform(get("/api/admin/users").header("Authorization", oldToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticationDoesNotCarryOverWithoutBearerToken() throws Exception {
        var response = mvc.perform(get("/api/admin/users").header("Authorization", bearer))
                .andExpect(status().isOk()).andReturn();
        assertNull(response.getRequest().getSession(false));
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicReadsRemainAccessibleButWritesAreProtected() throws Exception {
        mvc.perform(get("/api/exhibits/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/qrcodes/resolve/missing")).andExpect(status().isNotFound());
        mvc.perform(post("/api/exhibits/999999")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void corsAllowsPatchPreflight() throws Exception {
        mvc.perform(options("/api/admin/users/1")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.containsString("PATCH")));
    }

    @Test
    void concurrentDeactivation_LeavesOneActiveAdmin() throws Exception {
        User second = saveUser("second", "ADMIN");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> deactivateAfterBarrier(admin.getId(), ready, start));
            var other = executor.submit(() -> deactivateAfterBarrier(second.getId(), ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successful = first.get(15, TimeUnit.SECONDS) + other.get(15, TimeUnit.SECONDS);
            assertEquals(1, successful);
        } finally {
            start.countDown();
        }
        assertEquals(1, users.findAll().stream().filter(User::isActive).count());
    }

    private int deactivateAfterBarrier(Long id, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("Barrier timed out"); }
        try {
            userService.deactivate(id);
            return 1;
        } catch (ApiException ex) {
            assertEquals(409, ex.getStatus().value());
            return 0;
        }
    }

    private User saveUser(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setActive(true);
        user.setPasswordHash(encoder.encode(PASSWORD));
        return users.saveAndFlush(user);
    }

    private String token(User user) {
        return "Bearer " + jwt.generateToken(user.getUsername(), user.getId(), user.getTokenVersion());
    }
}
