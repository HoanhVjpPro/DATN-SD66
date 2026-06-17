package com.example.datnhathub.service;

import com.example.datnhathub.dto.RegisterDto;
import com.example.datnhathub.entity.Customer;
import com.example.datnhathub.entity.Role;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.CustomerRepository;
import com.example.datnhathub.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // ════════════════════════════════════════
    // REGISTER
    // ════════════════════════════════════════
    @Transactional
    public String register(RegisterDto dto) {

        // 1. Validate username
        if (dto.getUsername() == null || dto.getUsername().trim().length() < 4) {
            return "Tên đăng nhập tối thiểu 4 ký tự!";
        }

        // 2. Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(dto.getUsername().trim())) {
            return "Tên đăng nhập đã tồn tại!";
        }

        // 3. Kiểm tra email đã tồn tại
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && userRepository.existsByEmail(dto.getEmail().trim())) {
            return "Email đã được sử dụng!";
        }

        // 4. Kiểm tra mật khẩu khớp
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return "Mật khẩu xác nhận không khớp!";
        }

        // 5. Tạo Users — RoleID = 3 (CUSTOMER)
        Users user = new Users();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(dto.getPassword());          // plain text theo yêu cầu
        user.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        user.setStatus(true);
        Role role = new Role();
        role.setRoleId(3);
        user.setRole(role);                 // RoleID = 3: CUSTOMER

        Users savedUser = userRepository.save(user);

        // 6. Tạo Customer — CustomerCode = "KH" + UserID
        Customer customer = new Customer();
        customer.setUser(savedUser);
        customer.setCustomerCode("KH" + savedUser.getUserID());
        customerRepository.save(customer);

        return null; // null = thành công, không có lỗi
    }

    // ════════════════════════════════════════
    // LOGIN
    // ════════════════════════════════════════
    public Users login(String username, String password) {

        // Tìm user theo username
        Users user = userRepository.findByUsername(username.trim()).orElse(null);

        if (user == null) return null;                    // không tồn tại

        if (!user.getPassword().equals(password)) return null; // sai mật khẩu

        if (!Boolean.TRUE.equals(user.getStatus())) return null; // bị khóa

        return user;
    }
}
