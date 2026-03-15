package com.example.pg.merchantapplication.presentation;

import com.example.pg.merchantapplication.command.application.MerchantApplicationService;
import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.merchantapplication.query.application.MerchantApplicationQueryService;
import com.example.pg.merchantapplication.query.application.dto.PagedApplicationsResultDto;
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
 * 관리자 페이지 (HTML 폼) - 가맹점 심사 관리
 */
@Slf4j
@Controller
@RequestMapping("/admin/merchant-applications")
@RequiredArgsConstructor
public class MerchantApplicationAdminController {

    private final MerchantApplicationQueryService merchantApplicationQueryService;
    private final MerchantApplicationService merchantApplicationService;

    /**
     * 가맹점 신청 목록 (offset 기반 페이지네이션, size=10, 10개 단위 블록)
     * status: 필터, businessNumber: 사업자번호 검색
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessNumber,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request,
            Model model
    ) {
        log.debug("[MerchantApplication] Admin list status={} businessNumber={} page={}", status, businessNumber, page);
        PagedApplicationsResultDto result = merchantApplicationQueryService.findPaged(status, businessNumber, page);
        model.addAttribute("page", result.page());
        model.addAttribute("applications", result.content());
        model.addAttribute("status", status);
        model.addAttribute("businessNumber", businessNumber);
        model.addAttribute("startPage", result.startPage());
        model.addAttribute("endPage", result.endPage());
        model.addAttribute("_csrf", request.getAttribute("_csrf"));
        return "admin/merchant-applications/list";
    }

    /**
     * 가맹점 신청 상세
     */
    @GetMapping("/{applicationId}")
    public String detail(@PathVariable String applicationId, HttpServletRequest request, Model model) {
        log.debug("[MerchantApplication] Admin detail applicationId={}", applicationId);
        MerchantApplication merchantApplication = merchantApplicationQueryService.findById(applicationId);
        model.addAttribute("merchantApplication", merchantApplication);
        model.addAttribute("_csrf", request.getAttribute("_csrf"));
        return "admin/merchant-applications/detail";
    }

    /**
     * 승인 처리 (폼 제출)
     */
    @PostMapping("/{applicationId}/approve")
    public String approve(@PathVariable String applicationId, RedirectAttributes redirectAttributes) {
        log.debug("[MerchantApplication] Admin approve applicationId={}", applicationId);
        merchantApplicationService.approve(applicationId);
        redirectAttributes.addFlashAttribute("message", "승인 완료되었습니다.");
        return "redirect:/admin/merchant-applications/" + applicationId;
    }

    /**
     * 거절 처리 (폼 제출)
     */
    @PostMapping("/{applicationId}/reject")
    public String reject(
            @PathVariable String applicationId,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes
    ) {
        log.debug("[MerchantApplication] Admin reject applicationId={} reasonLength={}", applicationId, reason != null ? reason.length() : 0);
        merchantApplicationService.reject(applicationId, reason);
        redirectAttributes.addFlashAttribute("message", "거절 처리되었습니다.");
        return "redirect:/admin/merchant-applications/" + applicationId;
    }
}
