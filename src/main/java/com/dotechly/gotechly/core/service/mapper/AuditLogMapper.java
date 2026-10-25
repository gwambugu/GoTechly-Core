package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.AuditLog;
import com.dotechly.gotechly.core.service.dto.AuditLogDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for the entity {@link AuditLog} and its DTO {@link AuditLogDTO}.
 */
@Component
public class AuditLogMapper implements EntityMapper<AuditLogDTO, AuditLog> {

    @Override
    public AuditLog toEntity(AuditLogDTO dto) {
        if (dto == null) {
            return null;
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setId(dto.getId());
        auditLog.setEventType(dto.getEventType());
        auditLog.setEntityName(dto.getEntityName());
        auditLog.setPhaseName(dto.getPhaseName());
        auditLog.setOldValue(dto.getOldValue());
        auditLog.setNewValue(dto.getNewValue());
        auditLog.setChangedFields(dto.getChangedFields());
        auditLog.setActorId(dto.getActorId());
        auditLog.setActorType(dto.getActorType());
        auditLog.setActorUsername(dto.getActorUsername());
        auditLog.setRole(dto.getRole());
        auditLog.setIpAddress(dto.getIpAddress());
        auditLog.setUserAgent(dto.getUserAgent());
        auditLog.setSessionId(dto.getSessionId());
        auditLog.setRequestId(dto.getRequestId());
        auditLog.setEndpoint(dto.getEndpoint());
        auditLog.setHttpMethod(dto.getHttpMethod());
        auditLog.setEnvironment(dto.getEnvironment());
        auditLog.setServiceName(dto.getServiceName());
        auditLog.setCreatedAt(dto.getCreatedAt());

        return auditLog;
    }

    @Override
    public AuditLogDTO toDto(AuditLog entity) {
        if (entity == null) {
            return null;
        }

        AuditLogDTO auditLogDTO = new AuditLogDTO();
        auditLogDTO.setId(entity.getId());
        auditLogDTO.setEventType(entity.getEventType());
        auditLogDTO.setEntityName(entity.getEntityName());
        auditLogDTO.setPhaseName(entity.getPhaseName());
        auditLogDTO.setOldValue(entity.getOldValue());
        auditLogDTO.setNewValue(entity.getNewValue());
        auditLogDTO.setChangedFields(entity.getChangedFields());
        auditLogDTO.setActorId(entity.getActorId());
        auditLogDTO.setActorType(entity.getActorType());
        auditLogDTO.setActorUsername(entity.getActorUsername());
        auditLogDTO.setRole(entity.getRole());
        auditLogDTO.setIpAddress(entity.getIpAddress());
        auditLogDTO.setUserAgent(entity.getUserAgent());
        auditLogDTO.setSessionId(entity.getSessionId());
        auditLogDTO.setRequestId(entity.getRequestId());
        auditLogDTO.setEndpoint(entity.getEndpoint());
        auditLogDTO.setHttpMethod(entity.getHttpMethod());
        auditLogDTO.setEnvironment(entity.getEnvironment());
        auditLogDTO.setServiceName(entity.getServiceName());
        auditLogDTO.setCreatedAt(entity.getCreatedAt());

        return auditLogDTO;
    }

    @Override
    public List<AuditLog> toEntity(List<AuditLogDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        List<AuditLog> list = new ArrayList<>(dtoList.size());
        for (AuditLogDTO auditLogDTO : dtoList) {
            list.add(toEntity(auditLogDTO));
        }

        return list;
    }

    @Override
    public List<AuditLogDTO> toDto(List<AuditLog> entityList) {
        if (entityList == null) {
            return null;
        }

        List<AuditLogDTO> list = new ArrayList<>(entityList.size());
        for (AuditLog auditLog : entityList) {
            list.add(toDto(auditLog));
        }

        return list;
    }
}
