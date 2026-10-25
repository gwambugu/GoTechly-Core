package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.CbsNodeLease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Claiming a STAN node ordinal.
 *
 * <p>Every write here is a <b>conditional</b> bulk update whose {@code where} clause carries the
 * precondition, and the row count is the answer. That is deliberate: read-then-write would let two
 * instances both read a free ordinal and both write it, which is precisely the duplicate this table
 * exists to prevent. Oracle re-evaluates the predicate after acquiring the row lock, so of two
 * instances racing the same ordinal exactly one updates a row and the other updates none.
 */
@Repository
public interface CbsNodeLeaseRepository extends JpaRepository<CbsNodeLease, Integer> {

    /**
     * Rows naming this instance, lowest ordinal first.
     *
     * <p><b>A list, not an Optional.</b> {@code owner} is deliberately not unique (see the entity),
     * so a query that assumed a single row would throw
     * {@code IncorrectResultSizeDataAccessException} if an instance ever named two - turning a stray
     * extra row into a Core that cannot boot. Returning them all lets the caller keep the lowest and
     * put the rest back in the pool.
     */
    List<CbsNodeLease> findByOwnerOrderByNodeIdAsc(String owner);

    @Query("select l from CbsNodeLease l order by l.nodeId asc")
    List<CbsNodeLease> findAllOrdered();

    /**
     * Takes an ordinal, but only if it is free, already ours, or held by an instance that has stopped
     * checking in. Returns the number of rows updated: 1 on success, 0 if someone else got there
     * first.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CbsNodeLease l
               set l.owner = :owner, l.leasedAt = :now, l.heartbeatAt = :now
             where l.nodeId = :nodeId
               and (l.owner is null or l.owner = :owner or l.heartbeatAt is null
                    or l.heartbeatAt < :staleBefore)
            """)
    int claim(@Param("nodeId") int nodeId,
              @Param("owner") String owner,
              @Param("now") Instant now,
              @Param("staleBefore") Instant staleBefore);

    /**
     * Proof of life. Returns 0 when the ordinal is no longer ours — which is how the owner discovers
     * it was declared stale and taken over, rather than carrying on emitting STANs from a block
     * another instance is now using.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CbsNodeLease l set l.heartbeatAt = :now where l.nodeId = :nodeId and l.owner = :owner")
    int heartbeat(@Param("nodeId") int nodeId,
                  @Param("owner") String owner,
                  @Param("now") Instant now);

    /**
     * Gives back every ordinal this instance holds. Called on clean shutdown so a redeploy does not
     * have to wait out the stale window, and before a fresh claim so an instance moving ordinals
     * cannot end up owning two.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CbsNodeLease l set l.owner = null, l.leasedAt = null, l.heartbeatAt = null where l.owner = :owner")
    int releaseAllOwnedBy(@Param("owner") String owner);
}
