package com.example.pg.card.presentation;

import com.example.pg.card.command.application.CardService;
import com.example.pg.card.presentation.dto.CreateCardRegistrationSessionRequest;
import com.example.pg.card.presentation.dto.CreateCardRegistrationSessionResponse;
import com.example.pg.config.filter.MerchantAuthFilter;
import com.example.pg.card.command.application.result.CreateSessionResult;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/card-registration-sessions")
@RequiredArgsConstructor
public class CardRegistrationTokenController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CreateCardRegistrationSessionResponse> createToken(
            HttpServletRequest httpRequest,
            @RequestBody CreateCardRegistrationSessionRequest request
    ) {
        String merchantId = (String) httpRequest.getAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE);
        CreateSessionResult result = cardService.createToken(merchantId, request.ownerId(), request.returnUrl());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateCardRegistrationSessionResponse(result.token(), result.registrationUrl()));
    }
}
