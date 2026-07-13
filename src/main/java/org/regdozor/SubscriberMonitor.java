package org.regdozor;

import java.util.Set;

public class SubscriberMonitor {
    private final TelegramReceiver telegramReceiver;
    private final OffsetStore offsetStore;
    private final SeenStore seenStore;

    public SubscriberMonitor(TelegramReceiver telegramReceiver, OffsetStore offsetStore, SeenStore seenStore) {
        if (telegramReceiver == null) {
            throw new IllegalArgumentException("telegramReceiver не может быть null!");
        }
        this.telegramReceiver = telegramReceiver;

        if (offsetStore == null) {
            throw new IllegalArgumentException("offsetStore не может быть null!");
        }
        this.offsetStore = offsetStore;

        if (seenStore == null) {
            throw new IllegalArgumentException("seenStore не может быть null!");
        }
        this.seenStore = seenStore;
    }

    public void run() {
        long offset = offsetStore.load();
        GetUpdatesResponse getUpdatesResponse = telegramReceiver.receive(offset);
        Set<String> chatIds = seenStore.load();
        long maxUpdateId = offset - 1;

        for (Update update : getUpdatesResponse.result()) {
            if (update.message() != null) {
                chatIds.add(String.valueOf(update.message().chat().id()));
            }

            if (update.update_id() > maxUpdateId) {
                maxUpdateId = update.update_id();
            }
        }

        seenStore.save(chatIds);
        offsetStore.save(maxUpdateId + 1);
    }
}
