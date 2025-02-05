package bio.terra.pearl.core.service.publishing;

import bio.terra.pearl.core.BaseSpringBootTest;
import bio.terra.pearl.core.factory.admin.AdminUserFactory;
import bio.terra.pearl.core.factory.portal.PortalEnvironmentFactory;
import bio.terra.pearl.core.factory.portal.PortalFactory;
import bio.terra.pearl.core.factory.survey.SurveyFactory;
import bio.terra.pearl.core.model.EnvironmentName;
import bio.terra.pearl.core.model.admin.AdminUser;
import bio.terra.pearl.core.model.portal.Portal;
import bio.terra.pearl.core.model.portal.PortalEnvironment;
import bio.terra.pearl.core.model.survey.Survey;
import bio.terra.pearl.core.service.portal.PortalEnvironmentConfigService;
import bio.terra.pearl.core.service.portal.PortalEnvironmentService;
import bio.terra.pearl.core.service.survey.SurveyService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PortalPublishingServiceTests extends BaseSpringBootTest {
    @Test
    public void testApplyPortalConfigChanges() throws Exception {
        AdminUser user = adminUserFactory.buildPersisted("testApplyPortalConfigChanges", true);
        Portal portal = portalFactory.buildPersisted("testApplyPortalConfigChanges");
        PortalEnvironment irbEnv = portalEnvironmentFactory.buildPersisted("testApplyPortalConfigChanges", EnvironmentName.irb, portal.getId());
        PortalEnvironment liveEnv = portalEnvironmentFactory.buildPersisted("testApplyPortalConfigChanges", EnvironmentName.live, portal.getId());

        var irbConfig = portalEnvironmentConfigService.find(irbEnv.getPortalEnvironmentConfigId()).get();
        irbConfig.setPassword("foobar");
        irbConfig.setEmailSourceAddress("info@demo.com");
        portalEnvironmentConfigService.update(irbConfig);

        var changes = portalDiffService.diffPortalEnvs(portal.getShortcode(), EnvironmentName.irb, EnvironmentName.live);
        portalPublishingService.applyChanges(portal.getShortcode(), EnvironmentName.live, changes, user);
        var liveConfig = portalEnvironmentConfigService.find(liveEnv.getPortalEnvironmentConfigId()).get();
        assertThat(liveConfig.getPassword(), equalTo("foobar"));
        assertThat(liveConfig.getEmailSourceAddress(), equalTo("info@demo.com"));
    }

    @Test
    public void testPublishesSurveyPortalChanges() throws Exception {
        AdminUser user = adminUserFactory.buildPersisted("testPublishesSurveyPortalChanges", true);
        Portal portal = portalFactory.buildPersisted("testPublishesSurveyPortalChanges");
        Survey survey = surveyFactory.buildPersisted("testPublishesSurveyPortalChanges");
        PortalEnvironment irbEnv = portalEnvironmentFactory.buildPersisted("testPublishesSurveyPortalChanges", EnvironmentName.irb, portal.getId());
        PortalEnvironment liveEnv = portalEnvironmentFactory.buildPersisted("testPublishesSurveyPortalChanges", EnvironmentName.live, portal.getId());
        irbEnv.setPreRegSurveyId(survey.getId());
        portalEnvironmentService.update(irbEnv);

        var changes = portalDiffService.diffPortalEnvs(portal.getShortcode(), EnvironmentName.irb, EnvironmentName.live);
        portalPublishingService.applyChanges(portal.getShortcode(), EnvironmentName.live, changes, user);

        PortalEnvironment updatedLiveEnv = portalEnvironmentService.find(liveEnv.getId()).get();
        assertThat(updatedLiveEnv.getPreRegSurveyId(), equalTo(survey.getId()));
        survey = surveyService.find(survey.getId()).get();
        assertThat(survey.getPublishedVersion(), equalTo(1));
    }

    @Autowired
    private PortalDiffService portalDiffService;
    @Autowired
    private PortalPublishingService portalPublishingService;
    @Autowired
    private SurveyFactory surveyFactory;
    @Autowired
    private PortalFactory portalFactory;
    @Autowired
    private PortalEnvironmentFactory portalEnvironmentFactory;
    @Autowired
    private AdminUserFactory adminUserFactory;
    @Autowired
    private PortalEnvironmentService portalEnvironmentService;
    @Autowired
    private SurveyService surveyService;
    @Autowired
    private PortalEnvironmentConfigService portalEnvironmentConfigService;
}
