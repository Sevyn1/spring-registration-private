package com.example.demo;
import com.example.demo.Model.MyAppUserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {
 @Autowired MockMvc mvc;
 @Autowired MyAppUserRepository repository;
 @Autowired PasswordEncoder encoder;
 static final String ACCOUNT="{\"username\":\"Favour\",\"email\":\"demo@example.test\",\"password\":\"Example-password-123\"}";
 @BeforeEach void reset(){repository.deleteAll();}
 void register() throws Exception {mvc.perform(post("/req/signup").with(csrf()).contentType("application/json").content(ACCOUNT)).andExpect(status().isCreated());}
 @Test void registrationDoesNotExposePassword() throws Exception {
 mvc.perform(post("/req/signup").with(csrf()).contentType("application/json").content(ACCOUNT)).andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("favour")).andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist());
 var saved=repository.findByUsername("favour").orElseThrow();assertNotEquals("Example-password-123",saved.getPassword());assertTrue(encoder.matches("Example-password-123",saved.getPassword())); }
 @Test void missingCsrfRejected() throws Exception {mvc.perform(post("/req/signup").contentType("application/json").content(ACCOUNT)).andExpect(status().isForbidden());assertEquals(0,repository.count());}
 @Test void duplicateNormalizedUsernameConflicts() throws Exception {register();mvc.perform(post("/req/signup").with(csrf()).contentType("application/json").content(ACCOUNT.replace("Favour","FAVOUR"))).andExpect(status().isConflict());assertEquals(1,repository.count());}
 @Test void invalidEmailRejected() throws Exception {mvc.perform(post("/req/signup").with(csrf()).contentType("application/json").content(ACCOUNT.replace("demo@example.test","bad"))).andExpect(status().isBadRequest());}
 @Test void shortPasswordRejected() throws Exception {mvc.perform(post("/req/signup").with(csrf()).contentType("application/json").content(ACCOUNT.replace("Example-password-123","short"))).andExpect(status().isBadRequest());}
 @Test void multibytePasswordRejected() throws Exception {mvc.perform(post("/req/signup").with(csrf()).contentType("application/json").content(ACCOUNT.replace("Example-password-123","é".repeat(40)))).andExpect(status().isBadRequest());}
 @Test void usernameValidation() throws Exception {mvc.perform(post("/req/signup").with(csrf()).contentType("application/json").content(ACCOUNT.replace("Favour","bad name"))).andExpect(status().isBadRequest());}
 @Test void malformedJsonRejected() throws Exception {mvc.perform(post("/req/signup").with(csrf()).contentType("application/json").content("{" )).andExpect(status().isBadRequest());}
 @Test void loginWorks() throws Exception {register();mvc.perform(post("/req/login").with(csrf()).param("username","FAVOUR").param("password","Example-password-123")).andExpect(authenticated()).andExpect(redirectedUrl("/index"));}
 @Test void wrongPasswordRejected() throws Exception {register();mvc.perform(post("/req/login").with(csrf()).param("username","favour").param("password","wrong")).andExpect(unauthenticated()).andExpect(redirectedUrl("/req/login?error"));}
 @Test void dashboardRequiresAuthentication() throws Exception {mvc.perform(get("/index")).andExpect(status().is3xxRedirection());mvc.perform(get("/index").with(user("favour"))).andExpect(status().isOk());}
 @Test void pagesRenderCsrfTokens() throws Exception {mvc.perform(get("/req/signup")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));mvc.perform(get("/req/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));}
 @Test void logoutRequiresCsrf() throws Exception {mvc.perform(post("/logout").with(user("favour"))).andExpect(status().isForbidden());mvc.perform(post("/logout").with(user("favour")).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(unauthenticated());}
}
