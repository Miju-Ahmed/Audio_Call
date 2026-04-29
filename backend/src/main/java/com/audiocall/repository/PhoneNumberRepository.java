package com.audiocall.repository;

import com.audiocall.model.PhoneNumber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, Long> {

    Optional<PhoneNumber> findByNumber(String number);

    boolean existsByNumber(String number);

    List<PhoneNumber> findByActiveTrue();

    List<PhoneNumber> findByGroupAndActiveTrue(String group);

    Page<PhoneNumber> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM PhoneNumber p WHERE p.active = true AND " +
           "(:group IS NULL OR p.group = :group) AND " +
           "(:search IS NULL OR p.number LIKE %:search% OR p.name LIKE %:search%)")
    Page<PhoneNumber> findFiltered(@Param("group") String group,
                                   @Param("search") String search,
                                   Pageable pageable);

    @Query("SELECT DISTINCT p.group FROM PhoneNumber p WHERE p.group IS NOT NULL ORDER BY p.group")
    List<String> findAllGroups();

    long countByActiveTrue();
}
