package com.example.pg.receipt.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchant.presentation.port.MerchantPort;
import com.example.pg.payment.presentation.port.PaymentPort;
import com.example.pg.payment.application.adapter.dto.PaymentSnapshotForReceiptDto;
import com.example.pg.receipt.domain.aggregate.Receipt;
import com.example.pg.receipt.domain.enumerate.ReceiptStatus;
import com.example.pg.receipt.domain.vo.ReceiptId;
import com.example.pg.receipt.infrastructure.persistence.ReceiptRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * 영수증 발급 서비스.
 * 결제 승인 완료(AUTHORIZED)인 결제에 대해 영수증을 생성한다.
 * 이미 해당 결제로 영수증이 있으면 기존 영수증을 반환한다(멱등).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.KOREA);
    private static final String KOREAN_FONT_FAMILY = "Noto Sans KR";
    private static final String KOREAN_FONT_PATH = "/fonts/NotoSansKR-Regular.ttf";
    private final PaymentPort paymentPort;
    private final MerchantPort merchantPort;
    private final ReceiptRepository receiptRepository;

    /**
     * 결제 ID에 대해 영수증 발급. 승인 완료된 결제만 가능하며, 이미 있으면 기존 영수증 반환(멱등).
     */
    public Receipt issueForPayment(String paymentId) {
        log.debug("[Receipt] issueForPayment start paymentId={}", paymentId);
        if (!paymentPort.existsPayment(paymentId)) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentId);
        }
        Optional<PaymentSnapshotForReceiptDto> snapshotOpt = paymentPort.findAuthorizedPayment(paymentId);
        if (snapshotOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.RECEIPT_CANNOT_ISSUE);
        }

        Optional<Receipt> existing = receiptRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            log.debug("[Receipt] issueForPayment idempotent-return paymentId={}", paymentId);
            return existing.get();
        }

        PaymentSnapshotForReceiptDto s = snapshotOpt.get();
        String merchantName = merchantPort.getMerchantName(s.merchantId());
        Receipt receipt = new Receipt(
                ReceiptId.generate(),
                s.paymentId(),
                s.merchantId(),
                merchantName,
                s.amount(),
                s.orderName(),
                s.merchantOrderId(),
                s.customerName(),
                s.approvalNumber(),
                s.transactionId(),
                s.approvedAt()
        );
        Receipt saved = receiptRepository.save(receipt);
        log.info("[Receipt] event=Issued paymentId={} receiptId={}", paymentId, saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Receipt> getByPaymentId(String paymentId) {
        log.debug("[Receipt] Query getByPaymentId paymentId={}", paymentId);
        return receiptRepository.findByPaymentId(paymentId);
    }

    public byte[] generate(Receipt receipt) {
        String html = buildReceiptHtml(receipt);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerKoreanFont(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("[ReceiptPdf] PDF 생성 실패 receiptId={}", receipt.getId(), e);
            throw new RuntimeException("영수증 PDF 생성에 실패했습니다.", e);
        }
    }

    /**
     * 한글 표시를 위해 Noto Sans KR 폰트 등록.
     * resources/fonts/NotoSansKR-Regular.ttf 가 없으면 등록을 건너뛰고, 한글이 깨질 수 있음.
     */
    private void registerKoreanFont(PdfRendererBuilder builder) {
        try (InputStream fontStream = getClass().getResourceAsStream(KOREAN_FONT_PATH)) {
            if (fontStream != null) {
                builder.useFont(() -> getClass().getResourceAsStream(KOREAN_FONT_PATH), KOREAN_FONT_FAMILY);
            } else {
                log.debug("[ReceiptPdf] 한글 폰트 없음 ({}), resources/fonts/README.md 참고", KOREAN_FONT_PATH);
            }
        } catch (IOException e) {
            log.warn("[ReceiptPdf] 한글 폰트 로드 실패: {}", e.getMessage());
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
                    body { font-family: "Noto Sans KR", "Malgun Gothic", sans-serif; font-size: 11pt; padding: 20px; }
                    h1 { font-size: 16pt; text-align: center; margin-bottom: 20px; }
                    .voided { color: #c00; font-weight: bold; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 16px; }
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

    /**
     * XML/HTML 특수문자 이스케이프.
     * {@code String.formatted} 인자 값 안의 {@code %}는 포맷 지시자로 해석되지 않으므로 이스케이프하지 않는다.
     */
    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
