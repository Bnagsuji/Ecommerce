package kr.hhplus.be.server.controller.product.response;

import kr.hhplus.be.server.domain.product.Product;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
public record ProductResponse(
        Long id,
        String name,
        int stock,
        long price,
        LocalDateTime regDate
){

    //다른 객체로부터 변환
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getStock(),
                product.getPrice(),
                product.getRegDate()
        );

    }

    public static Map<Long, ProductResponse> toMapById(List<ProductResponse> list) {
        return list.stream().collect(Collectors.toMap(ProductResponse::id, it -> it));
    }

}