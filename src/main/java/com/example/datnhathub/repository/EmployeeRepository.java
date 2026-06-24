package com.example.datnhathub.repository;

import com.example.datnhathub.entity.Employee;
import com.example.datnhathub.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    Optional<Employee> findByUser(Users user);
    Optional<Employee> findByUserUserID(Integer userId);
}
