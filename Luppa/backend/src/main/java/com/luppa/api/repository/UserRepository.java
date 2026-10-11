package com.luppa.api.repository;

import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query(value = """
            select
              (select count(*) from evento where idUsuario = :id)
            + (select count(*) from denuncias where idUsuario = :id)
            + (select count(*) from avaliacao where idUsuario = :id)
            + (select count(*) from eventoSalvo where idUsuario = :id)
            """, nativeQuery = true)
    long countBlockingReferences(@Param("id") Integer id);

    @Query("""
            select u from User u
            where (:search is null or lower(u.firstName) like lower(concat('%', :search, '%'))
                   or lower(u.lastName) like lower(concat('%', :search, '%'))
                   or lower(u.email) like lower(concat('%', :search, '%')))
              and (:role is null or u.role.id = :roleId)
            """)
    Page<User> search(
            @Param("search") String search,
            @Param("role") UserRole role,
            @Param("roleId") Integer roleId,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
