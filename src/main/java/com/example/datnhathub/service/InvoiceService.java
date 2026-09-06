package com.example.datnhathub.service;

import com.example.datnhathub.entity.OrderDetail;
import com.example.datnhathub.entity.Orders;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {

    // Sinh PDF hóa đơn cho 1 đơn hàng — dùng chung cho cả Admin tải (AdminExportController)
    // và Customer tự tải (OrderController). Áp dụng cho MỌI phương thức thanh toán
    // (COD, Chuyển khoản...), không phụ thuộc vào PaymentMethod hay trạng thái đơn.
    public byte[] generateInvoicePdf(Orders order) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 40);
        PdfWriter.getInstance(document, out);
        document.open();

        com.lowagie.text.Font titleFont  = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 20, com.lowagie.text.Font.BOLD, new Color(192, 99, 58));
        com.lowagie.text.Font headFont   = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, Color.WHITE);
        com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL);
        com.lowagie.text.Font boldFont   = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD);

        Paragraph title = new Paragraph("HÓA ĐƠN BÁN HÀNG", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph brand = new Paragraph("HatHub - hathub.vn", normalFont);
        brand.setAlignment(Element.ALIGN_CENTER);
        brand.setSpacingAfter(20);
        document.add(brand);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        document.add(new Paragraph("Mã đơn hàng: #" + order.getOrderCode(), boldFont));
        if (order.getOrderDate() != null) {
            document.add(new Paragraph("Ngày đặt: " + order.getOrderDate().format(dtf), normalFont));
        }
        if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
            document.add(new Paragraph("Khách hàng: " + order.getCustomer().getUser().getUsername(), normalFont));
            if (order.getCustomer().getUser().getPhone() != null) {
                document.add(new Paragraph("Số điện thoại: " + order.getCustomer().getUser().getPhone(), normalFont));
            }
        }
        if (order.getShipping() != null) {
            document.add(new Paragraph("Địa chỉ giao hàng: " + order.getShipping().getShippingAddress(), normalFont));
        }
        if (order.getPayment() != null) {
            document.add(new Paragraph("Phương thức thanh toán: " + order.getPayment().getPaymentMethod(), normalFont));
        }
        if (order.getStatus() != null) {
            document.add(new Paragraph("Trạng thái đơn hàng: " + order.getStatus(), normalFont));
        }

        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(10);
        document.add(spacer);

        PdfPTable table = new PdfPTable(new float[]{3f, 2f, 1f, 1.5f, 1.5f});
        table.setWidthPercentage(100);

        String[] headers = {"Sản phẩm", "Biến thể", "SL", "Đơn giá", "Thành tiền"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(new Color(17, 17, 17));
            cell.setPadding(6);
            table.addCell(cell);
        }

        DecimalFormat df = new DecimalFormat("#,###");
        BigDecimal shippingFee = (order.getShipping() != null && order.getShipping().getShippingFee() != null)
                ? order.getShipping().getShippingFee() : BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;

        if (order.getDetails() != null) {
            for (OrderDetail d : order.getDetails()) {
                BigDecimal lineTotal = d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()));
                subtotal = subtotal.add(lineTotal);

                table.addCell(new Phrase(d.getProductDetail().getProduct().getProductName(), normalFont));
                table.addCell(new Phrase(d.getProductDetail().getSize() + " / " + d.getProductDetail().getColor(), normalFont));
                table.addCell(new Phrase(String.valueOf(d.getQuantity()), normalFont));
                table.addCell(new Phrase(df.format(d.getUnitPrice()) + " đ", normalFont));
                table.addCell(new Phrase(df.format(lineTotal) + " đ", normalFont));
            }
        }

        document.add(table);

        Paragraph spacer2 = new Paragraph(" ");
        spacer2.setSpacingAfter(10);
        document.add(spacer2);

        Paragraph pSubtotal = new Paragraph("Tạm tính: " + df.format(subtotal) + " đ", normalFont);
        pSubtotal.setAlignment(Element.ALIGN_RIGHT);
        document.add(pSubtotal);

        Paragraph pShip = new Paragraph(
                "Phí vận chuyển: " + (shippingFee.compareTo(BigDecimal.ZERO) == 0 ? "Miễn phí" : df.format(shippingFee) + " đ"),
                normalFont);
        pShip.setAlignment(Element.ALIGN_RIGHT);
        document.add(pShip);

        Paragraph pTotal = new Paragraph(
                "TỔNG CỘNG: " + df.format(order.getTotalAmount()) + " đ",
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 13, com.lowagie.text.Font.BOLD, new Color(192, 99, 58)));
        pTotal.setAlignment(Element.ALIGN_RIGHT);
        pTotal.setSpacingBefore(6);
        document.add(pTotal);

        Paragraph footer = new Paragraph("\nCảm ơn quý khách đã mua hàng tại HatHub!", normalFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }
}
