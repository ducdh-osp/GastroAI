package vn.gastroai.be.domain.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalog_items")
@Getter
@Setter
@NoArgsConstructor
public class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CatalogType type;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active;
}
