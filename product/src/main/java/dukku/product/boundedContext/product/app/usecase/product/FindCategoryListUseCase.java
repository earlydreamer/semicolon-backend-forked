package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.shared.product.dto.product.CategoryCreateResponse;
import dukku.product.boundedContext.product.entity.Category;
import dukku.product.boundedContext.product.out.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FindCategoryListUseCase {
    private final CategoryRepository categoryRepository;

    public List<CategoryCreateResponse> execute() {
        return categoryRepository.findAll().stream()
                .map(Category::from)
                .toList();
    }
}
