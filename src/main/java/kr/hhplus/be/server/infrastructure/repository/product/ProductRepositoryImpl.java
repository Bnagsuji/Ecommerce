package kr.hhplus.be.server.infrastructure.repository.product;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.hhplus.be.server.controller.product.response.TopSellingProductResponse;
import kr.hhplus.be.server.domain.order.QOrder;
import kr.hhplus.be.server.domain.order.QOrderItem;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.product.QProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final JPAQueryFactory queryFactory;
    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public List<Product> findAllById(List<Long> ids) {
        return productJpaRepository.findAllById(ids);
    }

    @Override
    public List<Product> findTopSellingList(int days, int limit) {

        QOrderItem orderItem = QOrderItem.orderItem;
        QProduct product = QProduct.product;
        QOrder order = QOrder.order;

        LocalDateTime from = LocalDateTime.now().minusDays(days);
        return queryFactory
                .select(product)
                .from(orderItem)
                .join(order).on(order.id.eq(orderItem.orderId))
                .join(product).on(product.id.eq(orderItem.productId))
                .where(order.orderDate.between(from, LocalDateTime.now()))
                .groupBy(product.id)
                .orderBy(orderItem.quantity.sum().desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long id) {
        return productJpaRepository.findByIdForUpdate(id);
    }

}
