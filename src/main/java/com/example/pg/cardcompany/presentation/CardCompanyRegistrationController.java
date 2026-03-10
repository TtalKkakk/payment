package com.example.pg.cardcompany.presentation;

import com.example.pg.cardcompany.application.CardCompanyBillingKeyService;
import com.example.pg.cardcompany.application.CardRegisterSessionStore;
import com.example.pg.cardcompany.domain.result.RegistrationSessionResult;
import com.example.pg.cardcompany.query.application.CardCompanyQueryService;
import com.example.pg.cardcompany.query.application.dto.CardCompanyListItem;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
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
@Controller
@RequestMapping("/card")
@RequiredArgsConstructor
public class CardCompanyRegistrationController {

    private final CardCompanyQueryService cardCompanyQueryService;
    private final CardCompanyBillingKeyService cardCompanyBillingKeyService;
    private final CardRegisterSessionStore sessionStore;

    /**
     * 카드사 선택 페이지. returnUrl은 가맹점이 카드 등록 완료 후 받을 URL(쿼리 파라미터).
     */
    @GetMapping("/register")
    public String showCardCompanySelect(
            @RequestParam(value = "returnUrl", required = false) String returnUrl,
            HttpServletRequest request,
            Model model
    ) {
        List<CardCompanyListItem> cardCompanies = cardCompanyQueryService.findAllActive().stream()
                .map(CardCompanyListItem::from)
                .collect(Collectors.toList());
        model.addAttribute("cardCompanies", cardCompanies);
        model.addAttribute("returnUrl", returnUrl != null ? returnUrl : "");
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
            @RequestParam(value = "returnUrl", required = false) String returnUrl
    ) {
        String pgCallbackUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/card/callback/register")
                .build()
                .toUriString();

        String token = sessionStore.put(cardCompanyCode, returnUrl != null ? returnUrl : "");
        String pgCallbackWithToken = pgCallbackUrl + "?token=" + token;

        RegistrationSessionResult result = cardCompanyBillingKeyService.createRegistrationSession(cardCompanyCode, pgCallbackWithToken);

        String cardCompanyBaseUrl = cardCompanyQueryService.findByCode(cardCompanyCode).getBaseUrl();
        if (cardCompanyBaseUrl == null || cardCompanyBaseUrl.isBlank()) {
            return "redirect:/card/register?error=no-base-url";
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
            @RequestParam(value = "authCode", required = false) String authCode
    ) {
        if (authCode == null || authCode.isBlank()) {
            throw new BusinessException(ErrorCode.CARD_REGISTER_AUTH_CODE_REQUIRED);
        }
        var session = sessionStore.get(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_REGISTER_SESSION_INVALID, token));

        var billingKeyResult = cardCompanyBillingKeyService.issueBillingKey(session.cardCompanyCode(), authCode);
        sessionStore.remove(token);

        String returnUrl = session.returnUrl();
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = "/";
        }
        String separator = returnUrl.contains("?") ? "&" : "?";
        return "redirect:" + returnUrl + separator + "billingKeyToken=" + billingKeyResult.billingKeyToken();
    }
}
