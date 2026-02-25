package dukku.common.shared.ai.dto;

public record ProductSearchFilter(
        Long minPrice,
        Long maxPrice
) {
    public static final ProductSearchFilter NONE = new ProductSearchFilter(null, null);

    public boolean hasMinPrice() {
        return minPrice != null;
    }

    public boolean hasMaxPrice() {
        return maxPrice != null;
    }

    public boolean hasAnyFilter() {
        return hasMinPrice() || hasMaxPrice();
    }
}
