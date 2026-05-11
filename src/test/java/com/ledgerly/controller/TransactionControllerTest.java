package com.ledgerly.controller;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.Transaction;
import com.ledgerly.domain.User;
import com.ledgerly.repository.CategoryRepository;
import com.ledgerly.repository.TransactionRepository;
import com.ledgerly.repository.UserRepository;
import com.ledgerly.service.TransactionService;
import com.ledgerly.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private TransactionService transactionService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        User user = userService.register("test@test.com", "password123", "김인태");

        testCategory = new Category();
        testCategory.setUser(user);
        testCategory.setName("식비");
        testCategory.setType("EXPENSE");
        categoryRepository.save(testCategory);
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("비인증 요청 시 401 반환")
    void getTransactions_unauthenticated_returns401() throws Exception {
        // given — @BeforeEach로 사용자, 카테고리 존재, JWT·세션 없음

        // when
        mockMvc.perform(get("/api/transactions"))
                // then
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 사용자 거래 내역 조회 성공")
    void getTransactions_authenticated_returns200() throws Exception {
        // given — @BeforeEach로 사용자, 카테고리 존재

        // when
        mockMvc.perform(get("/api/transactions")
                        .with(user("test@test.com").roles("USER")))
                // then
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("거래 내역 등록 성공 시 201 반환")
    void saveTransaction_success() throws Exception {
        // given — @BeforeEach로 사용자, 카테고리 존재
        String body = String.format(
                "{\"categoryId\":%d,\"amount\":15000,\"description\":\"점심식사\",\"type\":\"EXPENSE\",\"transactionDate\":\"2026-04-11\"}",
                testCategory.getId()
        );

        // when
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("test@test.com").roles("USER"))
                        .content(body))
                .andExpect(status().isCreated());

        // then
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("거래 내역 삭제 성공 시 204 반환")
    void deleteTransaction_success() throws Exception {
        // given — 삭제 대상 거래 1건 저장
        User user = userService.findByEmail("test@test.com");
        Transaction tx = transactionService.save(user, testCategory.getId(), 15000, "점심식사", "EXPENSE", LocalDate.of(2026, 4, 11));

        // when
        mockMvc.perform(delete("/api/transactions/" + tx.getId())
                        .with(user("test@test.com").roles("USER")))
                .andExpect(status().isNoContent());

        // then
        assertThat(transactionRepository.count()).isEqualTo(0);
    }
}
