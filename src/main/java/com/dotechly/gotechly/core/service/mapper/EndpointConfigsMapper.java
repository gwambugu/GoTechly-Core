package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.EndpointConfigs;
import com.dotechly.gotechly.core.service.dto.EndpointConfigsDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EndpointConfigsMapper implements EntityMapper<EndpointConfigsDTO, EndpointConfigs> {

    @Override
    public EndpointConfigs toEntity(EndpointConfigsDTO dto) {
        if (dto == null) {
            return null;
        }

        EndpointConfigs endpointConfigs = new EndpointConfigs();

        endpointConfigs.setId(dto.getId());
        endpointConfigs.setServiceName(dto.getServiceName());
        endpointConfigs.setServiceType(dto.getServiceType());
        endpointConfigs.setRequestType(dto.getRequestType());
        endpointConfigs.setReqTemplate(dto.getReqTemplate());
        endpointConfigs.setEndpoint(dto.getEndpoint());
        endpointConfigs.setContentType(dto.getContentType());
        endpointConfigs.setAdditionalHeader1(dto.getAdditionalHeader1());
        endpointConfigs.setAdditionalHeader2(dto.getAdditionalHeader2());
        endpointConfigs.setAdditionalHeader3(dto.getAdditionalHeader3());
        endpointConfigs.setStatusExp(dto.getStatusExp());
        endpointConfigs.setTranIDExp(dto.getTranIDExp());
        endpointConfigs.setErrorExp(dto.getErrorExp());
        endpointConfigs.setTimeout(dto.getTimeout());
        endpointConfigs.setTimeOutExp(dto.getTimeOutExp());
        endpointConfigs.setProviderCode(dto.getProviderCode());
        endpointConfigs.setParam1(dto.getParam1());
        endpointConfigs.setParam2(dto.getParam2());
        endpointConfigs.setParam3(dto.getParam3());
        endpointConfigs.setAuthType(dto.getAuthType());
        endpointConfigs.setModifiedBy(dto.getModifiedBy());
        endpointConfigs.setModifyDate(dto.getModifyDate());
        endpointConfigs.setHm(dto.getHm());

        return endpointConfigs;
    }

    @Override
    public EndpointConfigsDTO toDto(EndpointConfigs entity) {
        if (entity == null) {
            return null;
        }

        EndpointConfigsDTO endpointConfigsDTO = new EndpointConfigsDTO();

        endpointConfigsDTO.setId(entity.getId());
        endpointConfigsDTO.setServiceName(entity.getServiceName());
        endpointConfigsDTO.setServiceType(entity.getServiceType());
        endpointConfigsDTO.setRequestType(entity.getRequestType());
        endpointConfigsDTO.setReqTemplate(entity.getReqTemplate());
        endpointConfigsDTO.setEndpoint(entity.getEndpoint());
        endpointConfigsDTO.setContentType(entity.getContentType());
        endpointConfigsDTO.setAdditionalHeader1(entity.getAdditionalHeader1());
        endpointConfigsDTO.setAdditionalHeader2(entity.getAdditionalHeader2());
        endpointConfigsDTO.setAdditionalHeader3(entity.getAdditionalHeader3());
        endpointConfigsDTO.setStatusExp(entity.getStatusExp());
        endpointConfigsDTO.setTranIDExp(entity.getTranIDExp());
        endpointConfigsDTO.setErrorExp(entity.getErrorExp());
        endpointConfigsDTO.setTimeout(entity.getTimeout());
        endpointConfigsDTO.setTimeOutExp(entity.getTimeOutExp());
        endpointConfigsDTO.setProviderCode(entity.getProviderCode());
        endpointConfigsDTO.setParam1(entity.getParam1());
        endpointConfigsDTO.setParam2(entity.getParam2());
        endpointConfigsDTO.setParam3(entity.getParam3());
        endpointConfigsDTO.setAuthType(entity.getAuthType());
        endpointConfigsDTO.setModifiedBy(entity.getModifiedBy());
        endpointConfigsDTO.setModifyDate(entity.getModifyDate());
        endpointConfigsDTO.setHm(entity.getHm());

        return endpointConfigsDTO;
    }

    @Override
    public List<EndpointConfigs> toEntity(List<EndpointConfigsDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        List<EndpointConfigs> list = new ArrayList<>(dtoList.size());
        for (EndpointConfigsDTO endpointConfigsDTO : dtoList) {
            list.add(toEntity(endpointConfigsDTO));
        }

        return list;
    }

    @Override
    public List<EndpointConfigsDTO> toDto(List<EndpointConfigs> entityList) {
        if (entityList == null) {
            return null;
        }

        List<EndpointConfigsDTO> list = new ArrayList<>(entityList.size());
        for (EndpointConfigs endpointConfigs : entityList) {
            list.add(toDto(endpointConfigs));
        }

        return list;
    }
}
