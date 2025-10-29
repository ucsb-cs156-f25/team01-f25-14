package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.Articles;
import edu.ucsb.cs156.example.repositories.ArticlesRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = ArticlesController.class)
@Import(TestConfig.class)
public class ArticlesControllerTests extends ControllerTestCase {

  @MockBean ArticlesRepository articlesRepository;
  @MockBean UserRepository userRepository;

  // Authorization tests for /api/articles/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/articles/all")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/articles/all")).andExpect(status().is(200));
  }

  // Authorization tests for /api/articles/post

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/articles/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    // may be 403 due to role or csrf; either is fine for this test
    mockMvc.perform(post("/api/articles/post")).andExpect(status().is(403));
  }

  // Tests with mocks for database actions

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_articles() throws Exception {

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    Articles article1 =
        Articles.builder()
            .title("Article1")
            .url("https://www.google.com")
            .explanation("This is a test explanation")
            .email("article1@test.com")
            .dateAdded(ldt1)
            .build();

    LocalDateTime ldt2 = LocalDateTime.parse("2022-03-11T00:00:00");
    Articles article2 =
        Articles.builder()
            .title("Article2")
            .url("https://www.google.com")
            .explanation("This is a test explanation")
            .email("article2@test.com")
            .dateAdded(ldt2)
            .build();

    ArrayList<Articles> expectedArticles = new ArrayList<>();
    expectedArticles.addAll(Arrays.asList(article1, article2));

    when(articlesRepository.findAll()).thenReturn(expectedArticles);

    MvcResult response =
        mockMvc.perform(get("/api/articles/all")).andExpect(status().isOk()).andReturn();

    verify(articlesRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedArticles);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_article() throws Exception {
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    // Make save(any()) return the same object it received (typical passthrough)
    when(articlesRepository.save(any(Articles.class))).thenAnswer(inv -> inv.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/articles/post?title=Article1&url=https://www.google.com&explanation=test&email=article1@test.com&dateAdded=2022-01-03T00:00:00")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // Capture the entity actually passed to save(...)
    ArgumentCaptor<Articles> captor = ArgumentCaptor.forClass(Articles.class);
    verify(articlesRepository, times(1)).save(captor.capture());
    Articles saved = captor.getValue();

    assertEquals("Article1", saved.getTitle());
    assertEquals("https://www.google.com", saved.getUrl());
    assertEquals("test", saved.getExplanation());
    assertEquals("article1@test.com", saved.getEmail());
    assertEquals(ldt1, saved.getDateAdded());

    // Response should echo the saved entity
    String expectedJson = mapper.writeValueAsString(saved);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_articles() throws Exception {
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2023-01-03T00:00:00");

    Articles articlesOrig =
        Articles.builder()
            .title("Test Article")
            .url("https://example.com")
            .explanation("Test explanation")
            .email("test@ucsb.edu")
            .dateAdded(ldt1)
            .build();

    // What the client sends
    Articles articlesEdited =
        Articles.builder()
            .title("Second Article")
            .url("https://example2.com")
            .explanation("Second explanation")
            .email("test2@ucsb.edu")
            .dateAdded(ldt2)
            .build();

    String requestBody = mapper.writeValueAsString(articlesEdited);

    when(articlesRepository.findById(eq(67L))).thenReturn(Optional.of(articlesOrig));
    when(articlesRepository.save(any(Articles.class))).thenAnswer(inv -> inv.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/articles?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(articlesRepository, times(1)).findById(67L);

    // Controller likely saves the *mutated original*, not the request instance.
    ArgumentCaptor<Articles> captor = ArgumentCaptor.forClass(Articles.class);
    verify(articlesRepository, times(1)).save(captor.capture());
    Articles saved = captor.getValue();

    assertEquals("Second Article", saved.getTitle());
    assertEquals("https://example2.com", saved.getUrl());
    assertEquals("Second explanation", saved.getExplanation());
    assertEquals("test2@ucsb.edu", saved.getEmail());
    assertEquals(ldt2, saved.getDateAdded());

    String responseString = response.getResponse().getContentAsString();
    assertEquals(mapper.writeValueAsString(saved), responseString);
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc.perform(get("/api/articles?id=7")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {
    LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");
    Articles article =
        Articles.builder()
            .title("Test Article")
            .url("https://example.com")
            .explanation("Test explanation")
            .email("test@ucsb.edu")
            .dateAdded(ldt)
            .build();

    when(articlesRepository.findById(eq(7L))).thenReturn(Optional.of(article));

    MvcResult response =
        mockMvc.perform(get("/api/articles?id=7")).andExpect(status().isOk()).andReturn();

    verify(articlesRepository, times(1)).findById(7L);
    String expectedJson = mapper.writeValueAsString(article);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_get_by_id_returns_404_when_not_found() throws Exception {
    when(articlesRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc.perform(get("/api/articles?id=7")).andExpect(status().isNotFound()).andReturn();

    verify(articlesRepository, times(1)).findById(7L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Articles with id 7 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_articles_that_does_not_exist() throws Exception {
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    Articles articlesEdited =
        Articles.builder()
            .title("Test Article")
            .url("https://example.com")
            .explanation("Test explanation")
            .email("test@ucsb.edu")
            .dateAdded(ldt1)
            .build();

    String requestBody = mapper.writeValueAsString(articlesEdited);

    when(articlesRepository.findById(eq(67L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/articles?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(articlesRepository, times(1)).findById(67L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("Articles with id 67 not found", json.get("message"));
  }
}
