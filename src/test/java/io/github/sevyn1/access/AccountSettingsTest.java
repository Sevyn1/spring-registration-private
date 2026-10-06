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
class AccountSettingsTest {
  @Autowired MockMvc http;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccountStore store;
  @Autowired PasswordEncoder encoder;
  static final String OLD = "Demo-original-password-42", NEW = "Demo-changed-password-43";

  @BeforeEach
  void clear() {
    jdbc.update("DELETE FROM accounts");
  }

  String body(Map<String, String> values) throws Exception {
    return json.writeValueAsString(values);
  }

  void signup(String name) throws Exception {
    http.perform(
            post("/api/accounts")
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(
                        Map.of(
                            "username",
                            name,
                            "displayName",
                            "Alex Member",
                            "email",
                            name + "@example.test",
                            "password",
                            OLD))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").doesNotExist());
  }

  MockHttpSession login(String name, String password) throws Exception {
    return (MockHttpSession)
        http.perform(
                post("/api/session")
                    .with(csrf())
                    .param("username", name)
                    .param("password", password))
            .andExpect(status().isNoContent())
            .andReturn()
            .getRequest()
            .getSession();
  }

  String details(String password) throws Exception {
    return body(
        Map.of(
            "displayName",
            "Updated Member",
            "email",
            "updated@example.test",
            "currentPassword",
            password,
            "username",
            "other"));
  }

  String password(String current, String next) throws Exception {
    return body(Map.of("currentPassword", current, "newPassword", next));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "not-an-email", "bad@", "person with spaces@example.test"})
  void registrationRequiresValidEmail(String email) throws Exception {
    http.perform(
            post("/api/accounts")
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(
                        Map.of(
                            "username",
                            "member",
                            "displayName",
                            "Member",
                            "email",
                            email,
                            "password",
                            OLD))))
        .andExpect(status().isBadRequest());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM accounts", Integer.class));
  }

  @Test
  void onlyOwnProfileIncludesEmailAndHasNoSecrets() throws Exception {
    signup("member");
    signup("other");
    var session = login("member", OLD);
    http.perform(get("/api/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("member@example.test"))
        .andExpect(jsonPath("$.hash").doesNotExist())
        .andExpect(jsonPath("$.password").doesNotExist());
    http.perform(
            patch("/api/me")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(details(OLD)))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.username").value("member"))
        .andExpect(jsonPath("$.displayName").value("Updated Member"));
    assertEquals("Alex Member", store.find("other").orElseThrow().displayName());
    assertEquals("updated@example.test", store.find("member").orElseThrow().email());
  }

  @Test
  void profileEditRejectsWrongPasswordWithoutChangingData() throws Exception {
    signup("member");
    var session = login("member", OLD);
    http.perform(
            patch("/api/me")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(details("incorrect")))
        .andExpect(status().isBadRequest());
    assertEquals("Alex Member", store.find("member").orElseThrow().displayName());
  }

  @Test
  void profileEditRejectsInvalidEmail() throws Exception {
    signup("member");
    var session = login("member", OLD);
    http.perform(
            patch("/api/me")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(
                        Map.of(
                            "displayName", "Changed", "email", "invalid", "currentPassword", OLD))))
        .andExpect(status().isBadRequest());
    assertEquals("member@example.test", store.find("member").orElseThrow().email());
  }

  @Test
  void settingsRequireAuthenticationAndCsrf() throws Exception {
    http.perform(
            patch("/api/me").with(csrf()).contentType("application/json").content(details(OLD)))
        .andExpect(status().isUnauthorized());
    http.perform(
            post("/api/me/password")
                .with(csrf())
                .contentType("application/json")
                .content(password(OLD, NEW)))
        .andExpect(status().isUnauthorized());
    signup("member");
    var session = login("member", OLD);
    http.perform(
            patch("/api/me").session(session).contentType("application/json").content(details(OLD)))
        .andExpect(status().isForbidden());
    http.perform(
            post("/api/me/password")
                .session(session)
                .contentType("application/json")
                .content(password(OLD, NEW)))
        .andExpect(status().isForbidden());
    assertTrue(encoder.matches(OLD, store.find("member").orElseThrow().hash()));
  }

  @Test
  void passwordChangeEndsSessionAndOnlyNewPasswordWorks() throws Exception {
    signup("member");
    var session = login("member", OLD);
    var issued = http.perform(get("/api/csrf").session(session)).andReturn();
    var token = json.readTree(issued.getResponse().getContentAsString());
    http.perform(
            post("/api/me/password")
                .session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType("application/json")
                .content(password(OLD, NEW)))
        .andExpect(status().isNoContent());
    assertTrue(session.isInvalid());
    var hash = store.find("member").orElseThrow().hash();
    assertTrue(encoder.matches(NEW, hash));
    assertFalse(encoder.matches(OLD, hash));
    http.perform(get("/api/me")).andExpect(status().isUnauthorized());
    http.perform(
            post("/api/session").with(csrf()).param("username", "member").param("password", OLD))
        .andExpect(status().isUnauthorized());
    var next = login("member", NEW);
    http.perform(get("/api/me").session(next)).andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "short",
        "",
        "Demo-original-password-42",
        "éééééééééééééééééééééééééééééééééééééééé"
      })
  void passwordChangeRejectsWeakSameOrOverlongPasswords(String value) throws Exception {
    signup("member");
    var session = login("member", OLD);
    http.perform(
            post("/api/me/password")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(password(OLD, value)))
        .andExpect(status().isBadRequest());
    assertFalse(session.isInvalid());
    assertTrue(encoder.matches(OLD, store.find("member").orElseThrow().hash()));
  }

  @Test
  void passwordChangeRequiresCorrectCurrentPassword() throws Exception {
    signup("member");
    var session = login("member", OLD);
    http.perform(
            post("/api/me/password")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(password("incorrect", NEW)))
        .andExpect(status().isBadRequest());
    assertTrue(encoder.matches(OLD, store.find("member").orElseThrow().hash()));
  }

  @Test
  void stalePasswordWriteCannotOverwriteNewerPassword() throws Exception {
    signup("member");
    var old = store.find("member").orElseThrow().hash();
    var changed = encoder.encode(NEW);
    assertTrue(store.updatePassword("member", old, changed));
    assertFalse(store.updatePassword("member", old, encoder.encode("Another-password-99")));
    assertFalse(store.updateProfile("member", "Stale edit", "stale@example.test", old));
    assertEquals(changed, store.find("member").orElseThrow().hash());
  }

  @Test
  void migrationPreservesExistingVersionOneAccounts() throws Exception {
    String url = "jdbc:h2:mem:migrationcheck;DB_CLOSE_DELAY=-1";
    org.flywaydb.core.Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
    var ds = new org.springframework.jdbc.datasource.DriverManagerDataSource(url, "sa", "");
    var db = new JdbcTemplate(ds);
    db.update(
        "INSERT INTO accounts(username,display_name,password_hash,created_at)"
            + " VALUES(?,?,?,CURRENT_TIMESTAMP)",
        "legacy",
        "Legacy Member",
        encoder.encode(OLD));
    org.flywaydb.core.Flyway.configure().dataSource(url, "sa", "").load().migrate();
    var migrated = new AccountStore(db).find("legacy").orElseThrow();
    assertEquals("Legacy Member", migrated.displayName());
    assertNull(migrated.email());
    assertTrue(encoder.matches(OLD, migrated.hash()));
  }
}
