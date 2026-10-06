package io.github.sevyn1.access;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccountApi {
  private final Accounts accounts;

  public AccountApi(Accounts accounts) {
    this.accounts = accounts;
  }

  public record TokenResponse(String headerName, String token) {}

  @GetMapping("/csrf")
  public ResponseEntity<TokenResponse> token(CsrfToken token) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(new TokenResponse(token.getHeaderName(), token.getToken()));
  }

  @PostMapping("/accounts")
  @ResponseStatus(HttpStatus.CREATED)
  public Accounts.PublicAccount register(@Valid @RequestBody Accounts.NewAccount input) {
    return accounts.register(input);
  }

  @GetMapping("/me")
  public ResponseEntity<Accounts.PrivateAccount> me(Principal principal) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(accounts.profile(principal.getName()));
  }

  @PatchMapping("/me")
  public ResponseEntity<Accounts.PrivateAccount> update(
      Principal principal, @Valid @RequestBody Accounts.ProfileChange input) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(accounts.update(principal.getName(), input));
  }

  @PostMapping("/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(
      Authentication authentication,
      @Valid @RequestBody Accounts.PasswordChange input,
      HttpServletRequest request,
      jakarta.servlet.http.HttpServletResponse response) {
    accounts.changePassword(authentication.getName(), input);
    new SecurityContextLogoutHandler().logout(request, response, authentication);
  }
}
