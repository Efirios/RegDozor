package org.regdozor;

/**
 * Координатор ветки «baseline» («что действует у тебя СЕГОДНЯ»): в отличие от «дозора»
 * (реакция на новый документ) — берёт весь каталог и шлёт по карточке на каждый товар.
 * Нужен для «холодного старта»: пользователь сразу видит свои действующие обязанности.
 * Тонкий класс-дирижёр: сам ничего не считает, только связывает трёх помощников
 * (загрузка -> форматирование -> отправка).
 */
public class BaselineReporter {
    /** «Загрузчик» каталога товаров из products.json. */
    private final ProductLoader loader;
    /** «Форматировщик» одной товарной карточки в текст с HTML-разметкой Telegram. */
    private final AlertFormatter formatter;
    /** «Рассыльщик»: шлёт сообщение всем подписчикам из реестра. */
    private final Broadcaster broadcaster;

    public BaselineReporter(ProductLoader loader, AlertFormatter formatter, Broadcaster broadcaster) {
        if (loader == null) {
            throw new IllegalArgumentException("loader не может быть null!");
        }
        this.loader = loader;

        if (formatter == null) {
            throw new IllegalArgumentException("formatter не может быть null!");
        }
        this.formatter = formatter;

        if (broadcaster == null) {
            throw new IllegalArgumentException("broadcaster не может быть null!");
        }
        this.broadcaster = broadcaster;
    }

    /** Загружает каталог и отправляет по одному сообщению-карточке на каждый товар. */
    public void run() {
        Product[] products = loader.load();
        for (Product p : products) {
            broadcaster.broadcast(formatter.format(p));
        }
    }
}
