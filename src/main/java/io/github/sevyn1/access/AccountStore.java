package io.github.sevyn1.access;

import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountStore {
  private final JdbcTemplate jdbc;

  public AccountStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record StoredAccount(
      String username, String displayName, String email, String hash, Instant createdAt) {}

  public Optional<StoredAccount> find(String username) {
    return jdbc
        .query(
            "SELECT username, display_name, email, password_hash, created_at FROM accounts WHERE"
                + " username = ?",
            (rs, n) ->
                new StoredAccount(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getTimestamp(5).toInstant()),
            username)
        .stream()
        .findFirst();
  }

  public void insert(
      String username, String displayName, String email, String hash, Instant createdAt) {
    jdbc.update(
        "INSERT INTO accounts(username,display_name,email,password_hash,created_at)"
            + " VALUES(?,?,?,?,?)",
        username,
        displayName,
        email,
        hash,
        java.sql.Timestamp.from(createdAt));
  }

  public boolean updateProfile(String username, String name, String email, String expectedHash) {
    return jdbc.update(
            "UPDATE accounts SET display_name=?, email=? WHERE username=? AND password_hash=?",
            name,
            email,
            username,
            expectedHash)
        == 1;
  }

  public boolean updatePassword(String username, String expectedHash, String newHash) {
    return jdbc.update(
            "UPDATE accounts SET password_hash=? WHERE username=? AND password_hash=?",
            newHash,
            username,
            expectedHash)
        == 1;
  }
}
