package org.regdozor.report;

import org.regdozor.catalog.AlertFormatter;
import org.regdozor.operator.ArticleTextFetcher;
import org.regdozor.match.RelevanceStrategy;
import org.regdozor.profile.Profile;
import org.regdozor.profile.UserProduct;
import org.regdozor.telegram.Broadcaster;

import java.util.List;

/**
 * Проверяет ОДИН документ и, если он касается товаров пользователя, шлёт уведомление.
 *
 * Координатор «дозора»: run(url) скачивает чистый текст и через внедрённую
 * СТРАТЕГИЮ ({@link RelevanceStrategy} — по кодам ИЛИ по группе, решается при сборке в App) проверяет,
 * относится ли документ к товарам/группе пользователя.
 * Если да — шлёт уведомление; не релевантно — молчит (сообщения, которого не должно быть, не создаём).
 *
 * Товары берёт из ПРОФИЛЯ (внедрён готовым, грузится один раз в App) — раньше на КАЖДЫЙ документ
 * перечитывался каталог, это ушло вместе с переездом кодов в профиль.
 *
 * ⚠️ ТЕКСТ СООБЩЕНИЯ — ЗАГЛУШКА («вышел релиз по твоей группе» + ссылка). По решению Б (2026-08-01)
 * его заменит пер-товарный алерт: товар + дата старта + ДОСЛОВНАЯ цитата этапов из статьи ЦРПТ +
 * риск из КоАП по обязанностям ГРУППЫ ({@code KoapRisk}) + ссылки + дисклеймер.
 */
public class DozorReporter {
    private final Profile profile;
    private final ArticleTextFetcher articleTextFetcher;
    private final RelevanceStrategy relevanceStrategy;
    private final Broadcaster broadcaster;

    public DozorReporter(Profile profile, ArticleTextFetcher articleTextFetcher,
                         RelevanceStrategy relevanceStrategy, Broadcaster broadcaster) {
        if (profile == null) {
            throw new IllegalArgumentException("profile не может быть null!");
        }
        this.profile = profile;

        if (articleTextFetcher == null) {
            throw new IllegalArgumentException("articleTextFetcher не может быть null!");
        }
        this.articleTextFetcher = articleTextFetcher;

        if (relevanceStrategy == null) {
            throw new IllegalArgumentException("relevanceStrategy не может быть null!");
        }
        this.relevanceStrategy = relevanceStrategy;

        if (broadcaster == null) {
            throw new IllegalArgumentException("broadcaster не может быть null!");
        }
        this.broadcaster = broadcaster;
    }

    /**
     * Обрабатывает ОДИН документ: скачать текст → проверить релевантность → при попадании разослать всем.
     * Если документ не про пользователя — молчит (сообщения, которого не должно быть, не создаём).
     *
     * @param documentUrl ссылка на документ (у нас — выпуск честныйзнак из ленты релизов)
     */
    public void run(String documentUrl) {
        String text = articleTextFetcher.fetchCleanText(documentUrl);
        List<UserProduct> relevant = relevanceStrategy.findRelevant(text, profile.products());

        if (relevant.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("🔔 <b>ЦРПТ опубликовал новый релиз по твоей товарной группе</b>\n");
        sb.append("Что именно изменилось — смотри первоисточник.\n\n");
        sb.append("📄 <a href=\"").append(documentUrl).append("\">Первоисточник</a>\n\n");
        sb.append("\n⚠\uFE0F ").append(AlertFormatter.DISCLAIMER);
        broadcaster.broadcast(sb.toString());
    }
}
