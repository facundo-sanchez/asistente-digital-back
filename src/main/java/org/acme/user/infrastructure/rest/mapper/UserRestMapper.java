package org.acme.user.infrastructure.rest.mapper;

import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.acme.user.domain.model.User;
import org.acme.user.infrastructure.rest.dto.UpdateProfileRequest;
import org.acme.user.infrastructure.rest.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface UserRestMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "googleSubject", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProfile(UpdateProfileRequest request, @MappingTarget User user);
}
