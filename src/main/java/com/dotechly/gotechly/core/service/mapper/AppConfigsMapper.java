package com.dotechly.gotechly.core.service.mapper;


import com.dotechly.gotechly.core.domain.AppConfigs;
import com.dotechly.gotechly.core.service.dto.AppConfigsDTO;
import com.dotechly.gotechly.core.service.impl.AppConfigsCryptoService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AppConfigsMapper implements EntityMapper<AppConfigsDTO, AppConfigs> {

    @Override
    public AppConfigs toEntity(AppConfigsDTO dto) {
        if (dto == null) {
            return null;
        }

        AppConfigs appConfigs = new AppConfigs();

        appConfigs.setId(dto.getId());
        appConfigs.setConfigId(dto.getConfigId());
        appConfigs.setConfigValue(dto.getConfigValue());
        appConfigs.setServiceType(dto.getServiceType());
        appConfigs.setConfigStatus(dto.getConfigStatus());
        appConfigs.setConfigBinValue(dto.getConfigBinValue());
        appConfigs.setSensitive(dto.isSensitive());

        return appConfigs;
    }

    @Override
    public AppConfigsDTO toDto(AppConfigs entity) {
        if (entity == null) {
            return null;
        }

        AppConfigsDTO appConfigsDTO = new AppConfigsDTO();

        appConfigsDTO.setId(entity.getId());
        appConfigsDTO.setConfigId(entity.getConfigId());
        // SECURITY: this DTO feeds every portal/REST response and the maker-checker
        // diff. For a sensitive row we return a fixed mask, never the stored value
        // (encrypted OR plaintext). Decryption for legitimate consumers happens only
        // on the plugin/SPI path (AppConfigsService.toSpi / findByConfigIdPrefix),
        // which does NOT go through this mapper. Do not "optimize" this masking away.
        appConfigsDTO.setConfigValue(
            entity.isSensitive() ? AppConfigsCryptoService.MASKED_VALUE : entity.getConfigValue());
        appConfigsDTO.setServiceType(entity.getServiceType());
        appConfigsDTO.setConfigStatus(entity.getConfigStatus());
        appConfigsDTO.setConfigBinValue(entity.getConfigBinValue());
        appConfigsDTO.setSensitive(entity.isSensitive());

        return appConfigsDTO;
    }

    @Override
    public List<AppConfigs> toEntity(List<AppConfigsDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        List<AppConfigs> list = new ArrayList<>(dtoList.size());
        for (AppConfigsDTO appConfigsDTO : dtoList) {
            list.add(toEntity(appConfigsDTO));
        }

        return list;
    }

    @Override
    public List<AppConfigsDTO> toDto(List<AppConfigs> entityList) {
        if (entityList == null) {
            return null;
        }

        List<AppConfigsDTO> list = new ArrayList<>(entityList.size());
        for (AppConfigs appConfigs : entityList) {
            list.add(toDto(appConfigs));
        }

        return list;
    }
}
