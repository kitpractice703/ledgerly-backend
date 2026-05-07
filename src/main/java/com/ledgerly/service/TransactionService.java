package com.ledgerly.service;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.Transaction;
import com.ledgerly.domain.User;
import com.ledgerly.dto.TransactionResponseDto;
import com.ledgerly.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TransactionService - 거래 내역 CRUD 및 조회 비즈니스 로직을 담당하는 서비스
 *
 * <p>[보안] 모든 수정·삭제 메서드에서 요청 사용자와 리소스 소유자의 일치 여부를 검증하여
 * IDOR(Insecure Direct Object Reference) 취약점을 방지합니다.
 * URL에 타인의 거래 ID를 직접 입력해도 데이터에 접근하거나 변경할 수 없습니다.</p>
 *
 * <p>[설계] 조회 전용 메서드에는 {@code @Transactional(readOnly = true)}를 적용하여
 * JPA의 더티 체킹(변경 감지)을 비활성화하고 DB 읽기 성능을 향상시킵니다.</p>
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    /**
     * 새로운 거래 내역을 저장합니다.
     *
     * <p>[보안] 카테고리 조회 시 {@link CategoryService#findById}를 통해
     * 해당 카테고리가 현재 사용자의 소유임을 검증합니다.
     * 타인의 카테고리 ID를 사용한 거래 생성이 불가합니다.</p>
     */
    @Transactional
    public Transaction save(
            User user,
            Long categoryId,
            Integer amount,
            String description,
            String type,
            LocalDate date
    ) {
        Category category = categoryService.findById(categoryId, user);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setTransactionDate(date);

        return transactionRepository.save(transaction);
    }

    /**
     * 특정 연월의 거래 내역 목록을 날짜 내림차순으로 조회합니다.
     *
     * <p>[설계] 월의 첫날과 마지막날을 {@link LocalDate#withDayOfMonth}로 동적 계산하여
     * 윤달(2월 29일 등) 처리를 Java 표준 라이브러리에 위임합니다.</p>
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> findDtosByUserAndMonth(User user, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(user, startDate, endDate)
                .stream()
                .map(TransactionResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 거래 내역 단건을 조회하고 소유권을 검증합니다.
     *
     * <p>[보안] ID로 거래를 찾은 후 요청 사용자의 ID와 거래 소유자의 ID를 비교합니다.
     * 불일치 시 예외를 발생시켜 타인의 거래 내역 접근을 차단합니다.</p>
     *
     * @throws IllegalArgumentException 거래가 존재하지 않거나 접근 권한이 없는 경우
     */
    @Transactional(readOnly = true)
    public Transaction findById(Long transactionId, User user) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래 내역입니다."));

        // [보안] IDOR 방지: 존재하는 거래라도 소유자가 다르면 접근 불가
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return transaction;
    }

    /** 단건 거래 내역을 DTO로 변환하여 반환합니다. 소유권 검증은 {@link #findById}에 위임합니다. */
    @Transactional(readOnly = true)
    public TransactionResponseDto findByIdDto(Long transactionId, User user) {
        return new TransactionResponseDto(findById(transactionId, user));
    }

    /**
     * 거래 내역을 수정합니다.
     *
     * <p>[보안] 수정 전 소유권을 검증하여 타인의 거래 내역 수정 시도를 차단합니다.</p>
     * <p>[설계] JPA의 더티 체킹을 활용합니다. 트랜잭션 내에서 엔티티 필드를 변경하면
     * 트랜잭션 커밋 시 별도의 {@code save()} 호출 없이 UPDATE 쿼리가 자동 실행됩니다.</p>
     */
    @Transactional
    public void update(Long transactionId, User user, Long categoryId,
                       Integer amount, String description, String type, LocalDate transactionDate) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래 내역입니다."));

        // [보안] IDOR 방지: 타인의 거래 내역 수정 시도 차단
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        Category category = categoryService.findById(categoryId, user);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setTransactionDate(transactionDate);
    }

    /**
     * 거래 내역을 삭제합니다.
     *
     * <p>[보안] 삭제 전 소유권을 검증하여 타인의 거래 내역 삭제 시도를 차단합니다.</p>
     */
    @Transactional
    public void delete(Long transactionId, User user) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래 내역입니다."));

        // [보안] IDOR 방지: 타인의 거래 내역 삭제 시도 차단
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        transactionRepository.delete(transaction);
    }

    /**
     * 연도·월·카테고리·타입 필터를 적용하여 거래 내역 목록을 조회합니다.
     *
     * <p>[설계] 필터링은 DB 쿼리가 아닌 Java 스트림으로 처리합니다. 카테고리·타입 필터
     * 조합이 동적으로 변하므로 JPA Specification 또는 QueryDSL 없이 스트림으로
     * 간결하게 구현했습니다. 데이터 규모가 커지면 DB 레벨 필터링으로 전환을 고려할 수 있습니다.</p>
     *
     * @param month null이면 연간 전체 조회, 값이 있으면 해당 월만 조회
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> findByUserWithFilters(User user, int year, Integer month, Long categoryId, String type) {
        LocalDate startDate, endDate;
        if (month != null) {
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        } else {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }

        return transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(user, startDate, endDate)
                .stream()
                .filter(t -> categoryId == null || t.getCategory().getId().equals(categoryId))
                .filter(t -> type == null || type.isBlank() || t.getType().equals(type))
                .map(TransactionResponseDto::new)
                .toList();
    }

    /**
     * 특정 카테고리·타입·연월의 거래 금액 합계를 반환합니다. 예산 소진율 계산에 사용됩니다.
     *
     * @return 합계 금액 (거래 내역이 없으면 0)
     */
    @Transactional(readOnly = true)
    public int sumByUserAndCategoryAndMonth(
            User user,
            Long categoryId,
            String type,
            int year,
            int month
    ) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        Integer sum = transactionRepository.sumAmountByUserAndCategoryAndTypeDateBetween(
                user, categoryId, type, startDate, endDate
        );

        // [설계] JPA @Query의 SUM 집계는 대상 행이 없을 때 null을 반환하므로 0으로 치환합니다.
        return sum != null ? sum : 0;
    }
}
