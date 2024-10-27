package org.github.waltz4line.router;

public record RouterAttr(String path, String tag) {

    public static RouterAttr of(String path, String tag) {
        return new RouterAttr(path, tag);
    }

    public static RouterAttr of(String path) {
        return new RouterAttr(path, null);
    }
}
