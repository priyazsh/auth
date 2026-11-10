package me.priyazsh.auth.repository;

import me.priyazsh.auth.entity.AuthorizationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthorizationCodeRepository extends JpaRepository<AuthorizationCode, UUID> {

    Optional<AuthorizationCode> findByCode(String code);

    void deleteByCode(String code);
}
