package bio.terra.pearl.api.admin.controller.enrollee;

import bio.terra.pearl.api.admin.api.EnrolleeApi;
import bio.terra.pearl.api.admin.service.AuthUtilService;
import bio.terra.pearl.api.admin.service.enrollee.EnrolleeExtService;
import bio.terra.pearl.core.model.EnvironmentName;
import bio.terra.pearl.core.model.admin.AdminUser;
import bio.terra.pearl.core.model.participant.Enrollee;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class EnrolleeController implements EnrolleeApi {
  Logger log = LoggerFactory.getLogger(EnrolleeController.class);

  private AuthUtilService authUtilService;
  private EnrolleeExtService enrolleeExtService;
  private HttpServletRequest request;

  public EnrolleeController(
      AuthUtilService authUtilService,
      EnrolleeExtService enrolleeExtService,
      HttpServletRequest request) {
    this.authUtilService = authUtilService;
    this.enrolleeExtService = enrolleeExtService;
    this.request = request;
  }

  @Override
  public ResponseEntity<Object> find(
      String portalShortcode, String studyShortcode, String envName, String enrolleeShortcode) {
    AdminUser adminUser = authUtilService.requireAdminUser(request);
    Enrollee enrollee = enrolleeExtService.findWithAdminLoad(adminUser, enrolleeShortcode);
    return ResponseEntity.ok(enrollee);
  }

  @Override
  public ResponseEntity<Object> listChangeRecords(
      String portalShortcode, String studyShortcode, String envName, String enrolleeShortcode) {
    AdminUser adminUser = authUtilService.requireAdminUser(request);
    var records = enrolleeExtService.findDataChangeRecords(adminUser, enrolleeShortcode);
    return ResponseEntity.ok(records);
  }

  @Override
  public ResponseEntity<Object> withdraw(
      String portalShortcode, String studyShortcode, String envName, String enrolleeShortcode) {
    AdminUser adminUser = authUtilService.requireAdminUser(request);
    try {
      var withdrawn = enrolleeExtService.withdrawEnrollee(adminUser, enrolleeShortcode);
      return ResponseEntity.ok(new WithdrawnResponse(withdrawn.getId()));
    } catch (JsonProcessingException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<Object> enrolleesWithKits(
      String portalShortcode, String studyShortcode, String envName) {
    AdminUser adminUser = authUtilService.requireAdminUser(request);
    EnvironmentName environmentName = EnvironmentName.valueOfCaseInsensitive(envName);

    var enrollees =
        enrolleeExtService.findForKitManagement(
            adminUser, portalShortcode, studyShortcode, environmentName);
    return ResponseEntity.ok(enrollees);
  }

  public record WithdrawnResponse(UUID withdrawnEnrolleeId) {}
}
