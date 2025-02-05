package bio.terra.pearl.populate.service;

import bio.terra.pearl.core.model.notification.EmailTemplate;
import bio.terra.pearl.core.model.notification.NotificationConfig;
import bio.terra.pearl.core.model.portal.PortalEnvironment;
import bio.terra.pearl.core.service.notification.email.EmailTemplateService;
import bio.terra.pearl.core.service.portal.PortalEnvironmentService;
import bio.terra.pearl.core.service.portal.PortalService;
import bio.terra.pearl.populate.dao.EmailTemplatePopulateDao;
import bio.terra.pearl.populate.dto.notifications.EmailTemplatePopDto;
import bio.terra.pearl.populate.dto.notifications.NotificationConfigPopDto;
import bio.terra.pearl.populate.service.contexts.PortalPopulateContext;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplatePopulator extends BasePopulator<EmailTemplate, EmailTemplatePopDto, PortalPopulateContext> {
    private EmailTemplateService emailTemplateService;
    private PortalService portalService;
    private EmailTemplatePopulateDao emailTemplatePopulateDao;
    private PortalEnvironmentService portalEnvironmentService;

    public EmailTemplatePopulator(EmailTemplateService emailTemplateService,
                                  PortalService portalService,
                                  EmailTemplatePopulateDao emailTemplatePopulateDao,
                                  PortalEnvironmentService portalEnvironmentService) {
        this.emailTemplateService = emailTemplateService;
        this.portalService = portalService;
        this.emailTemplatePopulateDao = emailTemplatePopulateDao;
        this.portalEnvironmentService = portalEnvironmentService;
    }

    public NotificationConfig convertNotificationConfig(NotificationConfigPopDto configPopDto, PortalPopulateContext context) {
        NotificationConfig config = new NotificationConfig();
        BeanUtils.copyProperties(configPopDto, config);
        PortalEnvironment portalEnv = portalEnvironmentService
                .findOne(context.getPortalShortcode(), context.getEnvironmentName()).get();
        config.setPortalEnvironmentId(portalEnv.getId());

        EmailTemplate template;
        if (configPopDto.getPopulateFileName() != null) {
            template = context.fetchFromPopDto(configPopDto, emailTemplateService).get();
        } else {
            template = emailTemplateService.findByStableId(configPopDto.getEmailTemplateStableId(),
                    configPopDto.getEmailTemplateVersion()).get();
        }
        config.setEmailTemplateId(template.getId());
        config.setEmailTemplate(template);
        return config;
    }

    @Override
    protected void preProcessDto(EmailTemplatePopDto popDto, PortalPopulateContext context) throws IOException  {
        String bodyContent = filePopulateService.readFile(popDto.getBodyPopulateFile(), context);
        popDto.setBody(bodyContent);
    }

    @Override
    protected Class<EmailTemplatePopDto> getDtoClazz() {
        return EmailTemplatePopDto.class;
    }

    @Override
    public Optional<EmailTemplate> findFromDto(EmailTemplatePopDto popDto, PortalPopulateContext context) {
        Optional<EmailTemplate> existingOpt = context.fetchFromPopDto(popDto, emailTemplateService);
        if (existingOpt.isPresent()) {
            return existingOpt;
        }
        return emailTemplateService.findByStableId(popDto.getStableId(), popDto.getVersion());
    }

    @Override
    public EmailTemplate overwriteExisting(EmailTemplate existingObj, EmailTemplatePopDto popDto, PortalPopulateContext context) {
        // don't delete the template, since it may have other entities attached to it. Just mod the content
        existingObj.setBody(popDto.getBody());
        existingObj.setSubject(popDto.getSubject());
        existingObj.setName(popDto.getName());
        return emailTemplatePopulateDao.update(existingObj);
    }

    @Override
    public EmailTemplate createPreserveExisting(EmailTemplate existingObj, EmailTemplatePopDto popDto, PortalPopulateContext context) {
        if (Objects.equals(existingObj.getBody(), popDto.getBody()) &&
                Objects.equals(existingObj.getSubject(), popDto.getSubject()) &&
                Objects.equals(existingObj.getName(), popDto.getName())) {
            // the things are the same, don't bother creating a new version
            return existingObj;
        }
        int newVersion = emailTemplateService.getNextVersion(popDto.getStableId());
        popDto.setVersion(newVersion);
        return emailTemplateService.create(popDto);
    }

    @Override
    public EmailTemplate createNew(EmailTemplatePopDto popDto, PortalPopulateContext context, boolean overwrite) {
        return emailTemplateService.create(popDto);
    }
}
