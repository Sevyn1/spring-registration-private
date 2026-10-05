package io.github.sevyn1.access;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.*;
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
  public ResponseEntity<Accounts.PublicAccount> me(Principal principal) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(accounts.profile(principal.getName()));
  }
}
