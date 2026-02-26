package dukku.product.boundedContext.product.in;

import dukku.common.shared.product.dto.product.CategoryCreateRequest;
import dukku.common.shared.product.dto.product.CategoryCreateResponse;
import dukku.product.boundedContext.product.app.usecase.CreateCategoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;

    @PostMapping
    public ResponseEntity<CategoryCreateResponse> createCategory(
            @RequestBody @Validated CategoryCreateRequest request
    ) {
        CategoryCreateResponse response = createCategoryUseCase.execute(request);
        return ResponseEntity.ok(response);
    }
}
