package com.ledgerly.controller;

import com.ledgerly.domain.User;
import com.ledgerly.dto.BudgetRequestDto;
import com.ledgerly.dto.BudgetStatusDto;
import com.ledgerly.service.BudgetService;
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
 * BudgetController - 월별 예산 CRUD REST 컨트롤러
 *
 * <p>[설계] 예산 조회는 실제 지출액과 초과 여부를 포함한 {@link BudgetStatusDto}로 반환합니다.
 * 단순 엔티티가 아닌 비즈니스 가공 데이터를 응답하여 프론트엔드의 추가 계산을 줄입니다.</p>
 *
 * <p>[설계] year/month 기본값을 0으로 받아 0이면 현재 날짜로 대체합니다.
 * defaultValue를 현재 날짜 문자열로 지정하면 서버 시작 시점이 고정되므로 이 방식을 사용합니다.</p>
 */
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final UserService userService;

    /**
     * 특정 연월의 예산 현황(한도·지출·잔여·초과 여부)을 조회합니다.
     * year/month 파라미터가 0이면 현재 연월을 기본값으로 사용합니다.
     */
    @GetMapping
    public ResponseEntity<List<BudgetStatusDto>> findAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {

        if (year == 0) year = LocalDate.now().getYear();
        if (month == 0) month = LocalDate.now().getMonthValue();

        User user = userService.findByEmail(userDetails.getUsername());
        List<BudgetStatusDto> budgetStatuses = budgetService.findBudgetStatusByUserAndMonth(user, year, month);

        return ResponseEntity.ok(budgetStatuses);
    }

    /**
     * 카테고리·연월의 예산 한도를 등록합니다.
     * 동일 카테고리·연월 중복 등록 시 400 Bad Request를 반환합니다.
     */
    @PostMapping
    public ResponseEntity<?> save(@AuthenticationPrincipal UserDetails userDetails,
                                  @Valid @RequestBody BudgetRequestDto dto,
                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        User user = userService.findByEmail(userDetails.getUsername());

        try {
            budgetService.save(user, dto.getCategoryId(), dto.getLimitAmount(), dto.getYear(), dto.getMonth());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 예산 한도 금액을 수정합니다.
     * 한도 금액만 변경하므로 body 대신 query parameter로 전달받습니다.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long id,
                                    @RequestParam Integer limitAmount) {
        User user = userService.findByEmail(userDetails.getUsername());

        try {
            budgetService.update(id, user, limitAmount);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 예산을 삭제합니다. 성공 시 204 No Content를 반환합니다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long id) {
        User user = userService.findByEmail(userDetails.getUsername());
        budgetService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
