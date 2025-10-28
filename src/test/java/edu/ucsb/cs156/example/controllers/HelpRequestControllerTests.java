package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.HelpRequest;
import edu.ucsb.cs156.example.repositories.HelpRequestRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = HelpRequestController.class)
@Import(TestConfig.class)
public class HelpRequestControllerTests extends ControllerTestCase {

  @MockBean HelpRequestRepository helpRequestRepository;

  @MockBean UserRepository userRepository;

  // Authorization Tests

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/helprequest/all")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/helprequest/all")).andExpect(status().is(200));
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc.perform(get("/api/helprequest?id=7")).andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/helprequest/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/helprequest/post")).andExpect(status().is(403));
  }

  // GET /api/helprequest?id=7

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_by_id_when_it_exists() throws Exception {
    LocalDateTime t = LocalDateTime.parse("2022-01-03T00:00:00");
    HelpRequest help =
        HelpRequest.builder()
            .requesterEmail("student@ucsb.edu")
            .teamId("f25-14")
            .tableOrBreakoutRoom("table")
            .requestTime(t)
            .explanation("Need help")
            .solved(false)
            .build();

    when(helpRequestRepository.findById(eq(7L))).thenReturn(Optional.of(help));

    MvcResult response =
        mockMvc.perform(get("/api/helprequest?id=7")).andExpect(status().is(200)).andReturn();

    verify(helpRequestRepository, times(1)).findById(eq(7L));
    assertEquals(mapper.writeValueAsString(help), response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void get_by_id_when_not_found() throws Exception {
    when(helpRequestRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc.perform(get("/api/helprequest?id=7")).andExpect(status().isNotFound()).andReturn();

    verify(helpRequestRepository, times(1)).findById(eq(7L));

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("HelpRequest with id 7 not found", json.get("message"));
  }

  // GET /api/helprequest/all

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_requests() throws Exception {
    LocalDateTime t = LocalDateTime.parse("2022-01-03T00:00:00");

    HelpRequest h1 =
        HelpRequest.builder()
            .requesterEmail("a@ucsb.edu")
            .teamId("f25-14")
            .tableOrBreakoutRoom("table1")
            .requestTime(t)
            .explanation("help")
            .solved(false)
            .build();

    HelpRequest h2 =
        HelpRequest.builder()
            .requesterEmail("b@ucsb.edu")
            .teamId("f25-15")
            .tableOrBreakoutRoom("table2")
            .requestTime(t)
            .explanation("more help")
            .solved(true)
            .build();

    ArrayList<HelpRequest> expected = new ArrayList<>(Arrays.asList(h1, h2));

    when(helpRequestRepository.findAll()).thenReturn(expected);

    MvcResult response =
        mockMvc.perform(get("/api/helprequest/all")).andExpect(status().is(200)).andReturn();

    verify(helpRequestRepository, times(1)).findAll();
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  // POST /api/helprequest/post

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_post_new_help_request() throws Exception {

    LocalDateTime t = LocalDateTime.parse("2022-01-03T00:00:00");
    HelpRequest saved =
        HelpRequest.builder()
            .requesterEmail("admin@ucsb.edu")
            .teamId("f25-14")
            .tableOrBreakoutRoom("table")
            .requestTime(t)
            .explanation("Testing")
            .solved(true)
            .build();

    when(helpRequestRepository.save(eq(saved))).thenReturn(saved);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/helprequest/post?requesterEmail=admin@ucsb.edu&teamId=f25-14&tableOrBreakoutRoom=table&solved=true&explanation=Testing&requestTime=2022-01-03T00:00:00")
                    .with(csrf()))
            .andExpect(status().is(200))
            .andReturn();

    verify(helpRequestRepository, times(1)).save(eq(saved));

    HelpRequest actual =
        mapper.readValue(response.getResponse().getContentAsString(), HelpRequest.class);

    assertEquals("admin@ucsb.edu", actual.getRequesterEmail());
    assertEquals("f25-14", actual.getTeamId());
    assertEquals("table", actual.getTableOrBreakoutRoom());
    assertEquals("Testing", actual.getExplanation());
    assertEquals(true, actual.getSolved());
    assertEquals(t, actual.getRequestTime());
  }

  // DELETE /api/helprequest?id=

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_delete_help_request() throws Exception {
    LocalDateTime t = LocalDateTime.parse("2022-01-03T00:00:00");
    HelpRequest help =
        HelpRequest.builder()
            .requesterEmail("del@ucsb.edu")
            .teamId("f25-14")
            .tableOrBreakoutRoom("table")
            .requestTime(t)
            .explanation("delete me")
            .solved(false)
            .build();

    when(helpRequestRepository.findById(eq(15L))).thenReturn(Optional.of(help));

    MvcResult response =
        mockMvc
            .perform(delete("/api/helprequest?id=15").with(csrf()))
            .andExpect(status().is(200))
            .andReturn();

    verify(helpRequestRepository, times(1)).findById(15L);
    verify(helpRequestRepository, times(1)).delete(eq(help));

    Map<String, Object> json = responseToJson(response);
    assertEquals("HelpRequest with id 15 deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void delete_not_found_returns_404() throws Exception {
    when(helpRequestRepository.findById(eq(15L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/helprequest?id=15").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(helpRequestRepository, times(1)).findById(15L);

    Map<String, Object> json = responseToJson(response);
    assertEquals("HelpRequest with id 15 not found", json.get("message"));
  }

  // PUT /api/helprequest?id=

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_help_request() throws Exception {

    LocalDateTime ldt1 = LocalDateTime.parse("2023-05-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2024-03-03T00:00:00");

    HelpRequest helpRequestOrig =
        HelpRequest.builder()
            .requesterEmail("a@ucsb.edu")
            .teamId("f25-14")
            .tableOrBreakoutRoom("Table")
            .requestTime(ldt1)
            .explanation("test a")
            .solved(false)
            .build();

    HelpRequest helpRequestEdited =
        HelpRequest.builder()
            .requesterEmail("fv@ucsb.edu")
            .teamId("f25-99")
            .tableOrBreakoutRoom("Breakout")
            .requestTime(ldt2)
            .explanation("test fv")
            .solved(true)
            .build();

    String requestBody = mapper.writeValueAsString(helpRequestEdited);
    when(helpRequestRepository.findById(eq(67L))).thenReturn(Optional.of(helpRequestOrig));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/helprequest?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().is(200))
            .andReturn();

    verify(helpRequestRepository, times(1)).findById(67L);
    verify(helpRequestRepository, times(1)).save(helpRequestEdited);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_edit_nonexistent_returns_404() throws Exception {
    LocalDateTime t = LocalDateTime.parse("2022-01-03T00:00:00");

    HelpRequest edited =
        HelpRequest.builder()
            .requesterEmail("edit@ucsb.edu")
            .teamId("f25-99")
            .tableOrBreakoutRoom("table2")
            .requestTime(t)
            .explanation("Edited text")
            .solved(true)
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(helpRequestRepository.findById(eq(67L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/helprequest?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(helpRequestRepository, times(1)).findById(67L);

    Map<String, Object> json = responseToJson(response);
    assertEquals("HelpRequest with id 67 not found", json.get("message"));
  }
}
