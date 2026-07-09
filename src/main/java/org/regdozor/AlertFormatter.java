package org.regdozor;

public class AlertFormatter {
    private static final String DISCLAIMER = "Не является юридической консультацией. Сверяйтесь с первоисточником.";

    public String format(Product p) {
        StringBuilder sb = new StringBuilder();

        sb.append("\uD83D\uDD14 ").append(p.productNames().get(0)).append(" (Код ТН ВЭД: ").append(p.code()).append(")").
                append("\n");

        if (p.markingRequired() == null) {
            sb.append("Статус маркировки не определён\n");
        } else if (p.markingRequired()) {
            sb.append("Подлежит обязательной маркировке\n");
        } else {
            sb.append("Не подлежит обязательной маркировке\n");
        }

        for (Obligation o : p.obligations()) {
            sb.append("• ").append(o.what()).append(" — с ").append(o.since()).append("\n");
        }

        sb.append("\nОснование: ").append(p.wave()).append("\n");
        sb.append("Проверено: ").append(p.verifiedOn()).append("\n");
        sb.append("⚠\uFE0F ").append(DISCLAIMER);

        return sb.toString();
    }
}
