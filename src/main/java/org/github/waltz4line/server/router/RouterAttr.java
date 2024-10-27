package org.github.waltz4line.server.router;

public record RouterAttr(String path, String tag) {

    public String pathConcat(String subPath) {
        return path() + subPath;
    }

    public static RouterAttr of(String path, String tag) {
        return new RouterAttr(path, tag);
    }

    public static RouterAttr of(String path) {
        return new RouterAttr(path, null);
    }
}
