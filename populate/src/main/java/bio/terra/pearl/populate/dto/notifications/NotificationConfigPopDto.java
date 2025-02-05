package bio.terra.pearl.populate.dto.notifications;

import bio.terra.pearl.core.model.notification.NotificationConfig;
import bio.terra.pearl.populate.dto.FilePopulatable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class NotificationConfigPopDto extends NotificationConfig implements FilePopulatable {
    private String emailTemplateStableId;
    private int emailTemplateVersion;
    private String populateFileName;
}
