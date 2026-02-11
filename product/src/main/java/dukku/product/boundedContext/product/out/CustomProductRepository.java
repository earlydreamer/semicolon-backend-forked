package dukku.product.boundedContext.product.out;

import dukku.common.shared.product.dto.cqrs.ProductStatDto;

import java.util.List;

public interface CustomProductRepository {
    void bulkUpdateProductStats(List<ProductStatDto> stats);
}
