package dukku.semicolon.boundedContext.product.in;


import dukku.common.global.UserUtil;
import dukku.semicolon.boundedContext.product.app.facade.FollowFacade;
import dukku.semicolon.shared.product.docs.FollowApiDocs;
import dukku.semicolon.shared.product.dto.follow.FollowSellerResponse;
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
    public FollowSellerResponse followSeller(
            @PathVariable UUID sellerUuid
    ) {
        return followFacade.followSeller(UserUtil.getUserId(), sellerUuid);
    }

    @DeleteMapping("/sellers/{sellerUuid}/follow")
    @FollowApiDocs.UnfollowSeller
    public void unfollowSeller(
            @PathVariable UUID sellerUuid
    ) {
        followFacade.unfollowSeller(UserUtil.getUserId(), sellerUuid);
    }

    @GetMapping("/me/following/sellers")
    @FollowApiDocs.GetFollowedSellers
    public List<FollowSellerResponse> getFollowedSellers() {
        return followFacade.getFollowedSellers(UserUtil.getUserId());
    }

    @GetMapping("/sellers/{sellerUuid}/followers")
    @FollowApiDocs.GetSellerFollowers
    public List<FollowSellerResponse> getSellerFollowers(
            @PathVariable UUID sellerUuid
    ) {
        return followFacade.getSellerFollowers(sellerUuid);
    }
}
