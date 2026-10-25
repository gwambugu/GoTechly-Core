package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.PartnerTransactionLimit;
import com.dotechly.gotechly.core.service.dto.PartnerTransactionLimitDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PartnerTransactionLimitMapper implements EntityMapper<PartnerTransactionLimitDTO, PartnerTransactionLimit> {

    @Override
    public PartnerTransactionLimit toEntity(PartnerTransactionLimitDTO dto) {
        if (dto == null) {
            return null;
        }
        PartnerTransactionLimit partnerTransactionLimit = new PartnerTransactionLimit();
        partnerTransactionLimit.setId(dto.getId());
        partnerTransactionLimit.setPartnerId(dto.getPartnerId());
        partnerTransactionLimit.setPaymentType(dto.getPaymentType());
        partnerTransactionLimit.setDirection(dto.getDirection());
        partnerTransactionLimit.setCurrencyCode(dto.getCurrencyCode());
        partnerTransactionLimit.setMinAmount(dto.getMinAmount());
        partnerTransactionLimit.setMaxAmount(dto.getMaxAmount());
        partnerTransactionLimit.setDailyAmountLimit(dto.getDailyAmountLimit());
        partnerTransactionLimit.setDailyCountLimit(dto.getDailyCountLimit());
        partnerTransactionLimit.setMonthlyAmountLimit(dto.getMonthlyAmountLimit());
        partnerTransactionLimit.setMonthlyCountLimit(dto.getMonthlyCountLimit());
        partnerTransactionLimit.setFeeAmount(dto.getFeeAmount());
        partnerTransactionLimit.setFeePercentage(dto.getFeePercentage());
        partnerTransactionLimit.setStatus(dto.getStatus());
        partnerTransactionLimit.setEffectiveDate(dto.getEffectiveDate());
        partnerTransactionLimit.setExpiryDate(dto.getExpiryDate());
        partnerTransactionLimit.setMakerId(dto.getMakerId());
        partnerTransactionLimit.setMakerDate(dto.getMakerDate());
        partnerTransactionLimit.setCheckerId(dto.getCheckerId());
        partnerTransactionLimit.setCheckerDate(dto.getCheckerDate());
        partnerTransactionLimit.setCheckerRemarks(dto.getCheckerRemarks());
        partnerTransactionLimit.setDateCreated(dto.getDateCreated());
        partnerTransactionLimit.setCreatedBy(dto.getCreatedBy());
        partnerTransactionLimit.setDateModified(dto.getDateModified());
        partnerTransactionLimit.setModifiedBy(dto.getModifiedBy());
        return partnerTransactionLimit;
    }

    @Override
    public PartnerTransactionLimitDTO toDto(PartnerTransactionLimit entity) {
        if (entity == null) {
            return null;
        }
        PartnerTransactionLimitDTO dto = new PartnerTransactionLimitDTO();
        dto.setId(entity.getId());
        dto.setPartnerId(entity.getPartnerId());
        dto.setPaymentType(entity.getPaymentType());
        dto.setDirection(entity.getDirection());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setMinAmount(entity.getMinAmount());
        dto.setMaxAmount(entity.getMaxAmount());
        dto.setDailyAmountLimit(entity.getDailyAmountLimit());
        dto.setDailyCountLimit(entity.getDailyCountLimit());
        dto.setMonthlyAmountLimit(entity.getMonthlyAmountLimit());
        dto.setMonthlyCountLimit(entity.getMonthlyCountLimit());
        dto.setFeeAmount(entity.getFeeAmount());
        dto.setFeePercentage(entity.getFeePercentage());
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
    public List<PartnerTransactionLimit> toEntity(List<PartnerTransactionLimitDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<PartnerTransactionLimit> list = new ArrayList<>(dtoList.size());
        for (PartnerTransactionLimitDTO dto : dtoList) {
            list.add(toEntity(dto));
        }
        return list;
    }

    @Override
    public List<PartnerTransactionLimitDTO> toDto(List<PartnerTransactionLimit> entityList) {
        if (entityList == null) {
            return null;
        }
        List<PartnerTransactionLimitDTO> list = new ArrayList<>(entityList.size());
        for (PartnerTransactionLimit entity : entityList) {
            list.add(toDto(entity));
        }
        return list;
    }
}
