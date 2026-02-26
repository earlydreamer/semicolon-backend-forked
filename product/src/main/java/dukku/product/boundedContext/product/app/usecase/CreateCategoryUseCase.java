package dukku.product.boundedContext.product.app.usecase;

import dukku.common.global.exception.BadRequestException;
import dukku.common.shared.product.dto.product.CategoryCreateRequest;
import dukku.common.shared.product.dto.product.CategoryCreateResponse;
import dukku.product.boundedContext.product.entity.Category;
import dukku.product.boundedContext.product.out.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCategoryUseCase {
    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryCreateResponse execute(CategoryCreateRequest request) {
        if (categoryRepository.findByCategoryName(request.getName()).isPresent()) {
            throw new BadRequestException("이미 존재하는 카테고리 이름입니다.");
        }

        Category category;
        if (request.getParentId() == null) {
            category = Category.createRoot(request.getName());
        } else {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BadRequestException("상위 카테고리를 찾을 수 없습니다."));
            
            if (parent.getDepth() >= 3) {
                throw new BadRequestException("카테고리는 최대 3단계까지만 생성 가능합니다.");
            }
            category = Category.createChild(request.getName(), parent);
        }

        Category savedCategory = categoryRepository.save(category);
        return Category.from(savedCategory);
    }
}
