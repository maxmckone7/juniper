package bio.terra.pearl.core.model.publishing;

import bio.terra.pearl.core.model.notification.EmailTemplate;
import bio.terra.pearl.core.model.notification.NotificationConfig;
import bio.terra.pearl.core.model.site.SiteContent;
import bio.terra.pearl.core.model.survey.Survey;

import java.util.List;

/**
 * Record of a change to a PortalEnvironment.
 * Currently, we only track changes to non-sandbox environments, which are performed via the diff/publishing workflow
 * */
public record PortalEnvironmentChange(VersionedEntityChange<SiteContent> siteContentChange,
                                      List<ConfigChange> configChanges,
                                      VersionedEntityChange<Survey> preRegSurveyChanges,
                                      ListChange<NotificationConfig, VersionedConfigChange<EmailTemplate>> notificationConfigChanges,
                                      List<StudyEnvironmentChange> studyEnvChanges)
{}
