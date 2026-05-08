package com.ledgerly.controller;

import com.ledgerly.domain.User;
import com.ledgerly.dto.AnnualSummaryDto;
import com.ledgerly.dto.CategoryBreakdownDto;
import com.ledgerly.dto.MonthlyTrendDto;
import com.ledgerly.service.ReportService;
import com.ledgerly.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * ReportController - 재정 통계 리포트 데이터를 제공하는 컨트롤러
 *
 * 리포트 데이터를 용도별로 세 개의 엔드포인트로 분리합니다.
 * 월별 트렌드와 연간 요약은 연도 단위로, 카테고리 분석은 연도·월·타입으로 조회합니다.
 * 엔드포인트를 분리함으로써 프론트엔드에서 필요한 데이터만 선택적으로 요청할 수 있습니다.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    /** 특정 연도의 월별 수입·지출 트렌드를 12개월 전체로 반환합니다. */
    @GetMapping("/monthly-trend")
    public ResponseEntity<List<MonthlyTrendDto>> getMonthlyTrend(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        User user = userService.findByEmail(userDetails.getUsername());
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(reportService.getMonthlyTrend(user, y));
    }

    /**
     * 특정 연월의 카테고리별 수입 또는 지출 분포를 반환합니다.
     * type 파라미터 기본값은 "EXPENSE"입니다.
     */
    @GetMapping("/category-breakdown")
    public ResponseEntity<List<CategoryBreakdownDto>> getCategoryBreakdown(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "EXPENSE") String type) {
        User user = userService.findByEmail(userDetails.getUsername());
        int y = year != null ? year : LocalDate.now().getYear();
        int m = month != null ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(reportService.getCategoryBreakdown(user, y, m, type));
    }

    /** 특정 연도의 총 수입·지출·순이익·저축률을 반환합니다. */
    @GetMapping("/annual-summary")
    public ResponseEntity<AnnualSummaryDto> getAnnualSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        User user = userService.findByEmail(userDetails.getUsername());
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(reportService.getAnnualSummary(user, y));
    }
}
