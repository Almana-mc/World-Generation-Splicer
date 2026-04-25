package me.almana.wgsplicer.core.scan;

import me.almana.wgsplicer.core.api.ScanMode;
import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.domain.BlockMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BlockMatcherBuilder {

    private BlockMatcherBuilder() {}

    public static BlockMatcher build(ScanMode mode,
                                     Set<BlockId> oreTagBlocks,
                                     List<String> extraBlockIds,
                                     List<String> extraNamespaces) {
        Set<BlockId> configured = parseConfigured(extraBlockIds);
        Set<String> namespaces = collectNamespaces(extraNamespaces);

        switch (mode) {
            case ALL_BLOCKS_DEBUG:
                return id -> true;
            case ORES_ONLY:
                return oreTagBlocks::contains;
            case CONFIGURED_ONLY:
                return id -> configured.contains(id) || namespaces.contains(id.namespace());
            case ORES_AND_CONFIGURED:
            default:
                return id -> oreTagBlocks.contains(id)
                        || configured.contains(id)
                        || namespaces.contains(id.namespace());
        }
    }

    private static Set<BlockId> parseConfigured(List<String> ids) {
        Set<BlockId> out = new HashSet<>();
        for (String s : ids) {
            if (s == null || s.isEmpty()) continue;
            int colon = s.indexOf(':');
            if (colon <= 0 || colon == s.length() - 1) continue;
            out.add(new BlockId(s.substring(0, colon), s.substring(colon + 1)));
        }
        return out;
    }

    private static Set<String> collectNamespaces(List<String> namespaces) {
        Set<String> out = new HashSet<>();
        for (String ns : namespaces) {
            if (ns != null && !ns.isEmpty()) out.add(ns);
        }
        return out;
    }
}
