package kr.hhplus.be.server.domain.product;

import com.querydsl.core.Tuple;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long id);
    List<Product> findAllById(List<Long> ids);
    List<Product> findTopSellingList(int days, int limit);
}
