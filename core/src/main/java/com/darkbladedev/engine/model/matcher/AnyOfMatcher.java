package com.darkbladedev.engine.model.matcher;

import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.MatchResult;
import org.bukkit.block.Block;
import java.util.List;

public record AnyOfMatcher(List<BlockMatcher> matchers) implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        for (BlockMatcher matcher : matchers) {
            if (matcher.matches(block)) return true;
        }
        return false;
    }

    @Override
    public MatchResult match(Block block) {
        if (matchers == null || matchers.isEmpty()) {
            return MatchResult.fail("no matchers");
        }
        StringBuilder reasons = new StringBuilder();
        int failures = 0;
        for (BlockMatcher matcher : matchers) {
            if (matcher == null) {
                continue;
            }
            MatchResult res = matcher.match(block);
            if (res.success()) {
                return MatchResult.ok();
            }
            failures++;
            if (failures <= 3) {
                if (!reasons.isEmpty()) {
                    reasons.append("; ");
                }
                String r = res.reason();
                reasons.append(r == null || r.isBlank() ? matcher.getClass().getSimpleName() : r);
            }
        }
        return MatchResult.fail(reasons.isEmpty() ? "no options matched" : "no options matched: " + reasons);
    }
}
