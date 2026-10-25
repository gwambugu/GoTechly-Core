package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.ErrorCodes;
import com.dotechly.gotechly.core.service.dto.ErrorCodesDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ErrorCodesMapper implements EntityMapper<ErrorCodesDTO, ErrorCodes> {

    @Override
    public ErrorCodes toEntity(ErrorCodesDTO dto) {
        if (dto == null) {
            return null;
        }
        ErrorCodes errorCodes = new ErrorCodes();
        errorCodes.setId(dto.getId());
        errorCodes.setApiName(dto.getApiName());
        errorCodes.setServiceName(dto.getServiceName());
        errorCodes.setServiceType(dto.getServiceType());
        errorCodes.setRequestType(dto.getRequestType());
        errorCodes.setErrorCode(dto.getErrorCode());
        errorCodes.setErrorDescription(dto.getErrorDescription());
        errorCodes.setTransactionState(dto.getTransactionState());
        return errorCodes;
    }

    @Override
    public ErrorCodesDTO toDto(ErrorCodes entity) {
        if (entity == null) {
            return null;
        }
        ErrorCodesDTO errorCodesDTO = new ErrorCodesDTO();
        errorCodesDTO.setId(entity.getId());
        errorCodesDTO.setApiName(entity.getApiName());
        errorCodesDTO.setServiceName(entity.getServiceName());
        errorCodesDTO.setServiceType(entity.getServiceType());
        errorCodesDTO.setRequestType(entity.getRequestType());
        errorCodesDTO.setErrorCode(entity.getErrorCode());
        errorCodesDTO.setErrorDescription(entity.getErrorDescription());
        errorCodesDTO.setTransactionState(entity.getTransactionState());
        return errorCodesDTO;
    }

    @Override
    public List<ErrorCodes> toEntity(List<ErrorCodesDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<ErrorCodes> list = new ArrayList<>(dtoList.size());
        for (ErrorCodesDTO errorCodesDTO : dtoList) {
            list.add(toEntity(errorCodesDTO));
        }
        return list;
    }

    @Override
    public List<ErrorCodesDTO> toDto(List<ErrorCodes> entityList) {
        if (entityList == null) {
            return null;
        }
        List<ErrorCodesDTO> list = new ArrayList<>(entityList.size());
        for (ErrorCodes errorCodes : entityList) {
            list.add(toDto(errorCodes));
        }
        return list;
    }
}
