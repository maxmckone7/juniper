package bio.terra.pearl.api.admin.controller.portal;

import bio.terra.pearl.api.admin.api.PortalEnvironmentApi;
import bio.terra.pearl.api.admin.service.AuthUtilService;
import bio.terra.pearl.api.admin.service.portal.PortalPublishingExtService;
import bio.terra.pearl.core.model.EnvironmentName;
import bio.terra.pearl.core.model.admin.AdminUser;
import bio.terra.pearl.core.model.publishing.PortalEnvironmentChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PortalEnvironmentController implements PortalEnvironmentApi {
  private HttpServletRequest request;
  private AuthUtilService authUtilService;
  private PortalPublishingExtService portalPublishingExtService;
  private ObjectMapper objectMapper;

  public PortalEnvironmentController(
      HttpServletRequest request,
      AuthUtilService authUtilService,
      PortalPublishingExtService portalPublishingExtService,
      ObjectMapper objectMapper) {
    this.request = request;
    this.authUtilService = authUtilService;
    this.portalPublishingExtService = portalPublishingExtService;
    this.objectMapper = objectMapper;
  }

  @Override
  public ResponseEntity<Object> diff(String portalShortcode, String destEnv, String sourceEnv) {
    AdminUser user = authUtilService.requireAdminUser(request);
    return ResponseEntity.ok(
        portalPublishingExtService.diff(
            portalShortcode,
            EnvironmentName.valueOfCaseInsensitive(sourceEnv),
            EnvironmentName.valueOfCaseInsensitive(destEnv),
            user));
  }

  @Override
  public ResponseEntity<Object> apply(
      String portalShortcode, String destEnv, String sourceEnv, Object body) {
    AdminUser user = authUtilService.requireAdminUser(request);
    PortalEnvironmentChange change = objectMapper.convertValue(body, PortalEnvironmentChange.class);
    return ResponseEntity.ok(
        portalPublishingExtService.update(
            portalShortcode, EnvironmentName.valueOfCaseInsensitive(destEnv), change, user));
  }
}
