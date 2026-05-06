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

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

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

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> findById(@AuthenticationPrincipal UserDetails userDetails,
                                      @PathVariable Long id) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(transactionService.findByIdDto(id, user));
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long id) {
        User user = userService.findByEmail(userDetails.getUsername());
        transactionService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
