package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.Channels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Channels entity.
 */
@Repository
public interface ChannelsRepository extends JpaRepository<Channels, Long>, JpaSpecificationExecutor<Channels> {
    Optional<Channels> findByChannelName(String channelName);

    Optional<Channels> findBySystemNameAndApiName(String systemName, String apiName);
    Optional<Channels> findBySystemName(String systemName);
}
