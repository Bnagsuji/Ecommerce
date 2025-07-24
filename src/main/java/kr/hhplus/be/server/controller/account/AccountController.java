package kr.hhplus.be.server.controller.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.controller.account.request.AccountRequest;
import kr.hhplus.be.server.controller.account.response.AccountHistoryResponse;
import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="계좌",description = "계좌 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/balance")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{userId}")
    @Operation(summary = "현재 잔액 조회",description = "현재 잔액 정보 조회하는 API")
    public AccountResponse getBalance(@PathVariable Long userId) {
        return accountService.getBalance(userId);
    }




    @PostMapping("/charge")
    @Operation(summary = "잔액 충전",description = "잔액 충전 하는 API")
    public AccountResponse chargeBalance(@RequestBody AccountRequest request) {
        return accountService.chargeBalance(request.getUserId(), request.getAmount());
    }

    @PostMapping("/deduct")
    @Operation(summary = "잔액 차감",description = "잔액 차감 하는 API")
    public AccountResponse deductBalance(@RequestBody AccountRequest request) {
        return accountService.useBalance(request.getUserId(), request.getAmount());
    }

    @GetMapping("/{memberId}/history")
    @Operation(summary = "계좌 히스토리",description = "계좌 히스토리 조회 API")
    public List<AccountHistoryResponse> getBalanceHistory(@PathVariable Long memberId) {
        return accountService.getHistory(memberId);
    }
}
