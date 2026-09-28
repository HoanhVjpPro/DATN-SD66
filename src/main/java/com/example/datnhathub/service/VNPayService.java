package com.example.datnhathub.service;

import com.example.datnhathub.config.VNPayUtil;
import com.example.datnhathub.entity.Orders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    @Value("${vnp.payUrl}")
    private String vnpPayUrl;

    @Value("${vnp.returnUrl}")
    private String vnpReturnUrl;

    @Value("${vnp.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnp.hashSecret}")
    private String secretKey;

    public String createPaymentUrl(Orders order, HttpServletRequest request) {

        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ.");
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ.");
        }

        long amount = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";

        String vnp_TxnRef = String.valueOf(order.getOrderId());
        String vnp_IpAddr = getClientIp(request);
        Map<String, String> vnp_Params = new HashMap<>();

        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put( "vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl);
        vnp_Params.put("vnp_IpAddr",  vnp_IpAddr);

        TimeZone timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(timeZone);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(timeZone);

        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());

        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {

            String fieldName = fieldNames.get(i);
            String fieldValue = vnp_Params.get(fieldName);

            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }

            String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII);
            String encodedFieldName = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII);

            hashData.append(fieldName);
            hashData.append("=");
            hashData.append(encodedValue);
            query.append(encodedFieldName);
            query.append("=");
            query.append(encodedValue);

            if (i < fieldNames.size() - 1) {
                hashData.append("&");
                query.append("&");
            }
        }

        String vnp_SecureHash = VNPayUtil.hmacSHA512(secretKey, hashData.toString());

        return vnpPayUrl
                + "?"
                + query
                + "&vnp_SecureHash="
                + vnp_SecureHash;
    }

    public boolean verifyReturn(Map<String, String> params, String secureHash) {
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }

        Map<String, String> data = new HashMap<>(params);

        data.remove("vnp_SecureHash");
        data.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(data.keySet());

        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {

            String fieldName = fieldNames.get(i);

            String fieldValue = data.get(fieldName);

            if (fieldValue == null || fieldValue.isBlank()) {
                continue;
            }
            hashData.append(fieldName);

            hashData.append("=");
            hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

            if (i < fieldNames.size() - 1) {
                hashData.append("&");
            }
        }

        String calculatedHash = VNPayUtil.hmacSHA512(secretKey, hashData.toString());
        return secureHash.equalsIgnoreCase(
                calculatedHash
        );
    }

    public String getTmnCode() {
        return vnpTmnCode;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For" );

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");

        if (xRealIp != null && !xRealIp.isBlank()) {

            return xRealIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        // IPv6 (vd 0:0:0:0:0:0:0:1 khi chạy localhost) bị VNPay sandbox từ chối -> dùng IPv4
        if (remoteAddr == null || remoteAddr.isBlank() || remoteAddr.contains(":")) {
            return "127.0.0.1";
        }
        return remoteAddr;
    }
}