package org.regdozor;

/**
 * «Встречающий»: что происходит, когда человек ВПЕРВЫЕ написал боту (нажал Start).
 * Шлёт ему приветствие (кто мы, что сейчас придёт, что будет дальше, дисклеймер),
 * а следом — его baseline-карточки («что действует по твоим товарам сегодня»).
 *
 * Зачем отдельный класс: SubscriberMonitor занят своим делом — «опросить Telegram и вести реестр».
 * «Встретить новичка» — другая работа, поэтому она живёт здесь (одна ответственность на класс).
 * Признак новичка даёт сам реестр: Set.add() возвращает true, только если такого chat_id ещё не было.
 */
public class OnboardingReporter {
    /** «Оповещатель»: шлёт приветствие в ОДИН чат. */
    private final TelegramNotifier telegramNotifier;
    /** Присылает новичку карточки «что действует сегодня». */
    private final BaselineReporter baselineReporter;

    public OnboardingReporter(TelegramNotifier telegramNotifier, BaselineReporter baselineReporter) {
        if (telegramNotifier == null) {
            throw new IllegalArgumentException("telegramNotifier не может быть null!");
        }
        this.telegramNotifier = telegramNotifier;

        if (baselineReporter == null) {
            throw new IllegalArgumentException("baselineReporter не может быть null!");
        }
        this.baselineReporter = baselineReporter;
    }

    public void onboard(String chatId) {
        StringBuilder sb = new StringBuilder();

        sb.append("<b>РегДозор</b> — слежу за изменениями в маркировке „Честный знак“ по твоей товарной группе (одежда и бельё).\n");
        sb.append("Cейчас пришлю, что действует по твоим товарам сегодня.\n");
        sb.append("Дальше буду писать, когда ЦРПТ опубликует что-то новое по твоей группе.\n\n");
        sb.append("\n⚠\uFE0F ").append(AlertFormatter.DISCLAIMER);

        telegramNotifier.send(chatId, sb.toString());
        baselineReporter.sendTo(chatId);
    }
}
