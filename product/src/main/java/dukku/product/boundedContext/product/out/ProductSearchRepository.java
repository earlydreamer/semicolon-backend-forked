package dukku.product.boundedContext.product.out;

import dukku.product.boundedContext.product.entity.query.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
}
