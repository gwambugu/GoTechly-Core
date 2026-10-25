package com.dotechly.gotechly.core.service.mapper;


import com.dotechly.gotechly.core.domain.BankCodesLookup;
import com.dotechly.gotechly.core.service.dto.BankCodesLookupDTO;
import com.dotechly.gotechly.spi.service.dto.db.BankCodesLookupSPI;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BankCodesLookupMapper implements EntityMapper<BankCodesLookupDTO, BankCodesLookup> {

    @Override
    public BankCodesLookup toEntity(BankCodesLookupDTO dto) {
        if (dto == null) {
            return null;
        }
        BankCodesLookup e = new BankCodesLookup();
        e.setId(dto.getId());
        e.setCountryCode(dto.getCountryCode());
        e.setBankCode(dto.getBankCode());
        e.setBankName(dto.getBankName());
        e.setBankShortName(dto.getBankShortName());
        e.setBankBranchCode(dto.getBankBranchCode());
        e.setBic(dto.getBic());
        e.setPic(dto.getPic());
        e.setBin(dto.getBin());
        e.setAdditionalFields(dto.getAdditionalFields());
        e.setStatus(dto.getStatus());
        return e;
    }

    @Override
    public BankCodesLookupDTO toDto(BankCodesLookup entity) {
        if (entity == null) {
            return null;
        }
        BankCodesLookupDTO dto = new BankCodesLookupDTO();
        dto.setId(entity.getId());
        dto.setCountryCode(entity.getCountryCode());
        dto.setBankCode(entity.getBankCode());
        dto.setBankName(entity.getBankName());
        dto.setBankShortName(entity.getBankShortName());
        dto.setBankBranchCode(entity.getBankBranchCode());
        dto.setBic(entity.getBic());
        dto.setPic(entity.getPic());
        dto.setBin(entity.getBin());
        dto.setAdditionalFields(entity.getAdditionalFields());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    @Override
    public List<BankCodesLookup> toEntity(List<BankCodesLookupDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<BankCodesLookup> out = new ArrayList<>(dtoList.size());
        for (BankCodesLookupDTO dto : dtoList) {
            out.add(toEntity(dto));
        }
        return out;
    }

    @Override
    public List<BankCodesLookupDTO> toDto(List<BankCodesLookup> entityList) {
        if (entityList == null) {
            return null;
        }
        List<BankCodesLookupDTO> out = new ArrayList<>(entityList.size());
        for (BankCodesLookup e : entityList) {
            out.add(toDto(e));
        }
        return out;
    }

    public  BankCodesLookupSPI toSPI(BankCodesLookup dto) {
        if (dto == null) {
            return null;
        }

        BankCodesLookupSPI spi = new BankCodesLookupSPI();
        spi.setId(dto.getId());
        spi.setCountryCode(dto.getCountryCode());
        spi.setBankCode(dto.getBankCode());
        spi.setBankName(dto.getBankName());
        spi.setBankShortName(dto.getBankShortName());
        spi.setBankBranchCode(dto.getBankBranchCode());
        spi.setBic(dto.getBic());
        spi.setPic(dto.getPic());
        spi.setBin(dto.getBin());
        spi.setAdditionalFields(dto.getAdditionalFields());
        spi.setStatus(dto.getStatus());

        return spi;
    }

    public  BankCodesLookupDTO toDTO(BankCodesLookupSPI spi) {
        if (spi == null) {
            return null;
        }
        BankCodesLookupDTO dto = new BankCodesLookupDTO();
        dto.setId(spi.getId());
        dto.setCountryCode(spi.getCountryCode());
        dto.setBankCode(spi.getBankCode());
        dto.setBankName(spi.getBankName());
        dto.setBankShortName(spi.getBankShortName());
        dto.setBankBranchCode(spi.getBankBranchCode());
        dto.setBic(spi.getBic());
        dto.setPic(spi.getPic());
        dto.setBin(spi.getBin());
        dto.setAdditionalFields(spi.getAdditionalFields());
        dto.setStatus(spi.getStatus());

        return dto;
    }
}
