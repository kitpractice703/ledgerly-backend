package com.ledgerly.controller;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.User;
import com.ledgerly.dto.CategoryRequestDto;
import com.ledgerly.service.CategoryService;
import com.ledgerly.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CategoryController - 카테고리 CRUD REST 컨트롤러
 *
 * 모든 카테고리 조회·수정·삭제는 인증된 사용자의 소유 카테고리만 대상으로 합니다.
 * {@link CategoryService}의 {@code findByIdAndUser}가 소유권 검증을 담당합니다.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    /** 현재 사용자의 모든 카테고리를 조회합니다. */
    @GetMapping
    public ResponseEntity<List<Category>> findAll(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(categoryService.findAll(user));
    }

    /** 새 카테고리를 등록합니다. */
    @PostMapping
    public ResponseEntity<?> save(@AuthenticationPrincipal UserDetails userDetails,
                                  @Valid @RequestBody CategoryRequestDto dto,
                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        User user = userService.findByEmail(userDetails.getUsername());
        categoryService.save(user, dto.getName(), dto.getType());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** 카테고리 이름 또는 타입을 수정합니다. */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long id,
                                    @Valid @RequestBody CategoryRequestDto dto,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        User user = userService.findByEmail(userDetails.getUsername());
        categoryService.update(id, user, dto.getName(), dto.getType());
        return ResponseEntity.ok().build();
    }

    /**
     * 카테고리를 삭제합니다.
     *
     * {@link DataIntegrityViolationException}을 별도로 캐치합니다.
     * 해당 카테고리를 참조하는 거래 내역이 존재하면 DB 외래키 제약으로 삭제가 거부되며,
     * 이를 사용자 친화적인 메시지로 변환하여 반환합니다.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long id) {
        User user = userService.findByEmail(userDetails.getUsername());
        try {
            categoryService.delete(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // 거래 내역이 연결된 카테고리는 데이터 정합성을 위해 삭제를 거부합니다.
            return ResponseEntity.badRequest().body("해당 카테고리를 사용하는 거래 내역이 있어 삭제할 수 없습니다.");
        }
    }
}
