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
                throw new IllegalArgumentException("The external pack base URL must be plain HTTPS");
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid external pack base URL", ex);
        }
        return url.replaceAll("/+$", "") + "/" + fileName(sha1);
    }

    /**
     * GitHub's public Release asset URL. The tag is keyed by ZIP SHA-1 so the
     * exact asset is stable even if the Java plugin changes independently.
     */
    private static String validatedRepository(String repository) {
        if (repository == null || !repository.matches(
                "[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9][A-Za-z0-9_.-]{0,99}")) {
            throw new IllegalArgumentException("Invalid public GitHub repository");
        }
        return repository;
    }

    public static String githubRelease(String repository, String sha1) {
        String hash = fileName(sha1).substring("funnel-pack-".length(),
                "funnel-pack-".length() + 40);
        return "https://github.com/" + validatedRepository(repository)
                + "/releases/download/pack-" + hash + "/" + fileName(hash);
    }

    /**
     * Direct static GitHub raw download. Unlike /releases/download this URL
     * does not redirect to GitHub's short-lived signed release-assets endpoint.
     * ZIPs are automatically committed to the dedicated pack-assets branch.
     */
    public static String githubRaw(String repository, String sha1) {
        return "https://raw.githubusercontent.com/" + validatedRepository(repository)
                + "/pack-assets/" + fileName(sha1);
    }
}
