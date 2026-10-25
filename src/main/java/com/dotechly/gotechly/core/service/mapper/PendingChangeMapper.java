package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.PendingChange;
import com.dotechly.gotechly.core.service.dto.PendingChangeDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PendingChangeMapper implements EntityMapper<PendingChangeDTO, PendingChange> {

    @Override
    public PendingChange toEntity(PendingChangeDTO dto) {
        if (dto == null) return null;

        PendingChange entity = new PendingChange();
        entity.setId(dto.getId());
        entity.setEntityType(dto.getEntityType());
        entity.setChangeType(dto.getChangeType());
        entity.setEntityId(dto.getEntityId());
        entity.setPayload(dto.getPayload());
        entity.setPreviousValue(dto.getPreviousValue());
        entity.setStatus(dto.getStatus());
        entity.setDescription(dto.getDescription());
        entity.setMakerUsername(dto.getMakerUsername());
        entity.setMakerDate(dto.getMakerDate());
        entity.setCheckerUsername(dto.getCheckerUsername());
        entity.setCheckerDate(dto.getCheckerDate());
        entity.setCheckerRemarks(dto.getCheckerRemarks());
        return entity;
    }

    @Override
    public PendingChangeDTO toDto(PendingChange entity) {
        if (entity == null) return null;

        PendingChangeDTO dto = new PendingChangeDTO();
        dto.setId(entity.getId());
        dto.setEntityType(entity.getEntityType());
        dto.setChangeType(entity.getChangeType());
        dto.setEntityId(entity.getEntityId());
        dto.setPayload(entity.getPayload());
        dto.setPreviousValue(entity.getPreviousValue());
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());
        dto.setMakerUsername(entity.getMakerUsername());
        dto.setMakerDate(entity.getMakerDate());
        dto.setCheckerUsername(entity.getCheckerUsername());
        dto.setCheckerDate(entity.getCheckerDate());
        dto.setCheckerRemarks(entity.getCheckerRemarks());
        return dto;
    }

    @Override
    public List<PendingChange> toEntity(List<PendingChangeDTO> dtoList) {
        if (dtoList == null) return null;
        List<PendingChange> list = new ArrayList<>(dtoList.size());
        for (PendingChangeDTO dto : dtoList) {
            list.add(toEntity(dto));
        }
        return list;
    }

    @Override
    public List<PendingChangeDTO> toDto(List<PendingChange> entityList) {
        if (entityList == null) return null;
        List<PendingChangeDTO> list = new ArrayList<>(entityList.size());
        for (PendingChange entity : entityList) {
            list.add(toDto(entity));
        }
        return list;
    }
}
