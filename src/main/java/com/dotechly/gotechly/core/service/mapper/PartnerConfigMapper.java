package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.PartnerConfig;
import com.dotechly.gotechly.core.service.dto.PartnerConfigDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PartnerConfigMapper implements EntityMapper<PartnerConfigDTO, PartnerConfig> {

    @Override
    public PartnerConfig toEntity(PartnerConfigDTO dto) {
        if (dto == null) {
            return null;
        }
        PartnerConfig partnerConfig = new PartnerConfig();
        partnerConfig.setId(dto.getId());
        partnerConfig.setPartnerId(dto.getPartnerId());
        partnerConfig.setParamName(dto.getParamName());
        partnerConfig.setParamValue(dto.getParamValue());
        partnerConfig.setParamDescription(dto.getParamDescription());
        partnerConfig.setStatus(dto.getStatus());
        partnerConfig.setMakerId(dto.getMakerId());
        partnerConfig.setMakerDate(dto.getMakerDate());
        partnerConfig.setCheckerId(dto.getCheckerId());
        partnerConfig.setCheckerDate(dto.getCheckerDate());
        partnerConfig.setCheckerRemarks(dto.getCheckerRemarks());
        partnerConfig.setDateCreated(dto.getDateCreated());
        partnerConfig.setCreatedBy(dto.getCreatedBy());
        partnerConfig.setDateModified(dto.getDateModified());
        partnerConfig.setModifiedBy(dto.getModifiedBy());
        return partnerConfig;
    }

    @Override
    public PartnerConfigDTO toDto(PartnerConfig entity) {
        if (entity == null) {
            return null;
        }
        PartnerConfigDTO dto = new PartnerConfigDTO();
        dto.setId(entity.getId());
        dto.setPartnerId(entity.getPartnerId());
        dto.setParamName(entity.getParamName());
        dto.setParamValue(entity.getParamValue());
        dto.setParamDescription(entity.getParamDescription());
        dto.setStatus(entity.getStatus());
        dto.setMakerId(entity.getMakerId());
        dto.setMakerDate(entity.getMakerDate());
        dto.setCheckerId(entity.getCheckerId());
        dto.setCheckerDate(entity.getCheckerDate());
        dto.setCheckerRemarks(entity.getCheckerRemarks());
        dto.setDateCreated(entity.getDateCreated());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setDateModified(entity.getDateModified());
        dto.setModifiedBy(entity.getModifiedBy());
        return dto;
    }

    @Override
    public List<PartnerConfig> toEntity(List<PartnerConfigDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<PartnerConfig> list = new ArrayList<>(dtoList.size());
        for (PartnerConfigDTO dto : dtoList) {
            list.add(toEntity(dto));
        }
        return list;
    }

    @Override
    public List<PartnerConfigDTO> toDto(List<PartnerConfig> entityList) {
        if (entityList == null) {
            return null;
        }
        List<PartnerConfigDTO> list = new ArrayList<>(entityList.size());
        for (PartnerConfig entity : entityList) {
            list.add(toDto(entity));
        }
        return list;
    }
}
