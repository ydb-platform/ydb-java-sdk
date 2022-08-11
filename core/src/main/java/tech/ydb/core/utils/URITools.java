package tech.ydb.core.utils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class URITools {

    public static Map<String, List<String>> splitQuery(URI url) {
        if (Strings.isNullOrEmpty(url.getQuery())) {
            return Collections.emptyMap();
        }
        return Arrays.stream(url.getQuery().split("&"))
                .map(URITools::splitQueryParameter)
                .collect(Collectors.groupingBy(
                        SimpleImmutableEntry::getKey, 
                        LinkedHashMap::new, 
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private static SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new SimpleImmutableEntry<>(
                URLDecoder.decode(key, StandardCharsets.UTF_8),
                URLDecoder.decode(value, StandardCharsets.UTF_8)
        );
    }
}
