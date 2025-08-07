package kr.hhplus.be.server.domain.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer stock;

    private Long price;

    private LocalDateTime regDate;


    protected Product() {}

    @Builder
    public Product(String name, int stock, long price, LocalDateTime regDate) {
        this.name = name;
        this.stock = stock;
        this.price = price;
        this.regDate = regDate;
    }

    public void deductStock(int quantity) {
        this.stock -= quantity;
    }


}

