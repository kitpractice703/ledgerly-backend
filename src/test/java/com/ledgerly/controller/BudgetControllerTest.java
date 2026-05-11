package com.ledgerly.controller;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.User;
import com.ledgerly.repository.BudgetRepository;
import com.ledgerly.repository.CategoryRepository;
import com.ledgerly.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BudgetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BudgetRepository budgetRepository;

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
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("비인증 요청 시 401 반환")
    void getBudgets_unauthenticated_returns401() throws Exception {
        // given — @BeforeEach로 사용자·카테고리 존재, 인증 없음

        // when
        mockMvc.perform(get("/api/budgets"))
                // then
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 사용자 예산 조회 성공")
    void getBudgets_authenticated_returns200() throws Exception {
        // given — @BeforeEach로 사용자·카테고리 존재

        // when
        mockMvc.perform(get("/api/budgets")
                        .with(user("test@test.com").roles("USER")))
                // then
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("예산 등록 성공 시 201 반환")
    void saveBudget_success_returns201() throws Exception {
        // given — @BeforeEach로 사용자·카테고리 존재
        String body = String.format(
                "{\"categoryId\":%d,\"limitAmount\":300000,\"year\":2026,\"month\":4}",
                testCategory.getId()
        );

        // when
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("test@test.com").roles("USER"))
                        .content(body))
                .andExpect(status().isCreated());

        // then
        assertThat(budgetRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 카테고리 예산 중복 등록 시 400 반환")
    void saveBudget_duplicate_returns400() throws Exception {
        // given — @BeforeEach + 동일 조건 예산 1회 등록 성공
        String body = String.format(
                "{\"categoryId\":%d,\"limitAmount\":300000,\"year\":2026,\"month\":4}",
                testCategory.getId()
        );

        mockMvc.perform(post("/api/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user("test@test.com").roles("USER"))
                .content(body));

        // when — 동일 카테고리·연월 재등록
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("test@test.com").roles("USER"))
                        .content(body))
                .andExpect(status().isBadRequest());

        // then
        assertThat(budgetRepository.count()).isEqualTo(1);
    }
}
