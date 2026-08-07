package org.regdozor.match;

import org.regdozor.pravo.PravoEbpiTextFetcher;
import org.regdozor.profile.Subject;

import java.util.Map;

/**
 * Дирижёр риска: по (группа, обязанность, субъект) даёт готовую ЦИТАТУ штрафа из КоАП.
 *
 * Связывает три куска — таблицу «(группа, обязанность) → статья+часть» ({@link ObligationTableLoader}),
 * локатор ({@link KoapArticleLocator}) и извлекатель ({@link KoapPenaltyExtractor}) — плюс читалку текста
 * КоАП ({@link org.regdozor.pravo.PravoEbpiTextFetcher}). Сам ничего не парсит: держит четырёх помощников
 * и порядок вызовов.
 *
 * Место в цепочке: [профиль: субъект] + [markirovka: обязанность] → ЭТОТ класс → сборка алерта.
 * Хеш и маркер КоАП — константы: читалка по хешу находит документ, по маркеру канарейка проверяет,
 * что пришёл настоящий кодекс, а не заглушка.
 */
public class KoapRisk {
    // таблица «(группа, обязанность) → статья+часть»: по паре узнаём, в какую статью КоАП смотреть
    private final Map<ObligationKey, ObligationArticle> table;
    // читалка: по хешу достаёт текст акта из ЭБПИ (с канарейкой по маркеру)
    private final PravoEbpiTextFetcher pravoEbpiTextFetcher;
    // локатор: из текста КоАП вырезает нужную статью по номеру
    private final KoapArticleLocator koapArticleLocator;
    // извлекатель: из статьи достаёт цитату штрафа для субъекта
    private final KoapPenaltyExtractor koapPenaltyExtractor;
    // отпечаток КоАП в ЭБПИ (с pravo.gov.ru/codex/) — по нему читалка находит документ
    private static final String KOAP_HASH = "6639c6c6580e8aa0bdf84170d25823669dfb6a4144b03da245ef4889f24765c0";
    // кусок офиц. названия КоАП — канарейка проверяет, что пришёл настоящий кодекс, а не заглушка
    private static final String KOAP_MARKER = "административных правонарушениях";

    // Помощников создают СНАРУЖИ и передают сюда (dependency injection). Каждый обязателен — null не пускаем.
    public KoapRisk(Map<ObligationKey, ObligationArticle> table, PravoEbpiTextFetcher pravoEbpiTextFetcher,
                    KoapArticleLocator koapArticleLocator, KoapPenaltyExtractor koapPenaltyExtractor) {
        // таблица: без неё поиск статьи невозможен
        if (table == null) {
            throw new IllegalArgumentException("table не может быть null");
        }
        this.table = table;   // кладём в поле

        // читалка текста КоАП
        if (pravoEbpiTextFetcher == null) {
            throw new IllegalArgumentException("pravoEbpiTextFetcher не может быть null");
        }
        this.pravoEbpiTextFetcher = pravoEbpiTextFetcher;   // кладём в поле

        // локатор статьи
        if (koapArticleLocator == null) {
            throw new IllegalArgumentException("koapArticleLocator не может быть null");
        }
        this.koapArticleLocator = koapArticleLocator;   // кладём в поле

        // извлекатель штрафа
        if (koapPenaltyExtractor == null) {
            throw new IllegalArgumentException("koapPenaltyExtractor не может быть null");
        }
        this.koapPenaltyExtractor = koapPenaltyExtractor;   // кладём в поле
    }

    /**
     * По (группа, обязанность, субъект) отдаёт цитату штрафа: таблица → текст КоАП → статья → абзац субъекта.
     *
     * @param group      товарная группа ("одежда")
     * @param obligation обязанность ("нанесение" / "ГИС МТ")
     * @param subject    правовая форма клиента ({@link Subject#IP} / {@link Subject#LEGAL}) — берётся из профиля.
     *                   Типом, а не строкой: цена ошибки тут — штраф, отличающийся в 30 раз, и опечатку
     *                   в строке компилятор бы не поймал. Формулировку КоАП («на должностных лиц») достаём
     *                   у него сами — извлекатель про профили не знает и принимает голую строку
     * @return цитата штрафа из живой редакции КоАП (суммы словами)
     * @throws IllegalStateException если пары нет в таблице; либо ниже по цепочке (текст не готов —
     *                               канарейка читалки, статья или абзац субъекта не найдены)
     */
    public String riskFor(String group, String obligation, Subject subject) {
        // 1) таблица: по паре (группа, обязанность) достаём запись — куда (статья+часть) смотреть
        ObligationArticle row = table.get(new ObligationKey(group, obligation));
        // пары в таблице нет → не наша обязанность / таблицу не пополнили; не пускаем null дальше
        if (row == null) {
            throw new IllegalStateException ("Нет статьи для пары: " + group + " / " + obligation);
        }

        // 2) читалка: весь текст КоАП (канарейка внутри — если редакция не готова, упадёт здесь)
        String koapHtml = pravoEbpiTextFetcher.fetchText(KOAP_HASH, KOAP_MARKER);
        // 3) локатор: из текста КоАП вырезаем нужную статью по номеру+надстрочнику из записи
        String articleHtml = koapArticleLocator.locateArticle(koapHtml, row.baseNumber(), row.superscript());
        // 4) извлекатель: из статьи берём цитату штрафа для нужной части и субъекта
        String koapPenalty = koapPenaltyExtractor.penaltyFor(articleHtml, row.part(), subject.getKoapWording());
        return koapPenalty;   // отдаём цитату наружу
    }
}
