package io.github.sevyn1.access;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Accounts {
  private final AccountStore store;
  private final PasswordEncoder encoder;

  public Accounts(AccountStore store, PasswordEncoder encoder) {
    this.store = store;
    this.encoder = encoder;
  }

  public record NewAccount(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,24}") String username,
      @NotBlank @Size(max = 60) String displayName,
      @NotNull @Size(min = 12, max = 72) String password) {}

  public record PublicAccount(String username, String displayName, Instant createdAt) {}

  public static String normalize(String username) {
    return username.toLowerCase(Locale.ROOT);
  }

  public PublicAccount register(NewAccount input) {
    if (input.password().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Password must fit within 72 UTF-8 bytes.");
    String username = normalize(input.username());
    String name = input.displayName().strip();
    if (name.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name is required.");
    Instant now = Instant.now();
    try {
      store.insert(username, name, encoder.encode(input.password()), now);
    } catch (DuplicateKeyException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username unavailable.");
    }
    return new PublicAccount(username, name, now);
  }

  public PublicAccount profile(String username) {
    var account =
        store
            .find(normalize(username))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
    return new PublicAccount(account.username(), account.displayName(), account.createdAt());
  }
}
