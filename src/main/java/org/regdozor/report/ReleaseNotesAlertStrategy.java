package org.regdozor.report;

import org.jsoup.nodes.Document;
import org.regdozor.profile.UserProduct;

import java.util.List;

/**
 * Реализация {@link AlertStrategy} для ленты релизов честныйзнак.рф — КОРОТКОЕ сообщение:
 * факт «вышел релиз по твоей группе» + ссылка на первоисточник + дисклеймер.
 *
 * ⚠️ БЕЗ товаров, этапов и цитат КоАП — это решение, а не упущение. На этом пути матч идёт по НАЗВАНИЮ
 * ГРУППЫ ({@code GroupRelevanceStrategy}), то есть мы знаем лишь, что релиз УПОМИНАЕТ группу клиента,
 * но не знаем, что в нём написано. Назвать конкретные товары нельзя (стратегия вернула весь ассортимент
 * профиля, а не затронутые позиции), привести этапы нельзя (раздела «Основные этапы» на странице
 * честныйзнака нет), процитировать санкцию нельзя (мы приписали бы ответственность непрочитанному
 * тексту). Поэтому сообщение честно говорит только то, что нам известно, и отправляет к источнику.
 *
 * Параметры {@code doc} и {@code userProducts} не используются — они есть ради общего контракта.
 * Класс без полей и состояния: одного экземпляра хватает на всё приложение.
 */
public class ReleaseNotesAlertStrategy implements AlertStrategy {
    @Override
    public String composeAlert(Document doc, String documentUrl, List<UserProduct> userProducts) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔔 <b>ЦРПТ опубликовал новый релиз по твоей товарной группе</b>\n");
        sb.append("Что именно изменилось — смотри первоисточник.\n\n");
        sb.append("📄 <a href=\"").append(documentUrl).append("\">Первоисточник</a>\n\n");
        sb.append("\n⚠\uFE0F ").append(AlertBuilder.DISCLAIMER);
        return  sb.toString();
    }
}
