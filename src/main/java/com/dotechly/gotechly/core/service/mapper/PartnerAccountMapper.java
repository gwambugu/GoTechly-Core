package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.PartnerAccount;
import com.dotechly.gotechly.core.service.dto.PartnerAccountDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PartnerAccountMapper implements EntityMapper<PartnerAccountDTO, PartnerAccount> {

    @Override
    public PartnerAccount toEntity(PartnerAccountDTO dto) {
        if (dto == null) {
            return null;
        }
        PartnerAccount partnerAccount = new PartnerAccount();
        partnerAccount.setId(dto.getId());
        partnerAccount.setPartnerId(dto.getPartnerId());
        partnerAccount.setAccountNumber(dto.getAccountNumber());
        partnerAccount.setAccountName(dto.getAccountName());
        partnerAccount.setAccountType(dto.getAccountType());
        partnerAccount.setBankCode(dto.getBankCode());
        partnerAccount.setBankName(dto.getBankName());
        partnerAccount.setBranchCode(dto.getBranchCode());
        partnerAccount.setSwiftCode(dto.getSwiftCode());
        partnerAccount.setCountryCode(dto.getCountryCode());
        partnerAccount.setCurrencyCode(dto.getCurrencyCode());
        partnerAccount.setIsPrimary(dto.getIsPrimary());
        partnerAccount.setStatus(dto.getStatus());
        partnerAccount.setMakerId(dto.getMakerId());
        partnerAccount.setMakerDate(dto.getMakerDate());
        partnerAccount.setCheckerId(dto.getCheckerId());
        partnerAccount.setCheckerDate(dto.getCheckerDate());
        partnerAccount.setCheckerRemarks(dto.getCheckerRemarks());
        partnerAccount.setDateCreated(dto.getDateCreated());
        partnerAccount.setCreatedBy(dto.getCreatedBy());
        partnerAccount.setDateModified(dto.getDateModified());
        partnerAccount.setModifiedBy(dto.getModifiedBy());
        partnerAccount.setHash1(dto.getHash1());
        return partnerAccount;
    }

    @Override
    public PartnerAccountDTO toDto(PartnerAccount entity) {
        if (entity == null) {
            return null;
        }
        PartnerAccountDTO dto = new PartnerAccountDTO();
        dto.setId(entity.getId());
        dto.setPartnerId(entity.getPartnerId());
        dto.setAccountNumber(entity.getAccountNumber());
        dto.setAccountName(entity.getAccountName());
        dto.setAccountType(entity.getAccountType());
        dto.setBankCode(entity.getBankCode());
        dto.setBankName(entity.getBankName());
        dto.setBranchCode(entity.getBranchCode());
        dto.setSwiftCode(entity.getSwiftCode());
        dto.setCountryCode(entity.getCountryCode());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setIsPrimary(entity.getIsPrimary());
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
    public List<PartnerAccount> toEntity(List<PartnerAccountDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<PartnerAccount> list = new ArrayList<>(dtoList.size());
        for (PartnerAccountDTO dto : dtoList) {
            list.add(toEntity(dto));
        }
        return list;
    }

    @Override
    public List<PartnerAccountDTO> toDto(List<PartnerAccount> entityList) {
        if (entityList == null) {
            return null;
        }
        List<PartnerAccountDTO> list = new ArrayList<>(entityList.size());
        for (PartnerAccount entity : entityList) {
            list.add(toDto(entity));
        }
        return list;
    }
}
