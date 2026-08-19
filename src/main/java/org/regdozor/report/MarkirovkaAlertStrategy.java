package org.regdozor.report;

import org.jsoup.nodes.Document;
import org.regdozor.match.ObligationRisk;
import org.regdozor.match.RiskMemo;
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
 *   <li><b>«чем грозит»</b> — {@link RiskMemo} по ГРУППЕ товара: обязанности берутся от группы через
 *       выверенную таблицу, а не выводятся из текста статьи (статья не называет их нашими словами).</li>
 * </ul>
 *
 * ⚠️ Риски спрашиваются у ПАМЯТКИ, а не у {@code KoapRisk} напрямую, и своего экземпляра калькулятора
 * этот класс не держит. Причина: дозор идёт циклом по подписчикам, стратегию зовут по одному человеку
 * за раз, и она не знает, кто спросит после неё. Памятка общая на весь прогон — она и не даёт прочитать
 * текст КоАП дважды ради одинакового ответа.
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
    private final AlertBuilder alertBuilder;

    public MarkirovkaAlertStrategy(MarkirovkaStagesExtractor markirovkaStagesExtractor, AlertBuilder alertBuilder) {
        if (markirovkaStagesExtractor == null) {
            throw new IllegalArgumentException("markirovkaStagesExtractor не может быть null!");
        }
        this.markirovkaStagesExtractor = markirovkaStagesExtractor;

        if (alertBuilder == null) {
            throw new IllegalArgumentException("alertBuilder не может быть null!");
        }
        this.alertBuilder = alertBuilder;
    }

    @Override
    public String composeAlert(Document doc, String documentUrl, Profile profile, List<UserProduct> userProducts, RiskMemo riskMemo) {
        // этапы читаем по ТОМУ ЖЕ Document, что использовал матч — второго запроса к сайту не будет,
        // и календарь гарантированно из той же версии страницы, что и совпадение по кодам
        List<String> stages = markirovkaStagesExtractor.extractStages(doc);
        // группа — свойство ТОВАРА, а не статьи; список непуст по договору с DozorReporter (см. выше)
        UserProduct userProduct = userProducts.getFirst();
        String group = userProduct.group();
        // по группе — все её обязанности сразу; текст КоАП внутри читается ОДИН раз на обе
        List<ObligationRisk> obligationRisks = riskMemo.risksFor(group, profile.subject());
        // субъект уходит дважды и по разным поводам: в риски — чтобы выбрать абзац санкции
        // («на должностных лиц»), в сборщик — чтобы назвать его человеку («для ИП»)
        return alertBuilder.glueMessage(userProducts, stages, obligationRisks, profile.subject(), documentUrl);
    }
}
