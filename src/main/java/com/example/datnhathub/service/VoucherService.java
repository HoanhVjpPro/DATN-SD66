package com.example.datnhathub.service;

import com.example.datnhathub.entity.Voucher;
import com.example.datnhathub.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class VoucherService {
    @Autowired
    private VoucherRepository voucherRepository;

    public List<Voucher> getAll() {
        return voucherRepository.findAll();
    }

    public Voucher getById(Integer id) {
        return voucherRepository.findById(id).orElse(null);
    }

    public Voucher save(Voucher voucher) {
        return voucherRepository.save(voucher);
    }

    public void delete(Integer id) {
        voucherRepository.deleteById(id);
    }

    @Transactional
    public BigDecimal applyVoucher(String code, BigDecimal subtotal) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không hợp lệ"));

        if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
            throw new IllegalArgumentException("Voucher đã hết lượt sử dụng");
        }

        voucher.setQuantity(voucher.getQuantity() - 1);
        voucherRepository.save(voucher);
        return voucher.calculateDiscount(subtotal);
    }

    // Kiểm tra voucher hợp lệ mà KHÔNG trừ số lượng (dùng để preview trước khi đặt hàng)
    public Voucher validateVoucher(String code) {
        Voucher voucher = voucherRepository.findByCode(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không tồn tại"));

        if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
            throw new IllegalArgumentException("Voucher đã hết lượt sử dụng");
        }
        if (voucher.isExpired()) {
            throw new IllegalArgumentException("Voucher đã hết hạn sử dụng");
        }
        return voucher;
    }

    @Transactional
    public void updateQuantityAndExpiry(Integer id, Integer quantity, LocalDate expiryDate) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy voucher"));
        voucher.setQuantity(quantity);
        voucher.setExpiryDate(expiryDate);
        voucherRepository.save(voucher);
    }
}
