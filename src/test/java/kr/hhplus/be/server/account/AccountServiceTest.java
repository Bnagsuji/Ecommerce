package kr.hhplus.be.server.account;

import kr.hhplus.be.server.controller.account.request.TransactionType;
import kr.hhplus.be.server.controller.account.response.AccountHistoryResponse;
import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.repository.account.AccountRepository;
import kr.hhplus.be.server.service.account.AccountService;
import kr.hhplus.be.server.service.account.impl.AccountServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {


    @InjectMocks
    private AccountServiceImpl accountService;

    @Mock
    private AccountRepository accountRepository;

    @Test
    void 잔액_조회_성공_테스트() {
        //given
        Long userId =1L;
        //목객체랑 서비스에서 받아온 값이랑 비교
        AccountResponse origin = new AccountResponse(userId,200L);

        //when
        AccountResponse res = accountService.getBalance(userId);

        //then
        Assertions.assertEquals(origin, res);
    }


    @Test
    void 잔액_조회_유저없음_실패_테스트() {
        //given
        Long userId =900L;

        //when
        Exception ex =
                assertThrows(IllegalArgumentException.class,()-> accountService.getBalance(userId));

        //then
        assertEquals("존재하지 않는 사용자입니다.",ex.getMessage());
    }

    @Test
    void 기존_유저_잔액_충전_성공_테스트() {
        //given
        Long userId =1L;
        Long amount = 200L;
        Long chargeAmount = 300L;
        Long resAmount = amount+chargeAmount;

        //가짜 계좌 잔액 넣기
        AccountResponse origin = new AccountResponse(userId,resAmount);

        //when
        AccountResponse res = accountService.chargeBalance(userId,chargeAmount);

        //then
        Assertions.assertEquals(origin,res);
    }

    @Test
    void 신규_유저_잔액_충전_성공_테스트() {
        //given
        Long userId =999L;
        Long chargeAmount = 300L;

        //가짜 계좌 잔액 넣기
        AccountResponse origin = new AccountResponse(userId,chargeAmount);

        //when
        AccountResponse res = accountService.chargeBalance(userId,chargeAmount);

        //then
        Assertions.assertEquals(origin,res);
    }


    @Test
    void 잔액_최소_충전_정책_실패_테스트() {
        //given
        Long userId =900L;
        Long amount = 99L;

        //when
        Exception ex =
                assertThrows(IllegalArgumentException.class,()-> accountService.chargeBalance(userId,amount));

        //then
        assertEquals("최소 잔액 충전 정책에 맞지 않는 금액입니다.",ex.getMessage());
    }

    @Test
    void 잔액_최대_충전_정책_실패_테스트() {
        //given
        Long userId =900L;
        Long amount = 100001L;

        //when
        Exception ex =
                assertThrows(IllegalArgumentException.class,()-> accountService.chargeBalance(userId,amount));

        //then
        assertEquals("최대 잔액 충전 정책에 맞지 않는 금액입니다.",ex.getMessage());
    }

    @Test
    void 기존_유저_잔액_사용_성공_테스트() {
        //사용자 잔액 불러와서 사용해서 계좌에 차감되었는 지 확인
        Long userId =2L;
        Long originAmount=300L;
        Long useAmount = 200L;
        Long resAmount = originAmount-useAmount;

        //사용 후 예상되는 잔액 설정
        AccountResponse origin = new AccountResponse(userId,resAmount);

        //when
        AccountResponse res = accountService.useBalance(userId,useAmount);

        //then
        Assertions.assertEquals(origin,res);
    }

    @Test
    void 기존_유저_잔액_사용_최저기준_정책_실패_테스트() {
        //사용자 잔액 불러와서 사용해서 계좌에 차감되었는 지 확인
        Long userId =2L;
        Long useAmount = 99L;

        //when
        Exception ex =
                assertThrows(IllegalArgumentException.class,()-> accountService.useBalance(userId,useAmount));

        //then
        assertEquals("현재 100원 부터 사용 가능합니다.",ex.getMessage());
    }

    @Test
    void 기존_유저_잔액_사용_최대기준_정책_실패_테스트() {
        //사용자 잔액 불러와서 사용해서 계좌에 차감되었는 지 확인
        Long userId =2L;
        Long useAmount = 600L;

        //when
        Exception ex =
                assertThrows(IllegalArgumentException.class,()-> accountService.useBalance(userId,useAmount));

        //then
        assertEquals("사용하려는 금액이 현재 보유 중인 금액보다 많습니다.",ex.getMessage());
    }

    @Test
    void 금액_충전_시_히스토리_저장_테스트() {
        //given
        Long userId =2L;
        Long amount = 300L;

        AccountResponse res =  new AccountResponse(userId,amount);

        //when
        //위에 저장된 값 히스토리 저장
        AccountHistoryResponse historyRes =  accountService.saveHistory(res,TransactionType.CHARGE);

        //히스토리 값 비교
        //then
        Assertions.assertEquals(userId, historyRes.userId());
        Assertions.assertEquals(amount, historyRes.amount());
        Assertions.assertEquals(TransactionType.CHARGE, historyRes.type());
    }

    @Test
    void 금액_사용_시_히스토리_저장_테스트() {
        //given
        Long userId =2L;
        Long amount = 500L;

        AccountResponse res =  new AccountResponse(userId,amount);

        //when
        //위에 저장된 값 히스토리 저장
        AccountHistoryResponse historyRes =  accountService.saveHistory(res,TransactionType.USE);

        //히스토리 값 비교
        //then
        Assertions.assertEquals(userId, historyRes.userId());
        Assertions.assertEquals(amount, historyRes.amount());
        Assertions.assertEquals(TransactionType.USE, historyRes.type());
    }


}
