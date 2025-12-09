package com.osiansoftware.usermanagementsystem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    List<UserEntity> findByDeletedFalse();
    Optional<UserEntity> findByIdAndDeletedFalse(Long id);

    @Modifying
    @Query(value = "DELETE FROM users WHERE deleted = true AND deleted_At < :cutoff", nativeQuery = true)
    int hardDeleteOlderThan(LocalDateTime cutoff);

}
