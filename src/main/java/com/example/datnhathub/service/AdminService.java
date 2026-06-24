package com.example.datnhathub.service;

import com.example.datnhathub.entity.Employee;
import com.example.datnhathub.entity.Role;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.EmployeeRepository;
import com.example.datnhathub.repository.RoleRepository;
import com.example.datnhathub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    @Autowired
    private UserService userService;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private UserRepository userRepository;

    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void toggleUserStatus(Integer userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        user.setStatus(!Boolean.TRUE.equals(user.getStatus()));
        userRepository.save(user);
    }

    @Transactional
    public Users createEmployee(String username, String password, String email, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        Role role = roleRepository.findByRoleName("EMPLOYEE")
                .orElseThrow(() -> new IllegalStateException("Role EMPLOYEE chưa được cấu hình"));

        Users user = new Users();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(true);
        user.setRole(role);
        Users saved = userRepository.save(user);

        Employee employee = new Employee();
        employee.setUser(saved);
        employee.setEmployeeCode("NV" + saved.getUserID());
        employeeRepository.save(employee);
        return saved;
    }
}
