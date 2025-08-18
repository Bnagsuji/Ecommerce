package kr.hhplus.be.server.infrastructure.repository.product;

import kr.hhplus.be.server.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProductJpaRepository extends JpaRepository<Product, Long> {
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("SELECT p FROM Product p WHERE p.id = :id")
//    Optional<Product> findByIdForUpdate(@Param("id") Long id);

//    Optional<Product> findById(Long id);

}
