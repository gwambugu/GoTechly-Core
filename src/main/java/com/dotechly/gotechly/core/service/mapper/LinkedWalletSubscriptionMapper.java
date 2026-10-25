package com.dotechly.gotechly.core.service.mapper;


import com.dotechly.gotechly.core.domain.LinkedWalletSubscription;
import com.dotechly.gotechly.core.service.dto.LinkedWalletSubscriptionDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LinkedWalletSubscriptionMapper
        implements EntityMapper<LinkedWalletSubscriptionDTO, LinkedWalletSubscription> {

    @Override
    public LinkedWalletSubscription toEntity(LinkedWalletSubscriptionDTO dto) {
        if (dto == null) {
            return null;
        }
        LinkedWalletSubscription e = new LinkedWalletSubscription();
        e.setId(dto.getId());
        e.setPaymentType(dto.getPaymentType());
        e.setAliasName(dto.getAliasName());
        e.setRequestId(dto.getRequestId());
        e.setDateLinked(dto.getDateLinked());
        e.setServiceType(dto.getServiceType());
        e.setPspName(dto.getPspName());
        e.setMobilePhoneNumber(dto.getMobilePhoneNumber());
        e.setSavingsAccountNumber(dto.getSavingsAccountNumber());
        e.setOperatingCurrencyCode(dto.getOperatingCurrencyCode());
        e.setFirstName(dto.getFirstName());
        e.setMiddleName(dto.getMiddleName());
        e.setLastName(dto.getLastName());
        e.setDateOfBirth(dto.getDateOfBirth());
        e.setNationalId(dto.getNationalId());
        e.setPassportNumber(dto.getPassportNumber());
        e.setCountryCode(dto.getCountryCode());
        e.setCityTown(dto.getCityTown());
        e.setAddress1(dto.getAddress1());
        e.setAddress2(dto.getAddress2());
        e.setPostalCode(dto.getPostalCode());
        e.setPostalAddress(dto.getPostalAddress());
        e.setOpeningDate(dto.getOpeningDate());
        e.setAgentNumber(dto.getAgentNumber());
        e.setAgentName(dto.getAgentName());
        e.setBankName(dto.getBankName());
        e.setBankShortCode(dto.getBankShortCode());
        e.setReferenceNumber(dto.getReferenceNumber());
        e.setActionCode(dto.getActionCode());
        e.setStatus(dto.getStatus());
        e.setRemarks(dto.getRemarks());
        e.setPspDob(dto.getPspDob());
        e.setPspNationalId(dto.getPspNationalId());
        e.setPspPhone(dto.getPspPhone());
        e.setCreateDate(dto.getCreateDate());
        e.setLinkStatus(dto.getLinkStatus());
        e.setLinkTranId(dto.getLinkTranId());
        e.setVmtRefId(dto.getVmtRefId());
        e.setVmtRef(dto.getVmtRef());
        e.setCreditQualified(dto.getCreditQualified());
        e.setLoanAccount(dto.getLoanAccount());
        e.setLoanBalance(dto.getLoanBalance());
        e.setHash1(dto.getHash1());
        e.setHash2(dto.getHash2());
        e.setAdditionalFields(dto.getAdditionalFields());
        return e;
    }

    @Override
    public LinkedWalletSubscriptionDTO toDto(LinkedWalletSubscription entity) {
        if (entity == null) {
            return null;
        }
        LinkedWalletSubscriptionDTO dto = new LinkedWalletSubscriptionDTO();
        dto.setId(entity.getId());
        dto.setPaymentType(entity.getPaymentType());
        dto.setAliasName(entity.getAliasName());
        dto.setRequestId(entity.getRequestId());
        dto.setDateLinked(entity.getDateLinked());
        dto.setServiceType(entity.getServiceType());
        dto.setPspName(entity.getPspName());
        dto.setMobilePhoneNumber(entity.getMobilePhoneNumber());
        dto.setSavingsAccountNumber(entity.getSavingsAccountNumber());
        dto.setOperatingCurrencyCode(entity.getOperatingCurrencyCode());
        dto.setFirstName(entity.getFirstName());
        dto.setMiddleName(entity.getMiddleName());
        dto.setLastName(entity.getLastName());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setNationalId(entity.getNationalId());
        dto.setPassportNumber(entity.getPassportNumber());
        dto.setCountryCode(entity.getCountryCode());
        dto.setCityTown(entity.getCityTown());
        dto.setAddress1(entity.getAddress1());
        dto.setAddress2(entity.getAddress2());
        dto.setPostalCode(entity.getPostalCode());
        dto.setPostalAddress(entity.getPostalAddress());
        dto.setOpeningDate(entity.getOpeningDate());
        dto.setAgentNumber(entity.getAgentNumber());
        dto.setAgentName(entity.getAgentName());
        dto.setBankName(entity.getBankName());
        dto.setBankShortCode(entity.getBankShortCode());
        dto.setReferenceNumber(entity.getReferenceNumber());
        dto.setActionCode(entity.getActionCode());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setPspDob(entity.getPspDob());
        dto.setPspNationalId(entity.getPspNationalId());
        dto.setPspPhone(entity.getPspPhone());
        dto.setCreateDate(entity.getCreateDate());
        dto.setLinkStatus(entity.getLinkStatus());
        dto.setLinkTranId(entity.getLinkTranId());
        dto.setVmtRefId(entity.getVmtRefId());
        dto.setVmtRef(entity.getVmtRef());
        dto.setCreditQualified(entity.getCreditQualified());
        dto.setLoanAccount(entity.getLoanAccount());
        dto.setLoanBalance(entity.getLoanBalance());
        dto.setHash1(entity.getHash1());
        dto.setHash2(entity.getHash2());
        dto.setAdditionalFields(entity.getAdditionalFields());
        return dto;
    }

    @Override
    public List<LinkedWalletSubscription> toEntity(List<LinkedWalletSubscriptionDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<LinkedWalletSubscription> out = new ArrayList<>(dtoList.size());
        for (LinkedWalletSubscriptionDTO dto : dtoList) {
            out.add(toEntity(dto));
        }
        return out;
    }

    @Override
    public List<LinkedWalletSubscriptionDTO> toDto(List<LinkedWalletSubscription> entityList) {
        if (entityList == null) {
            return null;
        }
        List<LinkedWalletSubscriptionDTO> out = new ArrayList<>(entityList.size());
        for (LinkedWalletSubscription e : entityList) {
            out.add(toDto(e));
        }
        return out;
    }
}
