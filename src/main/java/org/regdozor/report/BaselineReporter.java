package org.regdozor.report;

import org.regdozor.catalog.AlertFormatter;
import org.regdozor.catalog.Product;
import org.regdozor.catalog.ProductLoader;
import org.regdozor.telegram.TelegramNotifier;

/**
 * Шлёт новому подписчику карточки по всем его товарам — «что действует сегодня».
 *
 * Координатор ветки «baseline»: в отличие от «дозора»
 * (реакция на новый документ) — берёт весь каталог и шлёт по карточке на каждый товар.
 * Решает «холодный старт»: подписчик сразу видит свои действующие обязанности.
 *
 * Шлёт ОДНОМУ названному чату (sendTo), а не всем: baseline — это событие ПОДПИСКИ,
 * а не запуска приложения. Раньше он рассылался всем на старте — при каждом перезапуске
 * все получали карточки заново (спам), а подписавшийся позже не получал ничего.
 * Теперь его зовёт онбординг нового подписчика.
 *
 * Тонкий класс-дирижёр: сам ничего не считает, только связывает трёх помощников
 * (загрузка -> форматирование -> отправка).
 */
public class BaselineReporter {
    /** «Загрузчик» каталога товаров из products.json. */
    private final ProductLoader loader;
    /** «Форматировщик» одной товарной карточки в текст с HTML-разметкой Telegram. */
    private final AlertFormatter formatter;
    /** «Оповещатель»: шлёт текст в ОДИН названный чат. */
    private final TelegramNotifier notifier;

    public BaselineReporter(ProductLoader loader, AlertFormatter formatter, TelegramNotifier notifier) {
        if (loader == null) {
            throw new IllegalArgumentException("loader не может быть null!");
        }
        this.loader = loader;

        if (formatter == null) {
            throw new IllegalArgumentException("formatter не может быть null!");
        }
        this.formatter = formatter;

        if (notifier == null) {
            throw new IllegalArgumentException("notifier не может быть null!");
        }
        this.notifier = notifier;
    }

    /**
     * Загружает каталог и отправляет указанному чату по одному сообщению-карточке на каждый товар.
     *
     * @param chatId кому шлём (обычно — новому подписчику при онбординге)
     */
    public void sendTo(String chatId) {
        Product[] products = loader.load();
        for (Product p : products) {
            notifier.send(chatId, formatter.format(p));
        }
    }
}
