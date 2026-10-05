package com.example.demo.Controller;
import com.example.demo.Model.MyAppUser;
import com.example.demo.Model.MyAppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
@RestController
public class RegistrationController {
 private final MyAppUserRepository repository;
 private final PasswordEncoder encoder;
 public RegistrationController(MyAppUserRepository repository, PasswordEncoder encoder) {this.repository=repository;this.encoder=encoder;}
 public record SignupRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_]{3,30}") String username, @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=12,max=72) String password) {}
 public record AccountResponse(Long id, String username) {}
 @PostMapping(value="/req/signup", consumes="application/json")
 @ResponseStatus(HttpStatus.CREATED)
 public AccountResponse createUser(@Valid @RequestBody SignupRequest request) {
  String name=request.username().toLowerCase(Locale.ROOT);
  if(repository.findByUsername(name).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Username unavailable");
  if(request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password exceeds 72 UTF-8 bytes");
  MyAppUser user=new MyAppUser(); user.setUsername(name);user.setEmail(request.email());user.setPassword(encoder.encode(request.password()));
  try {repository.saveAndFlush(user);} catch(DataIntegrityViolationException e) {throw new ResponseStatusException(HttpStatus.CONFLICT,"Username unavailable");}
  return new AccountResponse(user.getId(),user.getUsername());
 }
}
