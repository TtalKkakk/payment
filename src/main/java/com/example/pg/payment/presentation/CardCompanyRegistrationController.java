package com.example.pg.payment.presentation;

import com.example.pg.payment.command.application.BillingKeyService;
import com.example.pg.payment.presentation.dto.RegistrationSessionResponse;
import com.example.pg.payment.infrastructure.persistence.CardRegisterSessionStore;
import com.example.pg.payment.query.application.BillingKeyQueryService;
import com.example.pg.payment.query.application.dto.CardCompanyListItemDto;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.payment.command.application.BillingKeyExchangeService;
import com.example.pg.payment.presentation.security.BillingKeyRegisterTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 카드사 선택 → 등록 세션 생성 → 카드사 페이지 이동 → 콜백 → 빌링키 발급 후 가맹점 returnUrl로 리다이렉트.
 */
@Slf4j
@Controller
@RequestMapping("/card-form")
@RequiredArgsConstructor
public class CardCompanyRegistrationController {

    private final BillingKeyQueryService billingKeyQueryService;
    private final BillingKeyService billingKeyService;
    private final BillingKeyExchangeService billingKeyExchangeService;
    private final CardRegisterSessionStore sessionStore;

    /**
     * 카드사 선택 페이지. returnUrl은 가맹점이 카드 등록 완료 후 받을 URL(쿼리 파라미터).
     */
    @GetMapping("/register")
    public String showCardCompanySelect(
            @RequestParam("token") String token,
            HttpServletRequest request,
            Model model
    ) {
        String returnUrl = (String) request.getAttribute(BillingKeyRegisterTokenVerifier.ATTR_RETURN_URL);
        log.debug("[Payment] BillingKey register select returnUrl={}", returnUrl != null ? returnUrl : "");
        List<CardCompanyListItemDto> cardCompanies = billingKeyQueryService.findAllActive().stream()
                .map(CardCompanyListItemDto::from)
                .collect(Collectors.toList());
        model.addAttribute("cardCompanies", cardCompanies);
        model.addAttribute("returnUrl", returnUrl != null ? returnUrl : "");
        model.addAttribute("token", token);
        if (request.getAttribute("_csrf") != null) {
            model.addAttribute("_csrf", request.getAttribute("_csrf"));
        }
        return "cardcompany/card-company-select";
    }

    /**
     * 카드사 선택 시: 등록 세션 생성(/card-registration-session) 후 해당 카드사 등록 페이지로 리다이렉트.
     */
    @PostMapping("/register/start")
    public String startRegistration(
            @RequestParam("cardCompanyCode") String cardCompanyCode,
            @RequestParam("token") String token,
            HttpServletRequest request
    ) {
        log.debug("[Payment] BillingKey register start cardCompanyCode={}", cardCompanyCode);
        String returnUrl = (String) request.getAttribute(BillingKeyRegisterTokenVerifier.ATTR_RETURN_URL);
        String pgCallbackUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/card-form/callback/register")
                .build()
                .toUriString();

        String sessionToken = sessionStore.put(cardCompanyCode, returnUrl != null ? returnUrl : "");
        String pgCallbackWithToken = pgCallbackUrl + "?token=" + sessionToken;

        RegistrationSessionResponse result = billingKeyService.createRegistrationSession(cardCompanyCode, pgCallbackWithToken);

        String cardCompanyBaseUrl = billingKeyQueryService.findByCode(cardCompanyCode).getBaseUrl();
        if (cardCompanyBaseUrl == null || cardCompanyBaseUrl.isBlank()) {
            return "redirect:/card-form/register?token="
                    + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + "&error=no-base-url";
        }
        String redirectUrl = cardCompanyBaseUrl.replaceFirst("/$", "") + result.registrationUrl();
        return "redirect:" + redirectUrl;
    }

    /**
     * 카드사에서 카드 등록 완료 후 돌아오는 콜백. authCode로 빌링키 발급 후 returnUrl로 리다이렉트.
     */
    @GetMapping("/callback/register")
    public String callback(
            @RequestParam("token") String token,
            @RequestParam(value = "authCode", required = false) String authCode,
            HttpServletRequest request
    ) {
        log.debug("[Payment] BillingKey callback token={}", token != null ? "present" : "null");
        if (authCode == null || authCode.isBlank()) {
            throw new BusinessException(ErrorCode.CARD_REGISTER_AUTH_CODE_REQUIRED);
        }
        var session = sessionStore.get(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_REGISTER_SESSION_INVALID, token));

        var billingKeyResult = billingKeyService.issueBillingKey(session.cardCompanyCode(), authCode);
        sessionStore.remove(token);

        String returnUrl = session.returnUrl();
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = "/";
        }

        if (!returnUrl.contains("?") && returnUrl.endsWith("/")) {
            returnUrl = returnUrl.substring(0, returnUrl.length() - 1);
        }

        // billingKeyToken을 브라우저에 노출하지 않기 위해 1회용 code만 전달한다.
        // 가맹점 서버는 /api/billing-keys/exchange로 code를 보내 billingKeyToken을 교환한다.
        String merchantId = (String) request.getAttribute(BillingKeyRegisterTokenVerifier.ATTR_MERCHANT_ID);
        if (merchantId == null || merchantId.isBlank()) {
            throw new BusinessException(ErrorCode.BILLING_KEY_REGISTER_TOKEN_INVALID);
        }
        String code = billingKeyExchangeService.issueCode(
                merchantId,
                billingKeyResult.billingKeyToken(),
                session.cardCompanyCode(),
                billingKeyResult.cardBrand(),
                billingKeyResult.cardNumberMasked(),
                billingKeyResult.expiryMasked()
        );

        String separator = returnUrl.contains("?") ? "&" : "?";
        String redirect = returnUrl + separator
                + "code=" + code;
        return "redirect:" + redirect;
    }
}
