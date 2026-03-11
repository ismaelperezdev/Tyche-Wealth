package com.tychewealth.repository;

import com.tychewealth.entity.RefreshTokenEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

  /**
 * Locate a refresh token record by its token string.
 *
 * @param token the refresh token value to search for
 * @return an Optional containing the matching RefreshTokenEntity if found, or Optional.empty() otherwise
 */
Optional<RefreshTokenEntity> findByToken(String token);

  /**
       * Revoke all non-revoked refresh tokens for the specified user that have not yet expired.
       *
       * @param userId      the identifier of the user whose active refresh tokens will be revoked
       * @param currentTime reference time; only tokens with expiresAt after this time are considered active
       * @return            the number of rows updated
       */
      @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update RefreshTokenEntity rt
         set rt.revoked = true
       where rt.user.id = :userId
         and rt.revoked = false
         and rt.expiresAt > :currentTime
      """)
  int revokeActiveTokensByUserId(
      @Param("userId") Long userId, @Param("currentTime") Instant currentTime);

  /**
   * Mark the specified refresh token as revoked if it is currently active.
   *
   * @param token       the refresh token string to revoke
   * @param currentTime reference time used to determine token activity; only tokens with expiresAt after this time are eligible
   * @return            the number of rows updated (the number of tokens revoked)
   */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update RefreshTokenEntity rt
         set rt.revoked = true
       where rt.token = :token
         and rt.revoked = false
         and rt.expiresAt > :currentTime
      """)
  int revokeTokenIfActive(@Param("token") String token, @Param("currentTime") Instant currentTime);
}
