package org.regdozor.match;

public class KoapArticleLocator {
    private int findArticleStart(String koapHtml, String baseNumber, int superscript) {
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

    private String cutArticle(String koapHtml, String baseNumber, int superscript) {
        int start = findArticleStart(koapHtml, baseNumber, superscript);
        int end = koapHtml.indexOf("<p class=\"H\"", start);

        if (end == -1) {
            return koapHtml.substring(start, koapHtml.length());
        } else {
            return koapHtml.substring(start, end);
        }
    }

    public String locateArticle(String koapHtml, String baseNumber, int superscript) {
        String article = cutArticle(koapHtml, baseNumber, superscript);

        if (!(article.contains("маркировк") && article.contains("должностных лиц"))) {
            throw new IllegalStateException("Нет совпадений по названиям. Статья " + baseNumber + "^" +
                    superscript + " могла измениться");
        }

        return article;
    }
}
