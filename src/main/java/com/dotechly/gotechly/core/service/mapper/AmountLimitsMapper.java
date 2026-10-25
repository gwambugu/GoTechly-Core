package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.AmountLimits;
import com.dotechly.gotechly.core.service.dto.AmountLimitsDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AmountLimitsMapper implements EntityMapper<AmountLimitsDTO, AmountLimits> {

    @Override
    public AmountLimits toEntity(AmountLimitsDTO dto) {
        if (dto == null) {
            return null;
        }
        AmountLimits entity = new AmountLimits();
        entity.setId(dto.getId());
        entity.setChannelName(dto.getChannelName());
        entity.setServiceName(dto.getServiceName());
        entity.setCountryCode(dto.getCountryCode());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setTransactionLimit(dto.getTransactionLimit());
        entity.setDailyLimit(dto.getDailyLimit());
        entity.setStatus(dto.getStatus());
        entity.setDateCreated(dto.getDateCreated());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setDateModified(dto.getDateModified());
        entity.setModifiedBy(dto.getModifiedBy());
        return entity;
    }

    @Override
    public AmountLimitsDTO toDto(AmountLimits entity) {
        if (entity == null) {
            return null;
        }
        AmountLimitsDTO dto = new AmountLimitsDTO();
        dto.setId(entity.getId());
        dto.setChannelName(entity.getChannelName());
        dto.setServiceName(entity.getServiceName());
        dto.setCountryCode(entity.getCountryCode());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setTransactionLimit(entity.getTransactionLimit());
        dto.setDailyLimit(entity.getDailyLimit());
        dto.setStatus(entity.getStatus());
        dto.setDateCreated(entity.getDateCreated());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setDateModified(entity.getDateModified());
        dto.setModifiedBy(entity.getModifiedBy());
        return dto;
    }

    @Override
    public List<AmountLimits> toEntity(List<AmountLimitsDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<AmountLimits> list = new ArrayList<>(dtoList.size());
        for (AmountLimitsDTO dto : dtoList) {
            list.add(toEntity(dto));
        }
        return list;
    }

    @Override
    public List<AmountLimitsDTO> toDto(List<AmountLimits> entityList) {
        if (entityList == null) {
            return null;
        }
        List<AmountLimitsDTO> list = new ArrayList<>(entityList.size());
        for (AmountLimits entity : entityList) {
            list.add(toDto(entity));
        }
        return list;
    }
}
