package org.acme.provider.infrastructure.persistence.mapper;

import org.acme.provider.domain.model.EmailBodyField;
import org.acme.provider.domain.model.Provider;
import org.acme.provider.domain.model.ProviderEmailRule;
import org.acme.provider.infrastructure.persistence.entity.ProviderEmailRuleEntity;
import org.acme.provider.infrastructure.persistence.entity.ProviderEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

import java.util.HashMap;
import java.util.Map;

@Mapper(config = MapStructConfig.class)
public interface ProviderPersistenceMapper {
    Provider toModel(ProviderEntity entity);
    ProviderEntity toEntity(Provider model);

    ProviderEmailRule toModel(ProviderEmailRuleEntity entity);
    ProviderEmailRuleEntity toEntity(ProviderEmailRule model);

    Map<String, EmailBodyField> toBodyFields(Map<String, Map<String, String>> json);
    Map<String, Map<String, String>> toBodyFieldsJson(Map<String, EmailBodyField> fields);

    default EmailBodyField toBodyField(Map<String, String> json) {
        return json == null ? null : new EmailBodyField(json.get("regex"), json.get("format"));
    }

    default Map<String, String> toBodyFieldJson(EmailBodyField field) {
        if (field == null) return null;
        Map<String, String> json = new HashMap<>();
        json.put("regex", field.regex());
        if (field.format() != null) json.put("format", field.format());
        return json;
    }
}
