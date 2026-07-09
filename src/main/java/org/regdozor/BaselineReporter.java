package org.regdozor;

public class BaselineReporter {
    private final ProductLoader loader;
    private final AlertFormatter formatter;
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

    public void run() {
        Product[] products = loader.load();
        for (Product p : products) {
            notifier.send(formatter.format(p));
        }
    }
}
