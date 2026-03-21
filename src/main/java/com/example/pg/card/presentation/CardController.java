package com.example.pg.card.presentation;

import com.example.pg.card.presentation.dto.CreateCardRegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.pg.card.command.application.CardService;
import com.example.pg.card.query.application.CardQueryService;
import com.example.pg.card.command.application.result.CardRegistrationResult;

@Controller
@RequestMapping("/card")
@RequiredArgsConstructor
public class CardController {

    private final CardQueryService cardQueryService;
    private final CardService cardService;

    @GetMapping("/register")
    public String showRegisterForm(@RequestParam("token") String token, Model model) {
        cardQueryService.validateToken(token);
        model.addAttribute("token", token);
        return "card/register-form";
    }

    @PostMapping("/register")
    public String handleRegister(@ModelAttribute CreateCardRegistrationRequest request) {
        CardRegistrationResult result = cardService.registerCard(
                request.token(),
                request.cardNumber(),
                request.expiry(),
                request.cvc()
        );
        // 상점 서버로 리다이렉트 (1회용 authCode를 쿼리 파라미터로 전달)
        return "redirect:" + result.returnUrl() + "?authCode=" + result.authCode();
    }
}
