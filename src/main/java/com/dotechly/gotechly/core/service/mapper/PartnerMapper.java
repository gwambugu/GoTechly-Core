package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.Partner;
import com.dotechly.gotechly.core.service.dto.PartnerDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PartnerMapper implements EntityMapper<PartnerDTO, Partner> {

    @Override
    public Partner toEntity(PartnerDTO dto) {
        if (dto == null) {
            return null;
        }
        Partner partner = new Partner();
        partner.setId(dto.getId());
        partner.setPartnerCode(dto.getPartnerCode());
        partner.setClientShortName(dto.getClientShortName());
        partner.setLegalName(dto.getLegalName());
        partner.setAgentId(dto.getAgentId());
        partner.setPartnerType(dto.getPartnerType());
        partner.setCountryCode(dto.getCountryCode());
        partner.setContactEmail(dto.getContactEmail());
        partner.setContactPhone(dto.getContactPhone());
        partner.setClientCertificate(dto.getClientCertificate());
        partner.setSalt(dto.getSalt());
        partner.setCallbackUrl(dto.getCallbackUrl());
        partner.setIpWhitelist(dto.getIpWhitelist());
        partner.setSettlementCycle(dto.getSettlementCycle());
        partner.setContractStartDate(dto.getContractStartDate());
        partner.setContractEndDate(dto.getContractEndDate());
        partner.setKycStatus(dto.getKycStatus());
        partner.setKycExpiryDate(dto.getKycExpiryDate());
        partner.setStatus(dto.getStatus());
        partner.setMakerId(dto.getMakerId());
        partner.setMakerDate(dto.getMakerDate());
        partner.setCheckerId(dto.getCheckerId());
        partner.setCheckerDate(dto.getCheckerDate());
        partner.setCheckerRemarks(dto.getCheckerRemarks());
        partner.setDateCreated(dto.getDateCreated());
        partner.setCreatedBy(dto.getCreatedBy());
        partner.setDateModified(dto.getDateModified());
        partner.setModifiedBy(dto.getModifiedBy());
        partner.setHash1(dto.getHash1());
        return partner;
    }

    @Override
    public PartnerDTO toDto(Partner entity) {
        if (entity == null) {
            return null;
        }
        PartnerDTO dto = new PartnerDTO();
        dto.setId(entity.getId());
        dto.setPartnerCode(entity.getPartnerCode());
        dto.setClientShortName(entity.getClientShortName());
        dto.setLegalName(entity.getLegalName());
        dto.setAgentId(entity.getAgentId());
        dto.setPartnerType(entity.getPartnerType());
        dto.setCountryCode(entity.getCountryCode());
        dto.setContactEmail(entity.getContactEmail());
        dto.setContactPhone(entity.getContactPhone());
        dto.setClientCertificate(entity.getClientCertificate());
        dto.setSalt(entity.getSalt());
        dto.setCallbackUrl(entity.getCallbackUrl());
        dto.setIpWhitelist(entity.getIpWhitelist());
        dto.setSettlementCycle(entity.getSettlementCycle());
        dto.setContractStartDate(entity.getContractStartDate());
        dto.setContractEndDate(entity.getContractEndDate());
        dto.setKycStatus(entity.getKycStatus());
        dto.setKycExpiryDate(entity.getKycExpiryDate());
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
        dto.setHash1(entity.getHash1());
        return dto;
    }

    @Override
    public List<Partner> toEntity(List<PartnerDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<Partner> list = new ArrayList<>(dtoList.size());
        for (PartnerDTO dto : dtoList) {
            list.add(toEntity(dto));
        }
        return list;
    }

    @Override
    public List<PartnerDTO> toDto(List<Partner> entityList) {
        if (entityList == null) {
            return null;
        }
        List<PartnerDTO> list = new ArrayList<>(entityList.size());
        for (Partner entity : entityList) {
            list.add(toDto(entity));
        }
        return list;
    }
}
