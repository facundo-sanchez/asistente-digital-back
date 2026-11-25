package org.acme.user.infrastructure.persistence.mapper;

import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.acme.user.domain.model.User;
import org.acme.user.domain.model.UserToken;
import org.acme.user.infrastructure.persistence.entity.UserEntity;
import org.acme.user.infrastructure.persistence.entity.UserTokenEntity;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface UserPersistenceMapper {
    User toModel(UserEntity entity);
    UserEntity toEntity(User model);

    UserToken toModel(UserTokenEntity entity);
    UserTokenEntity toEntity(UserToken model);
}
