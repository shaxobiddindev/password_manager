package com.company.passwordmanager.repository;

import com.company.passwordmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByLogin(String login);
    boolean existsByEmail(String email);
    boolean existsByLogin(String login);
    java.util.List<User> findAllByRole(User.Role role);
    java.util.List<User> findByLoginContainingIgnoreCaseOrEmailContainingIgnoreCase(String login, String email);
}
