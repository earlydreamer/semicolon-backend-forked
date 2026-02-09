package dukku.semicolon.boundedContext.product.in;


import dukku.common.global.UserUtil;
import dukku.semicolon.boundedContext.product.app.facade.FollowFacade;
import dukku.semicolon.shared.product.docs.FollowApiDocs;
import dukku.semicolon.shared.product.dto.follow.FollowActionResponse;
import dukku.semicolon.shared.product.dto.follow.FollowedSellerCardResponse;
import dukku.semicolon.shared.product.dto.follow.FollowerUserCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@FollowApiDocs.FollowTag
public class FollowController {

    private final FollowFacade followFacade;

    @PostMapping("/sellers/{sellerUuid}/follow")
    @FollowApiDocs.FollowSeller
    public FollowActionResponse followSeller(
            @PathVariable UUID sellerUuid
    ) {
        return followFacade.followSeller(UserUtil.getUserId(), sellerUuid);
    }

    @DeleteMapping("/sellers/{sellerUuid}/follow")
    @FollowApiDocs.UnfollowSeller
    public FollowActionResponse unfollowSeller(
            @PathVariable UUID sellerUuid
    ) {
        return followFacade.unfollowSeller(UserUtil.getUserId(), sellerUuid);
    }

    @GetMapping("/me/following/sellers")
    @FollowApiDocs.GetFollowedSellers
    public List<FollowedSellerCardResponse> getFollowedSellers() {
        return followFacade.getFollowedSellers(UserUtil.getUserId());
    }

    @GetMapping("/sellers/{sellerUuid}/followers")
    @FollowApiDocs.GetSellerFollowers
    public List<FollowerUserCardResponse> getSellerFollowers(
            @PathVariable UUID sellerUuid
    ) {
        return followFacade.getSellerFollowers(sellerUuid);
    }
}
