package dukku.semicolon.boundedContext.product.app.support;

import dukku.semicolon.boundedContext.product.entity.tag.Tag;
import dukku.semicolon.boundedContext.product.out.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductTagSupport {
    private final TagRepository tagRepository;

    @Transactional
    public List<Tag> getOrCreateTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }

        // 중복 제거 및 공백 제거
        List<String> uniqueNames = tagNames.stream()
                .map(String::trim)
                .distinct()
                .filter(name -> !name.isBlank())
                .toList();

        List<Tag> result = new ArrayList<>();

        for (String name : uniqueNames) {
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> tagRepository.save(Tag.create(name)));
            result.add(tag);
        }

        return result;
    }
}
