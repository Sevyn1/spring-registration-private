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
      @NotBlank @Email @Size(max = 254) String email,
      @NotNull @Size(min = 12, max = 72) String password) {}

  public record ProfileChange(
      @NotBlank @Size(max = 60) String displayName,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 72) String currentPassword) {}

  public record PasswordChange(
      @NotBlank @Size(max = 72) String currentPassword,
      @NotNull @Size(min = 12, max = 72) String newPassword) {}

  public record PublicAccount(String username, String displayName, Instant createdAt) {}

  public record PrivateAccount(
      String username, String displayName, String email, Instant createdAt) {}

  public static String normalize(String username) {
    return username.toLowerCase(Locale.ROOT);
  }

  private static void checkPassword(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Password must fit within 72 UTF-8 bytes.");
  }

  public PublicAccount register(NewAccount input) {
    checkPassword(input.password());
    String username = normalize(input.username());
    String name = input.displayName().strip();
    if (name.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name is required.");
    Instant now = Instant.now();
    try {
      store.insert(username, name, input.email().strip(), encoder.encode(input.password()), now);
    } catch (DuplicateKeyException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username unavailable.");
    }
    return new PublicAccount(username, name, now);
  }

  private AccountStore.StoredAccount find(String username) {
    return store
        .find(normalize(username))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
  }

  private AccountStore.StoredAccount verify(String username, String password) {
    checkPassword(password);
    var account = find(username);
    if (!encoder.matches(password, account.hash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
    return account;
  }

  public PrivateAccount profile(String username) {
    var a = find(username);
    return new PrivateAccount(a.username(), a.displayName(), a.email(), a.createdAt());
  }

  public PrivateAccount update(String username, ProfileChange input) {
    String name = input.displayName().strip();
    if (name.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name is required.");
    var a = verify(username, input.currentPassword());
    if (!store.updateProfile(a.username(), name, input.email().strip(), a.hash()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Account changed. Sign in again before saving.");
    return profile(username);
  }

  public void changePassword(String username, PasswordChange input) {
    checkPassword(input.newPassword());
    var a = verify(username, input.currentPassword());
    if (encoder.matches(input.newPassword(), a.hash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a different new password.");
    if (!store.updatePassword(a.username(), a.hash(), encoder.encode(input.newPassword())))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Password changed. Sign in again.");
  }
}
