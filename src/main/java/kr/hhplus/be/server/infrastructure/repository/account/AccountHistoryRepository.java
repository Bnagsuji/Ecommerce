package kr.hhplus.be.server.infrastructure.repository.account;

import kr.hhplus.be.server.domain.account.AccountHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountHistoryRepository extends JpaRepository<AccountHistory, Long> {
    List<AccountHistory> findByUserId(Long userId);
}
