package com.example.pg.card_company.presentation;

import com.example.pg.card_company.application.BillingKeyWebhookNotifier;
import com.example.pg.card_company.application.CardCompanyService;
import com.example.pg.card_company.application.dto.CardRegisterSession;
import com.example.pg.card_company.domain.vo.BaseUrl;
import com.example.pg.card_company.presentation.dto.RegistrationSessionResponse;
import com.example.pg.card_company.presentation.dto.BillingKeyTokenResponse;
import com.example.pg.card_company.util.dto.CardCompanyListItemDto;
import com.example.pg.common.config.filter.FormDataTokenVerifier;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 카드사 선택 → 등록 세션 생성 → 카드사 페이지 이동 → 콜백 → 빌링키 발급 후 가맹점 returnUrl로 리다이렉트.
 */
@Slf4j
@Controller
@RequestMapping("/card-form")
@RequiredArgsConstructor
public class CardCompanyFormController {
    private final CardCompanyService cardCompanyService;
    private final BillingKeyWebhookNotifier billingKeyWebhookNotifier;

    /**
     * 카드사 선택 페이지. returnUrl은 가맹점이 카드 등록 완료 후 받을 URL(쿼리 파라미터).
     */
    @GetMapping("/register")
    public String showCardCompanySelect(
            @RequestParam("token") String token,
            HttpServletRequest request,
            Model model
    ) {
        String returnUrl = (String) request.getAttribute(FormDataTokenVerifier.ATTR_RETURN_URL);
        String webhookUrl = (String) request.getAttribute(FormDataTokenVerifier.ATTR_WEBHOOK_URL);
        log.debug("[CardCompany] BillingKey register select returnUrl={}", returnUrl != null ? returnUrl : "");
        List<CardCompanyListItemDto> cardCompanies = cardCompanyService.findAllActive().stream()
                .map(CardCompanyListItemDto::from)
                .collect(Collectors.toList());
        model.addAttribute("cardCompanies", cardCompanies);
        model.addAttribute("returnUrl", returnUrl != null ? returnUrl : "");
        model.addAttribute("webhookUrl", webhookUrl != null ? webhookUrl : "");
        model.addAttribute("token", token);
        if (request.getAttribute("_csrf") != null) {
            model.addAttribute("_csrf", request.getAttribute("_csrf"));
        }
        return "card_company/card-company-select";
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
        log.debug("[CardCompany] BillingKey register start cardCompanyCode={}", cardCompanyCode);
        String returnUrl = (String) request.getAttribute(FormDataTokenVerifier.ATTR_RETURN_URL);
        String webhookUrl = (String) request.getAttribute(FormDataTokenVerifier.ATTR_WEBHOOK_URL);
        String merchantId = (String) request.getAttribute(FormDataTokenVerifier.ATTR_MERCHANT_ID);

        String pgCallbackUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/card-form/callback/register")
                .build()
                .toUriString();

        String sessionToken = cardCompanyService.saveCardRegisterSession(new CardRegisterSession(cardCompanyCode, returnUrl, webhookUrl, merchantId));
        String pgCallbackWithToken = pgCallbackUrl + "?token=" + sessionToken;

        // 카드사와 통신
        // 결과로 HTML 페이지를 만드는 것 x, 브라우저에게 페이지를 리다이렉트하기 전 작업
        RegistrationSessionResponse response = cardCompanyService.createRegistrationSession(cardCompanyCode, pgCallbackWithToken);

        BaseUrl cardCompanyBaseUrl = cardCompanyService.findByCode(cardCompanyCode).getBaseUrl();
        String redirectUrl = cardCompanyBaseUrl.normalize() + response.registrationUrl();
        return "redirect:" + redirectUrl;
    }

    /**
     * 카드사에서 카드 등록 완료 후 돌아오는 콜백. authCode로 빌링키 발급 후 returnUrl로 리다이렉트.
     */
    @GetMapping("/callback/register")
    public String callback(
            @RequestParam("token") String sessionToken,
            @RequestParam(value = "authCode", required = false) String authCode,
            HttpServletRequest request
    ) {
        log.debug("[CardCompany] BillingKey callback token={}", sessionToken != null ? "present" : "null");
        CardRegisterSession session = cardCompanyService.getCardRegisterSession(sessionToken);

        BillingKeyTokenResponse response = cardCompanyService.issueBillingKey(session.cardCompanyCode(), authCode);
        cardCompanyService.removeCardRegisterSession(sessionToken);

        String returnUrl = session.returnUrl();
        String webhookUrl = session.webhookUrl();

        // 웹훅으로 빌링키를 넘기는 설계: app.billing-key.webhook.enabled=true 일 때만 POST
        billingKeyWebhookNotifier.notifyBillingKeyRegisteredIfEnabled(
                session.merchantId(),
                webhookUrl,
                session.cardCompanyCode(),
                response
        );

        // billingKeyToken은 서버-서버 웹훅으로 전달한다. (브라우저에는 토큰/교환 code를 내려주지 않는다)
        return "redirect:" + returnUrl;
    }
}
