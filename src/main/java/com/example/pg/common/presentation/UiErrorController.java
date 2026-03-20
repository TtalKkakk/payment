package com.example.pg.common.presentation;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 필터 단계에서 발생한 사용자용(브라우저) 에러를 공통 에러 템플릿으로 렌더링하기 위한 컨트롤러.
 *
 * 필터에서 request attribute로 statusCode/code/message를 넣고 forward 한다.
 */
@Slf4j
@Controller
@RequestMapping
public class UiErrorController {

    @GetMapping("/ui-error")
    public String uiError(HttpServletRequest request, Model model) {
        Object statusCode = request.getAttribute("statusCode");
        Object code = request.getAttribute("code");
        Object message = request.getAttribute("message");

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("code", code);
        model.addAttribute("message", message);

        return "error/user-error";
    }
}

