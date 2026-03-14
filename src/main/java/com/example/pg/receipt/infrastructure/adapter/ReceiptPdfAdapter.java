package com.example.pg.receipt.infrastructure.adapter;

import com.example.pg.receipt.command.application.port.ReceiptPdfPort;
import com.example.pg.receipt.domain.aggregate.Receipt;
import com.example.pg.receipt.domain.enumerate.ReceiptStatus;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * OpenHTMLtoPDF 기반 영수증 PDF 생성 어댑터.
 */
@Slf4j
@Component
public class ReceiptPdfAdapter implements ReceiptPdfPort {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.KOREA);

    @Override
    public byte[] generate(Receipt receipt) {
        String html = buildReceiptHtml(receipt);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("[ReceiptPdf] PDF 생성 실패 receiptId={}", receipt.getId(), e);
            throw new RuntimeException("영수증 PDF 생성에 실패했습니다.", e);
        }
    }

    private String buildReceiptHtml(Receipt receipt) {
        String title = receipt.isVoided() ? "영수증 (취소됨)" : "영수증";
        String statusText = receipt.getStatus() == ReceiptStatus.VOIDED ? "취소됨" : "발급완료";
        String approvedAt = receipt.getApprovedAt() != null
                ? receipt.getApprovedAt().format(DATE_TIME_FORMAT)
                : "";
        String amountFormatted = String.format(Locale.KOREA, "%,d", receipt.getAmount());

        return """
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta charset="UTF-8"/>
                <style>
                    body { font-family: Malgun Gothic, sans-serif; font-size: 11pt; padding: 20px; }
                    h1 { font-size: 16pt; text-align: center; margin-bottom: 20px; }
                    .voided { color: #c00; font-weight: bold; }
                    table { width: 100%; border-collapse: collapse; margin-top: 16px; }
                    th, td { border: 1px solid #333; padding: 8px; text-align: left; }
                    th { width: 120px; background: #f5f5f5; }
                    .footer { margin-top: 24px; text-align: center; font-size: 9pt; color: #666; }
                </style>
            </head>
            <body>
                <h1>%s</h1>
                <p class="%s">상태: %s</p>
                <table>
                    <tr><th>영수증 번호</th><td>%s</td></tr>
                    <tr><th>가맹점</th><td>%s</td></tr>
                    <tr><th>주문명</th><td>%s</td></tr>
                    <tr><th>가맹점 주문번호</th><td>%s</td></tr>
                    <tr><th>결제자</th><td>%s</td></tr>
                    <tr><th>결제 금액</th><td>%s 원</td></tr>
                    <tr><th>승인 번호</th><td>%s</td></tr>
                    <tr><th>거래 ID</th><td>%s</td></tr>
                    <tr><th>승인 일시</th><td>%s</td></tr>
                </table>
                <p class="footer">결제 ID: %s</p>
            </body>
            </html>
            """.formatted(
                escapeXml(title),
                receipt.isVoided() ? "voided" : "",
                escapeXml(statusText),
                escapeXml(receipt.getReceiptNumber()),
                escapeXml(receipt.getMerchantName()),
                escapeXml(receipt.getOrderName()),
                escapeXml(receipt.getMerchantOrderId()),
                escapeXml(receipt.getCustomerName()),
                amountFormatted,
                escapeXml(receipt.getApprovalNumber()),
                escapeXml(receipt.getTransactionId()),
                escapeXml(approvedAt),
                escapeXml(receipt.getPaymentId())
            );
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
