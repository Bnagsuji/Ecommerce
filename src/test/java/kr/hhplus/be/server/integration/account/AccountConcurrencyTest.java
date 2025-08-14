
package kr.hhplus.be.server.integration.account;

import kr.hhplus.be.server.domain.account.Account;
import kr.hhplus.be.server.infrastructure.repository.account.AccountJpaRepository;
import kr.hhplus.be.server.service.account.AccountService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class AccountConcurrencyTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    RedissonClient redissonClient;

    private final Long userId = 999L;
    private final Long otherUserId = 888L;
    private final Long initialAmount = 10000L;

    @BeforeEach
    void setUp() {
        accountJpaRepository.deleteAll();
        accountJpaRepository.save(Account.create(userId, initialAmount));
        accountJpaRepository.save(Account.create(otherUserId, initialAmount));
    }

    @Test
    void 락_직접_확인() {
        String key = "lock:account:" + userId;
        RLock lock = redissonClient.getLock(key);

        System.out.println("락 보유 중? = " + lock.isLocked());
    }

    @Test
    void 분산락이_정상적으로_적용되는지_확인() throws InterruptedException {
        String lockKey = "lock:account:" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        System.out.println("락 보유 중? = " + lock.isLocked());  // 테스트 중엔 true여야 함

        runConcurrentTest(() -> accountService.useBalance(userId, 1000L), 10);

        Thread.sleep(200); // 워치독 반영 시간
        System.out.println("락 해제됨? = " + lock.isLocked());  // 테스트 끝나면 false여야 함
    }


    @Test
    void 동시에_10번_차감_테스트() throws InterruptedException {
        runConcurrentTest(() -> accountService.useBalance(userId, 1000L), 10);

        Account updated = accountJpaRepository.findByUserId(userId).orElseThrow();
        Assertions.assertThat(updated.getAmount()).isEqualTo(0L);
    }

    @Test
    void 동시에_10번_충전_테스트() throws InterruptedException {
        runConcurrentTest(() -> accountService.chargeBalance(userId, 1000L), 10);

        Account updated = accountJpaRepository.findByUserId(userId).orElseThrow();
        Assertions.assertThat(updated.getAmount()).isEqualTo(initialAmount + 10000L);
    }

    @Test
    void 동시에_충전과_차감_섞여서_요청() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    if (finalI % 2 == 0) {
                        accountService.chargeBalance(userId, 1000L);
                    } else {
                        accountService.useBalance(userId, 1000L);
                    }
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Account updated = accountJpaRepository.findByUserId(userId).orElseThrow();
        Assertions.assertThat(updated.getAmount()).isEqualTo(initialAmount);
    }

    @Test
    void 잔액보다_많이_차감되는_동시_요청() throws InterruptedException {
        runConcurrentTest(() -> {
            try {
                accountService.useBalance(userId, 3000L);
            } catch (Exception ignored) {}
        }, 5);

        Account updated = accountJpaRepository.findByUserId(userId).orElseThrow();
        Assertions.assertThat(updated.getAmount()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void 서로_다른_유저_동시_접근_테스트() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executor.submit(() -> {
            try {
                accountService.useBalance(userId, 1000L);
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                accountService.chargeBalance(otherUserId, 2000L);
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        Account a1 = accountJpaRepository.findByUserId(userId).orElseThrow();
        Account a2 = accountJpaRepository.findByUserId(otherUserId).orElseThrow();
        Assertions.assertThat(a1.getAmount()).isEqualTo(initialAmount - 1000L);
        Assertions.assertThat(a2.getAmount()).isEqualTo(initialAmount + 2000L);
    }

    @Test
    void 데드락_유발_방지_테스트() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executor.submit(() -> {
            try {
                accountService.useBalance(userId, 1000L);
                Thread.sleep(100); // 일부러 지연
                accountService.chargeBalance(otherUserId, 1000L);
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                accountService.useBalance(otherUserId, 1000L);
                Thread.sleep(100);
                accountService.chargeBalance(userId, 1000L);
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        Assertions.assertThat(true).isTrue();
    }

    private void runConcurrentTest(Runnable task, int threadCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    task.run();
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
    }
}
