package org.regdozor;

import java.util.Set;

public class Broadcaster {
    private final SeenStore seenStore;
    private final TelegramNotifier telegramNotifier;

    public Broadcaster(SeenStore seenStore, TelegramNotifier telegramNotifier) {
        if (seenStore == null) {
            throw new IllegalArgumentException("seenStore не может быть null!");
        }
        this.seenStore = seenStore;

        if (telegramNotifier == null) {
            throw new IllegalArgumentException("telegramNotifier не может быть null!");
        }
        this.telegramNotifier = telegramNotifier;
    }

    public void broadcast(String text) {
        Set<String> chatIds = seenStore.load();
        for (String chatId : chatIds) {
            telegramNotifier.send(chatId, text);
        }
    }
}
