package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Orders;
import com.example.datnhathub.repository.OrderRepository;
import com.example.datnhathub.service.InvoiceService;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Controller
public class AdminExportController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/admin/orders/{id}/invoice/pdf")
    public ResponseEntity<byte[]> exportInvoicePdf(@PathVariable Integer id) throws Exception {

        Orders order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        byte[] pdfBytes = invoiceService.generateInvoicePdf(order);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=hoadon-donhang-" + order.getOrderCode() + ".pdf");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // Xuất Excel doanh thu
    @GetMapping("/admin/dashboard/export/excel")
    public ResponseEntity<byte[]> exportRevenueExcel() throws Exception {

        List<OrderRepository.MonthlyRevenueProjection> monthlyRevenue = orderRepository.getMonthlyRevenue();
        List<OrderRepository.TopProductProjection> topProducts = orderRepository.getTopSellingProducts();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font poiFont = workbook.createFont();
            poiFont.setBold(true);
            poiFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(poiFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Sheet 1: Doanh thu theo tháng
            Sheet sheet1 = workbook.createSheet("Doanh thu theo thang");
            Row header1 = sheet1.createRow(0);
            String[] cols1 = {"Tháng", "Doanh thu (đ)"};
            for (int i = 0; i < cols1.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header1.createCell(i);
                c.setCellValue(cols1[i]);
                c.setCellStyle(headerStyle);
            }
            int rowIdx = 1;
            for (var m : monthlyRevenue) {
                Row row = sheet1.createRow(rowIdx++);
                row.createCell(0).setCellValue(m.getMonth());
                row.createCell(1).setCellValue(m.getRevenue() != null ? m.getRevenue().doubleValue() : 0);
            }
            sheet1.autoSizeColumn(0);
            sheet1.autoSizeColumn(1);

            // Sheet 2: Top sản phẩm bán chạy
            Sheet sheet2 = workbook.createSheet("Top san pham ban chay");
            Row header2 = sheet2.createRow(0);
            String[] cols2 = {"Sản phẩm", "Số lượng bán", "Doanh thu (đ)"};
            for (int i = 0; i < cols2.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header2.createCell(i);
                c.setCellValue(cols2[i]);
                c.setCellStyle(headerStyle);
            }
            int rowIdx2 = 1;
            for (var p : topProducts) {
                Row row = sheet2.createRow(rowIdx2++);
                row.createCell(0).setCellValue(p.getProductName());
                row.createCell(1).setCellValue(p.getTotalQty());
                row.createCell(2).setCellValue(p.getTotalRevenue() != null ? p.getTotalRevenue().doubleValue() : 0);
            }
            sheet2.autoSizeColumn(0);
            sheet2.autoSizeColumn(1);
            sheet2.autoSizeColumn(2);

            workbook.write(out);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=doanh-thu-hathub.xlsx");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}