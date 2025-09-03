package com.rewardplatform.user.repository;

import com.rewardplatform.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByLoginProviderAndProviderId(User.LoginProvider provider, String providerId);
}
