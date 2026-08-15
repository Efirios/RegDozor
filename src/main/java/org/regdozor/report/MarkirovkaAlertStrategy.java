package org.regdozor.report;

import org.jsoup.nodes.Document;
import org.regdozor.match.KoapRisk;
import org.regdozor.match.ObligationRisk;
import org.regdozor.operator.MarkirovkaStagesExtractor;
import org.regdozor.profile.Profile;
import org.regdozor.profile.UserProduct;

import java.util.List;

/**
 * Реализация {@link AlertStrategy} для статей markirovka.ru — ПЕР-ТОВАРНЫЙ алерт:
 * какие товары затронуты, что и когда меняется, чем грозит нарушение.
 *
 * Сам ничего не добывает — дирижёр: знает, кого и в каком порядке позвать.
 * Три вопроса клиента — три разных источника ответа:
 * <ul>
 *   <li><b>«мой ли это товар»</b> — уже решено до нас: {@code RelevanceChecker} сверил коды товара
 *       с текстом статьи, и совпавшие приехали в {@code userProducts};</li>
 *   <li><b>«что меняется и когда»</b> — {@link MarkirovkaStagesExtractor} по {@code doc}: календарь
 *       дословно, первый этап несёт дату старта. Цитируем, а не пересказываем;</li>
 *   <li><b>«чем грозит»</b> — {@link KoapRisk} по ГРУППЕ товара: обязанности берутся от группы через
 *       выверенную таблицу, а не выводятся из текста статьи (статья не называет их нашими словами).</li>
 * </ul>
 *
 * ⚠️ ГРУППА БЕРЁТСЯ У ПЕРВОГО совпавшего товара — сознательное упрощение под НЫНЕШНЕЕ устройство
 * сообщения: у {@link AlertBuilder} один блок «чем грозит» на весь алерт, без разбивки по группам.
 * Пока профиль одногруппный (одежда), это точно; при торговле в нескольких группах риски разных групп
 * свалились бы в одну кучу без подписи. Мультигруппность — отдельная задача, где меняются ОБА класса:
 * здесь собрать уникальные группы, в сборщике — сделать блок на каждую.
 *
 * ⚠️ НЕЯВНЫЙ ДОГОВОР с {@link DozorReporter}: {@code getFirst()} на пустом списке бросает исключение,
 * и держится это на том, что репортёр зовёт стратегию ТОЛЬКО после проверки {@code relevant.isEmpty()}.
 * Не переставлять ту проверку.
 */
public class MarkirovkaAlertStrategy implements AlertStrategy {
    private final MarkirovkaStagesExtractor markirovkaStagesExtractor;
    private final KoapRisk koapRisk;
    private final AlertBuilder alertBuilder;
    private final Profile profile;

    public MarkirovkaAlertStrategy(MarkirovkaStagesExtractor markirovkaStagesExtractor, KoapRisk koapRisk,
                                   AlertBuilder alertBuilder, Profile profile) {
        if (markirovkaStagesExtractor == null) {
            throw new IllegalArgumentException("markirovkaStagesExtractor не может быть null!");
        }
        this.markirovkaStagesExtractor = markirovkaStagesExtractor;

        if (koapRisk == null) {
            throw new IllegalArgumentException("koapRisk не может быть null!");
        }
        this.koapRisk = koapRisk;

        if (alertBuilder == null) {
            throw new IllegalArgumentException("alertBuilder не может быть null!");
        }
        this.alertBuilder = alertBuilder;

        if (profile == null) {
            throw new IllegalArgumentException("profile не может быть null!");
        }
        this.profile = profile;
    }

    @Override
    public String composeAlert(Document doc, String documentUrl, List<UserProduct> userProducts) {
        // этапы читаем по ТОМУ ЖЕ Document, что использовал матч — второго запроса к сайту не будет,
        // и календарь гарантированно из той же версии страницы, что и совпадение по кодам
        List<String> stages = markirovkaStagesExtractor.extractStages(doc);
        // группа — свойство ТОВАРА, а не статьи; список непуст по договору с DozorReporter (см. выше)
        UserProduct userProduct = userProducts.getFirst();
        String group = userProduct.group();
        // по группе — все её обязанности сразу; текст КоАП внутри читается ОДИН раз на обе
        List<ObligationRisk> obligationRisks = koapRisk.risksForGroup(group, profile.subject());
        // субъект уходит дважды и по разным поводам: в риски — чтобы выбрать абзац санкции
        // («на должностных лиц»), в сборщик — чтобы назвать его человеку («для ИП»)
        return alertBuilder.glueMessage(userProducts, stages, obligationRisks, profile.subject(), documentUrl);
    }
}
