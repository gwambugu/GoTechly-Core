package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.Channels;
import com.dotechly.gotechly.core.service.dto.ChannelsDTO;
import com.dotechly.gotechly.spi.service.dto.db.ChannelsSPI;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class ChannelsMapper implements EntityMapper<ChannelsDTO, Channels> {

    @Override
    public Channels toEntity(ChannelsDTO dto) {
        if (dto == null) {
            return null;
        }
        Channels channels = new Channels();
        channels.setId(dto.getId());
        channels.setChannelName(dto.getChannelName());
        channels.setSystemName(dto.getSystemName());
        channels.setApiName(dto.getApiName());
        channels.setCallbackUrl(dto.getCallbackUrl());
        channels.setCallbackRequestTemplate(dto.getCallbackRequestTemplate());
        channels.setAuthCertificate(dto.getAuthCertificate());
        channels.setContentType(dto.getContentType());
        channels.setExpectedResponse(dto.getExpectedResponse());
        channels.setAuthType(dto.getAuthType());
        channels.setAdditionalFields(dto.getAdditionalFields());
        channels.setSourceIps(dto.getSourceIps());
        channels.setStatus(dto.getStatus());
        channels.setDateCreated(dto.getDateCreated());
        channels.setCreatedBy(dto.getCreatedBy());
        channels.setDateModified(dto.getDateModified());
        channels.setModifiedBy(dto.getModifiedBy());
        return channels;
    }

    @Override
    public ChannelsDTO toDto(Channels entity) {
        if (entity == null) {
            return null;
        }
        ChannelsDTO channelsDTO = new ChannelsDTO();
        channelsDTO.setId(entity.getId());
        channelsDTO.setChannelName(entity.getChannelName());
        channelsDTO.setSystemName(entity.getSystemName());
        channelsDTO.setApiName(entity.getApiName());
        channelsDTO.setCallbackUrl(entity.getCallbackUrl());
        channelsDTO.setCallbackRequestTemplate(entity.getCallbackRequestTemplate());
        channelsDTO.setAuthCertificate(entity.getAuthCertificate());
        channelsDTO.setContentType(entity.getContentType());
        channelsDTO.setExpectedResponse(entity.getExpectedResponse());
        channelsDTO.setAuthType(entity.getAuthType());
        channelsDTO.setAdditionalFields(entity.getAdditionalFields());
        channelsDTO.setSourceIps(entity.getSourceIps());
        channelsDTO.setStatus(entity.getStatus());
        channelsDTO.setDateCreated(entity.getDateCreated());
        channelsDTO.setCreatedBy(entity.getCreatedBy());
        channelsDTO.setDateModified(entity.getDateModified());
        channelsDTO.setModifiedBy(entity.getModifiedBy());
        return channelsDTO;
    }

    @Override
    public List<Channels> toEntity(List<ChannelsDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<Channels> list = new ArrayList<>(dtoList.size());
        for (ChannelsDTO dto : dtoList) {
            list.add(toEntity(dto));
        }
        return list;
    }

    @Override
    public List<ChannelsDTO> toDto(List<Channels> entityList) {
        if (entityList == null) {
            return null;
        }
        List<ChannelsDTO> list = new ArrayList<>(entityList.size());
        for (Channels entity : entityList) {
            list.add(toDto(entity));
        }
        return list;
    }

    public  ChannelsSPI toSPI(Channels entity) {
        if (entity == null) {
            return null;
        }

        ChannelsSPI spi = new ChannelsSPI();

        spi.setId(entity.getId());
        spi.setChannelName(entity.getChannelName());
        spi.setSystemName(entity.getSystemName());
        spi.setApiName(entity.getApiName());
        spi.setCallbackUrl(entity.getCallbackUrl());
        spi.setCallbackRequestTemplate(entity.getCallbackRequestTemplate());
        spi.setAuthCertificate(entity.getAuthCertificate());
        spi.setContentType(entity.getContentType());
        spi.setExpectedResponse(entity.getExpectedResponse());
        spi.setAuthType(entity.getAuthType());
        spi.setSourceIps(entity.getSourceIps());
        spi.setStatus(entity.getStatus());
        spi.setDateCreated(entity.getDateCreated());
        spi.setCreatedBy(entity.getCreatedBy());
        spi.setDateModified(entity.getDateModified());
        spi.setModifiedBy(entity.getModifiedBy());

        if (entity.getAdditionalFields() != null) {
            spi.setAdditionalFields(new HashMap<>(entity.getAdditionalFields()));
        }

        return spi;
    }

    public  Channels toEntity(ChannelsSPI spi) {
        if (spi == null) {
            return null;
        }

        Channels entity = new Channels();

        entity.setId(spi.getId());
        entity.setChannelName(spi.getChannelName());
        entity.setSystemName(spi.getSystemName());
        entity.setApiName(spi.getApiName());
        entity.setCallbackUrl(spi.getCallbackUrl());
        entity.setCallbackRequestTemplate(spi.getCallbackRequestTemplate());
        entity.setAuthCertificate(spi.getAuthCertificate());
        entity.setContentType(spi.getContentType());
        entity.setExpectedResponse(spi.getExpectedResponse());
        entity.setAuthType(spi.getAuthType());
        entity.setSourceIps(spi.getSourceIps());
        entity.setStatus(spi.getStatus());
        entity.setDateCreated(spi.getDateCreated());
        entity.setCreatedBy(spi.getCreatedBy());
        entity.setDateModified(spi.getDateModified());
        entity.setModifiedBy(spi.getModifiedBy());

        if (spi.getAdditionalFields() != null) {
            entity.setAdditionalFields(new HashMap<>(spi.getAdditionalFields()));
        }

        return entity;
    }
}
