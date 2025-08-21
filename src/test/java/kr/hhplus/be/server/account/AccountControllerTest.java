package kr.hhplus.be.server.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.controller.account.AccountController;
import kr.hhplus.be.server.controller.account.request.AccountRequest;
import kr.hhplus.be.server.controller.account.request.TransactionType;
import kr.hhplus.be.server.controller.account.response.AccountHistoryResponse;
import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.service.account.AccountService;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 현재_잔액_조회_성공_테스트() throws Exception {
        // given
        AccountResponse mockResponse = new AccountResponse(1L, 10000L);
        BDDMockito.given(accountService.getBalance(1L)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/balance/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.amount").value(10000L));
    }

    @Test
    void 잔액_충전_성공_테스트() throws Exception {
        // given
        AccountRequest request = new AccountRequest(1L, 5000L);
        AccountResponse mockResponse = new AccountResponse(1L, 15000L);
        BDDMockito.given(accountService.chargeBalance(1L, 5000L)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/balance/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.amount").value(15000L));
    }

    @Test
    void 잔액_차감_성공_테스트() throws Exception {
        // given
        AccountRequest request = new AccountRequest(1L, 3000L);
        AccountResponse mockResponse = new AccountResponse(1L, 7000L);
        BDDMockito.given(accountService.useBalance(1L, 3000L)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/balance/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.amount").value(7000L));
    }

    @Test
    void 계좌_히스토리_조회_성공_테스트() throws Exception {
        // given
        AccountHistoryResponse history1 = new AccountHistoryResponse(1L, 5000L, TransactionType.CHARGE, LocalDateTime.now());
        AccountHistoryResponse history2 = new AccountHistoryResponse(1L, -3000L, TransactionType.USE, LocalDateTime.now());
        List<AccountHistoryResponse> mockHistory = List.of(history1, history2);

        BDDMockito.given(accountService.getHistory(1L)).willReturn(mockHistory);

        // when & then
        mockMvc.perform(get("/api/balance/{memberId}/history", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1L))
                .andExpect(jsonPath("$[0].amount").value(5000L))
                .andExpect(jsonPath("$[0].type").value("CHARGE"));
    }
}
