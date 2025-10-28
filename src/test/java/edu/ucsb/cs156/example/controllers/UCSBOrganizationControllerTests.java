package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = UCSBOrganizationController.class)
@Import(TestConfig.class)
public class UCSBOrganizationControllerTests extends ControllerTestCase {
  @MockBean UCSBOrganizationRepository ucsbOrganizationRepository;

  @MockBean UserRepository userRepository;

  // Authorization tests for /api/ucsborganizations/admin/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/ucsborganization/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/ucsborganization/all")).andExpect(status().is(200)); // logged
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/ucsborganization/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/ucsborganization/post"))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_ucsborganization() throws Exception {

    // arrange
    UCSBOrganization ZPR =
        UCSBOrganization.builder()
            .orgTranslationShort("ZETA PHI RHO")
            .orgCode("ZPR")
            .orgTranslation("ZETA PHI RHO")
            .inactive(false)
            .build();

    UCSBOrganization SKY =
        UCSBOrganization.builder()
            .orgTranslationShort("SKYDIVING CLUB")
            .orgCode("SKY")
            .orgTranslation("SKYDIVING CLUB AT UCSB")
            .inactive(false)
            .build();

    ArrayList<UCSBOrganization> expectedOrganizations = new ArrayList<>();
    expectedOrganizations.addAll(Arrays.asList(ZPR, SKY));

    when(ucsbOrganizationRepository.findAll()).thenReturn(expectedOrganizations);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/ucsborganization/all")).andExpect(status().isOk()).andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedOrganizations);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_organization() throws Exception {
    // arrange
    UCSBOrganization ZPR =
        UCSBOrganization.builder()
            .orgTranslationShort("ZETA PHI RHO")
            .orgCode("ZPR")
            .orgTranslation("ZETA PHI RHO")
            .inactive(false)
            .build();

    when(ucsbOrganizationRepository.save(eq(ZPR))).thenReturn(ZPR);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/ucsborganization/post?orgTranslationShort=ZETA PHI RHO&orgCode=ZPR&orgTranslation=ZETA PHI RHO&inactive=false")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).save(ZPR);
    String expectedJson = mapper.writeValueAsString(ZPR);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/ucsborganization?orgCode=ZPR"))
        .andExpect(status().is(403)); // logged out users can't get by id
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange

    UCSBOrganization organization =
        UCSBOrganization.builder()
            .orgTranslationShort("ZETA PHI RHO")
            .orgCode("ZPR")
            .orgTranslation("ZETA PHI RHO")
            .inactive(false)
            .build();

    when(ucsbOrganizationRepository.findById(eq("ZPR"))).thenReturn(Optional.of(organization));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/ucsborganization?orgCode=ZPR"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findById(eq("ZPR"));
    String expectedJson = mapper.writeValueAsString(organization);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(ucsbOrganizationRepository.findById(eq("munger-hall"))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/ucsborganization?orgCode=munger-hall"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findById(eq("munger-hall"));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id munger-hall not found", json.get("message"));
  }
}
