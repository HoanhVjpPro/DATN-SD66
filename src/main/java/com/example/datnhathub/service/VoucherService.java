package com.example.datnhathub.service;

import com.example.datnhathub.entity.Voucher;
import com.example.datnhathub.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    public BigDecimal applyVoucher(String code) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không hợp lệ"));

        if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
            throw new IllegalArgumentException("Voucher đã hết lượt sử dụng");
        }

        voucher.setQuantity(voucher.getQuantity() - 1);
        voucherRepository.save(voucher);
        return voucher.getDiscountAmount() != null ? voucher.getDiscountAmount() : BigDecimal.ZERO;
    }
}
