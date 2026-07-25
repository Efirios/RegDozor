package org.regdozor;

public class KoapArticleLocator {
    public int findArticleStart(String koapHtml, String baseNumber, int superscript) {
        String anchor;

        if (superscript == 0) {
            anchor = "Статья " + baseNumber + ".";
        } else {
            anchor = "Статья " + baseNumber + "<span class=\"W9\">" + superscript + "</span>";
        }

        int start = koapHtml.indexOf(anchor);

        if (start == -1) {
            throw new IllegalStateException(anchor + " не нашлась");
        }

        return start;
    }
}
