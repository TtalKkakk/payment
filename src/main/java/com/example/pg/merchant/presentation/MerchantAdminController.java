package com.example.pg.merchant.presentation;

import com.example.pg.merchant.application.MerchantService;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.application.dto.PagedMerchantsResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 페이지 (MVC) - 가맹점 목록/상세 조회, API 정지.
 * /admin/** 접근 시 로그인 필요 (SecurityConfig).
 */
@Slf4j
@Controller
@RequestMapping("/admin/merchants")
@RequiredArgsConstructor
public class MerchantAdminController {
    private final MerchantService merchantService;

    /**
     * 가맹점 목록 (페이지네이션 10개씩, id로 검색 가능)
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) String id,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request,
            Model model
    ) {
        log.debug("[Merchant] Admin list id={} page={}", id, page);
        PagedMerchantsResultDto result = merchantService.findPaged(id, page);
        model.addAttribute("merchants", result.content());
        model.addAttribute("page", result.page());
        model.addAttribute("startPage", result.startPage());
        model.addAttribute("endPage", result.endPage());
        model.addAttribute("id", id);
        model.addAttribute("_csrf", request.getAttribute("_csrf"));
        return "admin/merchants/list";
    }

    /**
     * 가맹점 상세 (apiSecret 포함)
     */
    @GetMapping("/{merchantId}")
    public String detail(@PathVariable String merchantId, HttpServletRequest request, Model model) {
        log.debug("[Merchant] Admin detail merchantId={}", merchantId);
        Merchant merchant = merchantService.findById(merchantId);
        model.addAttribute("merchant", merchant);
        model.addAttribute("_csrf", request.getAttribute("_csrf"));
        return "admin/merchants/detail";
    }

    /**
     * API 정지 (ACTIVE → SUSPENDED). 처리 후 목록으로 리다이렉트.
     */
    @PostMapping("/{merchantId}/suspend")
    public String suspend(
            @PathVariable String merchantId,
            @RequestParam(required = false) String id,
            @RequestParam(defaultValue = "0") int page,
            RedirectAttributes redirectAttributes
    ) {
        log.info("[Merchant] Admin suspend merchantId={}", merchantId);
        merchantService.suspendMerchant(merchantId);
        redirectAttributes.addFlashAttribute("message", "API를 정지했습니다.");
        redirectAttributes.addAttribute("id", id);
        redirectAttributes.addAttribute("page", page);
        return "redirect:/admin/merchants";
    }

    /**
     * 정지 해제 (SUSPENDED → ACTIVE). 처리 후 목록으로 리다이렉트.
     */
    @PostMapping("/{merchantId}/activate")
    public String activate(
            @PathVariable String merchantId,
            @RequestParam(required = false) String id,
            @RequestParam(defaultValue = "0") int page,
            RedirectAttributes redirectAttributes
    ) {
        log.info("[Merchant] Admin activate merchantId={}", merchantId);
        merchantService.activateMerchant(merchantId);
        redirectAttributes.addFlashAttribute("message", "정지를 해제했습니다.");
        redirectAttributes.addAttribute("id", id);
        redirectAttributes.addAttribute("page", page);
        return "redirect:/admin/merchants";
    }

    /**
     * 탈퇴 처리. 정지(SUSPENDED) 상태인 가맹점만 WITHDRAWN으로 전환 가능.
     */
    @PostMapping("/{merchantId}/withdraw")
    public String withdraw(
            @PathVariable String merchantId,
            @RequestParam(required = false) String id,
            @RequestParam(defaultValue = "0") int page,
            RedirectAttributes redirectAttributes
    ) {
        log.info("[Merchant] Admin withdraw merchantId={}", merchantId);
        merchantService.withdrawMerchantFromSuspended(merchantId);
        redirectAttributes.addFlashAttribute("message", "탈퇴 처리했습니다.");
        redirectAttributes.addAttribute("id", id);
        redirectAttributes.addAttribute("page", page);
        return "redirect:/admin/merchants";
    }
}
