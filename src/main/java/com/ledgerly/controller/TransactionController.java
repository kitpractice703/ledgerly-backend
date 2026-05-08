package com.ledgerly.controller;

import com.ledgerly.domain.Transaction;
import com.ledgerly.domain.User;
import com.ledgerly.dto.TransactionRequestDto;
import com.ledgerly.dto.TransactionResponseDto;
import com.ledgerly.service.TransactionService;
import com.ledgerly.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * TransactionController - 거래 내역 CRUD REST 컨트롤러
 *
 * 모든 엔드포인트에서 {@code @AuthenticationPrincipal}로 현재 인증된 사용자를 주입받습니다.
 * JWT를 통해 인증된 사용자 이메일이 {@link org.springframework.security.core.userdetails.UserDetails#getUsername()}으로 반환됩니다.
 *
 * 소유권 검증(IDOR 방지)은 서비스 레이어({@link TransactionService})에서 수행합니다.
 * 컨트롤러는 요청 파싱과 응답 포맷에만 집중합니다.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    /**
     * 연도·월·카테고리·타입 필터로 거래 내역 목록을 조회합니다.
     * year 파라미터가 없으면 현재 연도를 기본값으로 사용합니다.
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponseDto>> findAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type) {
        User user = userService.findByEmail(userDetails.getUsername());
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(transactionService.findByUserWithFilters(user, y, month, categoryId, type));
    }

    /** 새로운 거래 내역을 등록합니다. 성공 시 201 Created와 생성된 리소스를 반환합니다. */
    @PostMapping
    public ResponseEntity<?> save(@AuthenticationPrincipal UserDetails userDetails,
                                  @Valid @RequestBody TransactionRequestDto dto,
                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        User user = userService.findByEmail(userDetails.getUsername());
        Transaction transaction = transactionService.save(
                user, dto.getCategoryId(), dto.getAmount(),
                dto.getDescription(), dto.getType(), dto.getTransactionDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new TransactionResponseDto(transaction));
    }

    /** 거래 내역 단건을 조회합니다. 소유권 검증은 서비스 레이어에서 처리합니다. */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> findById(@AuthenticationPrincipal UserDetails userDetails,
                                      @PathVariable Long id) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(transactionService.findByIdDto(id, user));
    }

    /** 거래 내역을 수정합니다. 소유권 불일치 시 서비스에서 예외를 발생시킵니다. */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long id,
                                    @Valid @RequestBody TransactionRequestDto dto,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        User user = userService.findByEmail(userDetails.getUsername());
        transactionService.update(id, user, dto.getCategoryId(), dto.getAmount(),
                dto.getDescription(), dto.getType(), dto.getTransactionDate());

        return ResponseEntity.ok().build();
    }

    /** 거래 내역을 삭제합니다. 성공 시 204 No Content를 반환합니다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long id) {
        User user = userService.findByEmail(userDetails.getUsername());
        transactionService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
