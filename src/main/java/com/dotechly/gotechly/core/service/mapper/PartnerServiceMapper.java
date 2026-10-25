package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.PartnerServiceEntity;
import com.dotechly.gotechly.core.service.dto.PartnerServiceDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PartnerServiceMapper implements EntityMapper<PartnerServiceDTO, PartnerServiceEntity> {

    @Override
    public PartnerServiceEntity toEntity(PartnerServiceDTO dto) {
        if (dto == null) {
            return null;
        }
        PartnerServiceEntity partnerService = new PartnerServiceEntity();
        partnerService.setId(dto.getId());
        partnerService.setPartnerId(dto.getPartnerId());
        partnerService.setPaymentType(dto.getPaymentType());
        partnerService.setDirection(dto.getDirection());
        partnerService.setStatus(dto.getStatus());
        partnerService.setEffectiveDate(dto.getEffectiveDate());
        partnerService.setExpiryDate(dto.getExpiryDate());
        partnerService.setMakerId(dto.getMakerId());
        partnerService.setMakerDate(dto.getMakerDate());
        partnerService.setCheckerId(dto.getCheckerId());
        partnerService.setCheckerDate(dto.getCheckerDate());
        partnerService.setCheckerRemarks(dto.getCheckerRemarks());
        partnerService.setDateCreated(dto.getDateCreated());
        partnerService.setCreatedBy(dto.getCreatedBy());
        partnerService.setDateModified(dto.getDateModified());
        partnerService.setModifiedBy(dto.getModifiedBy());
        return partnerService;
    }

    @Override
    public PartnerServiceDTO toDto(PartnerServiceEntity entity) {
        if (entity == null) {
            return null;
        }
        PartnerServiceDTO dto = new PartnerServiceDTO();
        dto.setId(entity.getId());
        dto.setPartnerId(entity.getPartnerId());
        dto.setPaymentType(entity.getPaymentType());
        dto.setDirection(entity.getDirection());
        dto.setStatus(entity.getStatus());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setExpiryDate(entity.getExpiryDate());
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
    public List<PartnerServiceEntity> toEntity(List<PartnerServiceDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<PartnerServiceEntity> list = new ArrayList<>(dtoList.size());
        for (PartnerServiceDTO dto : dtoList) {
            list.add(toEntity(dto));
        }
        return list;
    }

    @Override
    public List<PartnerServiceDTO> toDto(List<PartnerServiceEntity> entityList) {
        if (entityList == null) {
            return null;
        }
        List<PartnerServiceDTO> list = new ArrayList<>(entityList.size());
        for (PartnerServiceEntity entity : entityList) {
            list.add(toDto(entity));
        }
        return list;
    }
}
