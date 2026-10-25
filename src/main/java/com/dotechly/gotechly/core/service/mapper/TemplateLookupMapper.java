package com.dotechly.gotechly.core.service.mapper;


import com.dotechly.gotechly.core.domain.TemplateLookup;
import com.dotechly.gotechly.core.service.dto.TemplateLookupDTO;
import com.dotechly.gotechly.spi.service.dto.db.TemplateLookupSPI;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class TemplateLookupMapper implements EntityMapper<TemplateLookupDTO, TemplateLookup> {

    @Override
    public TemplateLookup toEntity(TemplateLookupDTO dto) {
        if (dto == null) {
            return null;
        }
        TemplateLookup e = new TemplateLookup();
        e.setId(dto.getId());
        e.setTemplateName(dto.getTemplateName());
        e.setContentType(dto.getContentType());
        e.setReqTemplate(dto.getReqTemplate() == null ? null : dto.getReqTemplate());
        e.setUrl(dto.getUrl());
        e.setAdditionalHeader1(dto.getAdditionalHeader1());
        e.setAdditionalHeader2(dto.getAdditionalHeader2());
        e.setStatusExp(dto.getStatusExp());
        e.setTranIdExp(dto.getTranIdExp());
        e.setErrorExp(dto.getErrorExp());
        e.setTimeOutExp(dto.getTimeOutExp());
        e.setProviderCode(dto.getProviderCode());
        e.setParam1(dto.getParam1());
        e.setParam2(dto.getParam2());
        e.setModifiedBy(dto.getModifiedBy());
        e.setModifyDate(dto.getModifyDate());
        e.setHm(dto.getHm());
        e.setAdditionalFields(dto.getAdditionalFields());
        return e;
    }

    @Override
    public TemplateLookupDTO toDto(TemplateLookup entity) {
        if (entity == null) {
            return null;
        }
        TemplateLookupDTO dto = new TemplateLookupDTO();
        dto.setId(entity.getId());
        dto.setTemplateName(entity.getTemplateName());
        dto.setContentType(entity.getContentType());
        dto.setReqTemplate(entity.getReqTemplate() == null ? null : entity.getReqTemplate());
        dto.setUrl(entity.getUrl());
        dto.setAdditionalHeader1(entity.getAdditionalHeader1());
        dto.setAdditionalHeader2(entity.getAdditionalHeader2());
        dto.setStatusExp(entity.getStatusExp());
        dto.setTranIdExp(entity.getTranIdExp());
        dto.setErrorExp(entity.getErrorExp());
        dto.setTimeOutExp(entity.getTimeOutExp());
        dto.setProviderCode(entity.getProviderCode());
        dto.setParam1(entity.getParam1());
        dto.setParam2(entity.getParam2());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifyDate(entity.getModifyDate());
        dto.setHm(entity.getHm());
        dto.setAdditionalFields(entity.getAdditionalFields());
        return dto;
    }

    @Override
    public List<TemplateLookup> toEntity(List<TemplateLookupDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<TemplateLookup> out = new ArrayList<>(dtoList.size());
        for (TemplateLookupDTO dto : dtoList) {
            out.add(toEntity(dto));
        }
        return out;
    }

    @Override
    public List<TemplateLookupDTO> toDto(List<TemplateLookup> entityList) {
        if (entityList == null) {
            return null;
        }
        List<TemplateLookupDTO> out = new ArrayList<>(entityList.size());
        for (TemplateLookup e : entityList) {
            out.add(toDto(e));
        }
        return out;
    }

    public  TemplateLookupSPI toSPI(TemplateLookup dto) {
        if (dto == null) {
            return null;
        }

        TemplateLookupSPI spi = new TemplateLookupSPI();

        spi.setId(dto.getId());
        spi.setTemplateName(dto.getTemplateName());
        spi.setContentType(dto.getContentType());
        spi.setReqTemplate(dto.getReqTemplate());
        spi.setAdditionalFields(dto.getAdditionalFields());

        spi.setUrl(dto.getUrl());
        spi.setAdditionalHeader1(dto.getAdditionalHeader1());
        spi.setAdditionalHeader2(dto.getAdditionalHeader2());

        spi.setStatusExp(dto.getStatusExp());
        spi.setTranIdExp(dto.getTranIdExp());
        spi.setErrorExp(dto.getErrorExp());
        spi.setTimeOutExp(dto.getTimeOutExp());

        spi.setProviderCode(dto.getProviderCode());
        spi.setParam1(dto.getParam1());
        spi.setParam2(dto.getParam2());

        spi.setModifiedBy(dto.getModifiedBy());
        spi.setModifyDate(dto.getModifyDate());

        return spi;
    }
    public  TemplateLookupDTO toDTO(TemplateLookupSPI spi) {
        if (spi == null) {
            return null;
        }

        TemplateLookupDTO dto = new TemplateLookupDTO();

        dto.setId(spi.getId());
        dto.setTemplateName(spi.getTemplateName());
        dto.setContentType(spi.getContentType());
        dto.setReqTemplate(spi.getReqTemplate());
        dto.setAdditionalFields(spi.getAdditionalFields());

        dto.setUrl(spi.getUrl());
        dto.setAdditionalHeader1(spi.getAdditionalHeader1());
        dto.setAdditionalHeader2(spi.getAdditionalHeader2());

        dto.setStatusExp(spi.getStatusExp());
        dto.setTranIdExp(spi.getTranIdExp());
        dto.setErrorExp(spi.getErrorExp());
        dto.setTimeOutExp(spi.getTimeOutExp());

        dto.setProviderCode(spi.getProviderCode());
        dto.setParam1(spi.getParam1());
        dto.setParam2(spi.getParam2());

        dto.setModifiedBy(spi.getModifiedBy());
        dto.setModifyDate(spi.getModifyDate());

        return dto;
    }
}
