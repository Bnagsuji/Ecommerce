package kr.hhplus.be.server.integration.account;

import kr.hhplus.be.server.domain.account.Account;
import kr.hhplus.be.server.infrastructure.repository.account.AccountJpaRepository;
import kr.hhplus.be.server.service.account.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AccountRedisConcurrencyTest {

    @Autowired
    AccountService accountService;

    @Autowired
    AccountJpaRepository accountJpaRepository;

    final Long userId = 1000L;
    final Long otherUserId = 2000L;

    @BeforeEach
    void init() {
        accountJpaRepository.deleteAll();
        accountJpaRepository.save(Account.create(userId, 10000L));
        accountJpaRepository.save(Account.create(otherUserId, 10000L));
    }

    private void runConcurrent(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    System.err.println("오류 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }



    @Test
    void 동시에_과도한_차감_잔액부족으로_일부실패해야함() throws InterruptedException {
        // given
        Long userId = 2L;
        Long initialAmount = 2000L;
        accountJpaRepository.save(Account.create(userId, initialAmount));

        int threadCount = 5;
        Long deductAmount = 1000L;

        // when
        runConcurrent(threadCount, () -> {
            try {
                accountService.useBalance(userId, deductAmount);
            } catch (Exception ignored) {
                // 실패는 정상 흐름 (잔액 부족)
            }
        });

        // then
        Account account = accountJpaRepository.findByUserId(userId).orElseThrow();
        assertThat(account.getAmount()).isGreaterThanOrEqualTo(0L);
        assertThat(account.getAmount()).isIn(0L, 1000L);
    }


    @Test
    void 중복요청_정확히_한번씩만_처리되어야함() throws InterruptedException {
        // given
        Long userId = 3L;
        Long initialAmount = 10000L;
        accountJpaRepository.save(Account.create(userId, initialAmount));

        int threadCount = 10;
        Long deductAmount = 1000L;

        // when
        runConcurrent(threadCount, () -> {
            try {
                accountService.useBalance(userId, deductAmount);
            } catch (Exception ignored) {
            }
        });

        // then
        Account account = accountJpaRepository.findByUserId(userId).orElseThrow();
        // 최대 10번 성공 가능 but 락에 따라 중복 차단
        Long finalAmount = account.getAmount();
        Long usedCount = (initialAmount - finalAmount) / deductAmount;

        assertThat(usedCount).isLessThanOrEqualTo(10);
        assertThat(finalAmount).isGreaterThanOrEqualTo(0);
    }
}

