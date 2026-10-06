package io.github.sevyn1.access;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccessFlowTest {
  @Autowired MockMvc http;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccountStore store;
  @Autowired PasswordEncoder encoder;
  private static final String PASSWORD = "Demo-access-password-42";

  @BeforeEach
  void clear() {
    jdbc.update("DELETE FROM accounts");
  }

  String account(String username, String name, String password) throws Exception {
    return json.writeValueAsString(
        Map.of(
            "username",
            username,
            "displayName",
            name,
            "email",
            "demo@example.test",
            "password",
            password));
  }

  ResultActions register(String username) throws Exception {
    return http.perform(
        post("/api/accounts")
            .with(csrf())
            .contentType("application/json")
            .content(account(username, "Demo Member", PASSWORD)));
  }

  @Test
  void createdAccountOmitsStoredSecrets() throws Exception {
    register("Demo_Member")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("demo_member"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.hash").doesNotExist())
        .andExpect(jsonPath("$.email").doesNotExist());
    var saved = store.find("demo_member").orElseThrow();
    assertTrue(encoder.matches(PASSWORD, saved.hash()));
    assertNotEquals(PASSWORD, saved.hash());
    assertNotNull(saved.createdAt());
  }

  @Test
  void duplicateCaseVariantsReturnConflict() throws Exception {
    register("member").andExpect(status().isCreated());
    register("MEMBER")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Username unavailable."));
    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM accounts", Integer.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "bad space", "bad/name", "abcdefghijklmnopqrstuvwxyz"})
  void rejectsInvalidUsername(String name) throws Exception {
    register(name).andExpect(status().isBadRequest());
    assertTrue(store.find(name).isEmpty());
  }

  @Test
  void rejectsBlankDisplayName() throws Exception {
    http.perform(
            post("/api/accounts")
                .with(csrf())
                .contentType("application/json")
                .content(account("member", "   ", PASSWORD)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void trimsDisplayName() throws Exception {
    http.perform(
            post("/api/accounts")
                .with(csrf())
                .contentType("application/json")
                .content(account("member", "  Demo  ", PASSWORD)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.displayName").value("Demo"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"short", ""})
  void rejectsShortPassword(String password) throws Exception {
    http.perform(
            post("/api/accounts")
                .with(csrf())
                .contentType("application/json")
                .content(account("member", "Demo", password)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsPasswordOverByteLimit() throws Exception {
    http.perform(
            post("/api/accounts")
                .with(csrf())
                .contentType("application/json")
                .content(account("member", "Demo", "é".repeat(40))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMalformedAndMissingFields() throws Exception {
    for (String body : new String[] {"{", "{}"})
      http.perform(post("/api/accounts").with(csrf()).contentType("application/json").content(body))
          .andExpect(status().isBadRequest());
  }

  @Test
  void requiresCsrfOnAccountCreation() throws Exception {
    http.perform(
            post("/api/accounts")
                .contentType("application/json")
                .content(account("member", "Demo", PASSWORD)))
        .andExpect(status().isForbidden());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM accounts", Integer.class));
  }

  @Test
  void unauthenticatedProfileIsJson401() throws Exception {
    http.perform(get("/api/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/json"));
  }

  @Test
  void rejectsBadLoginWithoutRevealingAccountExistence() throws Exception {
    register("member");
    for (String user : new String[] {"member", "absent"}) {
      var result =
          http.perform(
                  post("/api/session")
                      .with(csrf())
                      .param("username", user)
                      .param("password", "incorrect"))
              .andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.message").value("Username or password incorrect."))
              .andReturn();
      assertEquals(
          "Username or password incorrect.",
          json.readTree(result.getResponse().getContentAsString()).get("message").asText());
    }
  }

  @Test
  void rejectsLoginWithoutCsrf() throws Exception {
    http.perform(post("/api/session").param("username", "member").param("password", PASSWORD))
        .andExpect(status().isForbidden());
  }

  @Test
  void completeSessionFlowUsingRealIssuedTokens() throws Exception {
    var initial = http.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
    var session = (MockHttpSession) initial.getRequest().getSession();
    var token = json.readTree(initial.getResponse().getContentAsString());
    http.perform(
            post("/api/accounts")
                .session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType("application/json")
                .content(account("Member", "<b>Demo</b>", PASSWORD)))
        .andExpect(status().isCreated());
    var login =
        http.perform(
                post("/api/session")
                    .session(session)
                    .header(token.get("headerName").asText(), token.get("token").asText())
                    .param("username", "MEMBER")
                    .param("password", PASSWORD))
            .andExpect(status().isNoContent())
            .andReturn();
    var authenticated = (MockHttpSession) login.getRequest().getSession();
    http.perform(get("/api/me").session(authenticated))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.username").value("member"))
        .andExpect(jsonPath("$.displayName").value("<b>Demo</b>"))
        .andExpect(jsonPath("$.hash").doesNotExist());
    http.perform(post("/api/session/logout").session(authenticated))
        .andExpect(status().isForbidden());
    var rotated = http.perform(get("/api/csrf").session(authenticated)).andReturn();
    var next = json.readTree(rotated.getResponse().getContentAsString());
    http.perform(
            post("/api/session/logout")
                .session(authenticated)
                .header(next.get("headerName").asText(), next.get("token").asText()))
        .andExpect(status().isNoContent());
    assertTrue(authenticated.isInvalid());
    http.perform(get("/api/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void staticPageHasSecurityHeadersAndIsPublic() throws Exception {
    http.perform(get("/index.html"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(
            header()
                .string(
                    "Content-Security-Policy",
                    org.hamcrest.Matchers.containsString("frame-ancestors 'none'")));
  }
}
