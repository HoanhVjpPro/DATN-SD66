package com.example.datnhathub.repository;

import com.example.datnhathub.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {
    // Tìm user theo username (dùng cho login)
    Optional<Users> findByUsername(String username);

    // Kiểm tra username đã tồn tại chưa (dùng cho register)
    boolean existsByUsername(String username);

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);

    boolean existsByPhone(String phone);
}
