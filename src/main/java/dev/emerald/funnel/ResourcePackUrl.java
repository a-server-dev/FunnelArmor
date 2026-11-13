package dev.emerald.funnel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Immutable public pack URLs keyed by the exact SHA-1 of the bundled ZIP. */
public final class ResourcePackUrl {
    private ResourcePackUrl() {}

    public static String fileName(String sha1) {
        if (sha1 == null || !sha1.matches("[a-fA-F0-9]{40}")) {
            throw new IllegalArgumentException("Invalid resource-pack SHA-1");
        }
        return "funnel-pack-" + sha1.toLowerCase(Locale.ROOT) + ".zip";
    }

    public static String fromBase(String base, String sha1) {
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("pack.external-base-url is empty");
        }
        String url = base.trim();
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null) {
                throw new IllegalArgumentException("The R2 public base URL must be plain HTTPS");
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid R2 public base URL", ex);
        }
        return url.replaceAll("/+$", "") + "/" + fileName(sha1);
    }
}
