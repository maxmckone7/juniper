package bio.terra.pearl.populate.service;

import bio.terra.pearl.core.model.EnvironmentName;
import bio.terra.pearl.core.model.portal.MailingListContact;
import bio.terra.pearl.core.model.portal.Portal;
import bio.terra.pearl.core.model.portal.PortalEnvironment;
import bio.terra.pearl.core.model.site.SiteContent;
import bio.terra.pearl.core.model.study.PortalStudy;
import bio.terra.pearl.core.model.study.Study;
import bio.terra.pearl.core.model.survey.Survey;
import bio.terra.pearl.core.service.participant.PortalParticipantUserService;
import bio.terra.pearl.core.service.portal.MailingListContactService;
import bio.terra.pearl.core.service.portal.PortalEnvironmentService;
import bio.terra.pearl.core.service.portal.PortalService;
import bio.terra.pearl.core.service.study.PortalStudyService;
import bio.terra.pearl.populate.dto.AdminUserDto;
import bio.terra.pearl.populate.dto.PortalEnvironmentPopDto;
import bio.terra.pearl.populate.dto.PortalPopDto;
import bio.terra.pearl.populate.dto.site.SiteImagePopDto;
import bio.terra.pearl.populate.service.contexts.FilePopulateContext;
import bio.terra.pearl.populate.service.contexts.PortalPopulateContext;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PortalPopulator extends BasePopulator<Portal, PortalPopDto, FilePopulateContext> {

    private PortalService portalService;

    private PortalEnvironmentService portalEnvironmentService;
    private SiteImagePopulator siteImagePopulator;
    private StudyPopulator studyPopulator;
    private SurveyPopulator surveyPopulator;
    private SiteContentPopulator siteContentPopulator;
    private PortalStudyService portalStudyService;
    private PortalParticipantUserPopulator portalParticipantUserPopulator;
    private MailingListContactService mailingListContactService;
    private AdminUserPopulator adminUserPopulator;



    public PortalPopulator(PortalService portalService,
                           StudyPopulator studyPopulator,
                           PortalStudyService portalStudyService,
                           SiteContentPopulator siteContentPopulator,
                           PortalParticipantUserPopulator portalParticipantUserPopulator,
                           PortalParticipantUserService ppUserService,
                           PortalEnvironmentService portalEnvironmentService,
                           SiteImagePopulator siteImagePopulator, SurveyPopulator surveyPopulator,
                           AdminUserPopulator adminUserPopulator,
                           MailingListContactService mailingListContactService) {
        this.siteContentPopulator = siteContentPopulator;
        this.portalParticipantUserPopulator = portalParticipantUserPopulator;
        this.portalEnvironmentService = portalEnvironmentService;
        this.siteImagePopulator = siteImagePopulator;
        this.surveyPopulator = surveyPopulator;
        this.portalService = portalService;
        this.studyPopulator = studyPopulator;
        this.portalStudyService = portalStudyService;
        this.mailingListContactService = mailingListContactService;
        this.adminUserPopulator = adminUserPopulator;
    }

    private void populateStudy(String studyFileName, PortalPopulateContext context, Portal portal, boolean overwrite) throws IOException {
        Study newStudy = studyPopulator.populate(context.newFrom(studyFileName), overwrite);
        Optional<PortalStudy> portalStudyOpt = portalStudyService.findStudyInPortal(newStudy.getShortcode(), portal.getId());
        if (portalStudyOpt.isEmpty()) {
            PortalStudy portalStudy = portalStudyService.create(portal.getId(), newStudy.getId());
            portal.getPortalStudies().add(portalStudy);
            portalStudy.setStudy(newStudy);
        }
    }

    private void initializePortalEnv(PortalEnvironmentPopDto portalEnvPopDto,
                                     PortalPopulateContext portalPopContext, boolean overwrite) throws IOException {
        /** unless we've overwritten the whole portal (overwirte mode) don't alter non-sandbox environments */
        if (!portalEnvPopDto.getEnvironmentName().equals(EnvironmentName.sandbox) && !overwrite) {
            return;
        }

        PortalPopulateContext envConfig = portalPopContext.newFrom(portalEnvPopDto.getEnvironmentName());
        // we're iterating over each population file spec, so now match the current on to the
        // actual entity that got saved as a result of the portal create call.
        PortalEnvironment savedEnv = portalEnvironmentService
                .findOne(portalPopContext.getPortalShortcode(), portalEnvPopDto.getEnvironmentName()).get();

        if (portalEnvPopDto.getSiteContentPopDto() != null) {
            SiteContent content = siteContentPopulator
                    .findFromDto(portalEnvPopDto.getSiteContentPopDto(), portalPopContext).get();
            savedEnv.setSiteContent(content);
            savedEnv.setSiteContentId(content.getId());
        }
        if (portalEnvPopDto.getPreRegSurveyDto() != null) {
            Survey matchedSurvey = surveyPopulator.findFromDto(portalEnvPopDto.getPreRegSurveyDto(), portalPopContext).get();
            savedEnv.setPreRegSurveyId(matchedSurvey.getId());
        }
        for (String userFileName : portalEnvPopDto.getParticipantUserFiles()) {
            portalParticipantUserPopulator.populate(envConfig.newFrom(userFileName), overwrite);
        }
        // re-save the portal environment to update it with any attached siteContents or preRegSurveys
        portalEnvironmentService.update(savedEnv);
        populateMailingList(portalEnvPopDto, savedEnv, overwrite);
    }

    private void populateMailingList(PortalEnvironmentPopDto portalEnvPopDto, PortalEnvironment savedEnv,
                                     boolean overwrite) {
        if (!overwrite) {
            // we don't support updating mailing lists in-place yet
            return;
        }
        for (MailingListContact contact : portalEnvPopDto.getMailingListContacts()) {
            contact.setPortalEnvironmentId(savedEnv.getId());
            contact.setEmail(contact.getEmail());
            contact.setName(contact.getName());
            mailingListContactService.create(contact);
        }
    }

    @Override
    protected Class<PortalPopDto> getDtoClazz() {
        return PortalPopDto.class;
    }

    @Override
    public Optional<Portal> findFromDto(PortalPopDto popDto, FilePopulateContext context) {
        return portalService.findOneByShortcode(popDto.getShortcode());
    }

    @Override
    public Portal overwriteExisting(Portal existingObj, PortalPopDto popDto, FilePopulateContext context) throws IOException {
        portalService.delete(existingObj.getId(), Set.of(PortalService.AllowedCascades.STUDY));
        return createNew(popDto, context, true);
    }

    @Override
    public Portal createPreserveExisting(Portal existingObj, PortalPopDto popDto, FilePopulateContext context) throws IOException {
        existingObj.setName(popDto.getName());
        portalService.update(existingObj);
        return populateChildren(existingObj, popDto, context, false);
    }

    @Override
    public Portal createNew(PortalPopDto popDto, FilePopulateContext context, boolean overwrite) throws IOException {
        Portal portal = portalService.create(popDto);
        return populateChildren(portal, popDto, context, overwrite);
    }

    protected Portal populateChildren(Portal portal, PortalPopDto popDto, FilePopulateContext context, boolean overwrite) throws IOException {
        PortalPopulateContext portalPopContext = new PortalPopulateContext(context, portal.getShortcode(), null);

        for (AdminUserDto adminUserDto : popDto.getAdminUsers()) {
            adminUserPopulator.populateForPortal(adminUserDto, portalPopContext, overwrite, portal);
        }
        for (SiteImagePopDto imagePopDto : popDto.getSiteImageDtos()) {
            siteImagePopulator.populateFromDto(imagePopDto, portalPopContext, overwrite);
        }
        for (String surveyFile : popDto.getSurveyFiles()) {
            surveyPopulator.populate(portalPopContext.newFrom(surveyFile), overwrite);
        }
        for (String siteContentFile : popDto.getSiteContentFiles()) {
            siteContentPopulator.populate(portalPopContext.newFrom(siteContentFile), overwrite);
        }

        for (PortalEnvironmentPopDto portalEnvironment : popDto.getPortalEnvironmentDtos()) {
            initializePortalEnv(portalEnvironment, portalPopContext, overwrite);
        }
        for (String studyFileName : popDto.getPopulateStudyFiles()) {
            populateStudy(studyFileName, portalPopContext, portal, overwrite);
        }

        return portal;
    }

    /**
     * just populates the images from the given portal.json file.
     * Useful for populating/updating site content, which may rely on images form the root file
     * */
    public void populateImages(String portalFilePath, boolean overwrite) throws IOException {
        FilePopulateContext fileContext = new FilePopulateContext(portalFilePath);
        String portalFileString = filePopulateService.readFile(fileContext.getRootFileName(), fileContext);
        PortalPopDto popDto = readValue(portalFileString);
        PortalPopulateContext portalPopContext = new PortalPopulateContext(fileContext, popDto.getShortcode(), null);
        for (SiteImagePopDto imagePopDto : popDto.getSiteImageDtos()) {
            siteImagePopulator.populateFromDto(imagePopDto, portalPopContext, overwrite);
        }
    }
}
