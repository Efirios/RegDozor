package org.regdozor;

/**
 * «Форматировщик»: превращает одну товарную карточку (Product) в готовый текст сообщения.
 * Собирает: заголовок (имя+коды) -> статус маркировки (3 ветки по markingRequired, включая null) ->
 * по каждой обязанности: срок, риск (или «не проверен»), основание -> дата сверки -> ДИСКЛЕЙМЕР.
 * Текст несёт HTML-разметку Telegram (&lt;b&gt;/&lt;u&gt;/&lt;a&gt;) — то есть класс СЕЙЧАС привязан к Telegram.
 * Это осознанный «шов»: при втором канале вывода (почта/iOS) надо будет развязать «что показать» и «как разметить».
 * Дисклеймер — обязательная часть каждого сообщения (продукт — «дозорный», не юрист).
 */
public class AlertFormatter {
    public static final String DISCLAIMER = "Не является юридической консультацией. Сверяйтесь с первоисточником.";

    public String format(Product p) {
        StringBuilder sb = new StringBuilder();

        sb.append("\uD83D\uDD14 <u><b>").append(p.productNames().get(0)).append("</b></u>").append(" (ТН ВЭД: ").append(p.code()).append(",").
                append(" ОКПД2: ").append(p.okpd2()).append(")").append("\n");

        if (p.markingRequired() == null) {
            sb.append("Статус маркировки не определён\n");
        } else if (p.markingRequired()) {
            sb.append("Подлежит обязательной маркировке\n");
        } else {
            sb.append("Не подлежит обязательной маркировке\n");
        }

        for (Obligation o : p.obligations()) {
            Risk r = o.risk();
            sb.append("\n• ").append(o.what()).append(" — с <u><b>").append(o.since()).append("</b></u>").append("\n");

            if (r != null) {
                sb.append("\uD83D\uDCB8 Риск: ").append(r.article()).append(" — <u><b>").append(r.fine()).append(" + ").
                        append(r.consequence()).append(".").append("</b></u>\n");
                sb.append("Норматив: <a href=\"").append(r.sourceUrl()).append("\">").append(r.article()).append("</a>\n");
                sb.append("Норматив проверен: ").append(r.verifiedOn()).append("\n");
            } else {
                sb.append("❔ Риск: не проверен").append("\n");
            }

            sb.append("Основание: <a href=\"").append(o.sourceUrl()).append("\">").append(o.source()).append("</a>\n");
        }

        sb.append("Коды и сроки проверены: ").append(p.verifiedOn()).append("\n");
        sb.append("\n⚠\uFE0F ").append(DISCLAIMER);

        return sb.toString();
    }
}
