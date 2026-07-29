package org.regdozor.pravo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.regdozor.net.HttpTextFetcher;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Достаёт текст правового акта из ЭБПИ по хешу документа.
 *
 * ЭБПИ — внутренний сервис портала pravo.gov.ru, который хранит тексты актов В ДЕЙСТВУЮЩЕЙ
 * РЕДАКЦИИ, то есть со всеми вклеенными поправками. Это ЕДИНСТВЕННЫЙ источник машиночитаемого
 * текста первого яруса: официальный API публикации (publication.pravo.gov.ru) отдаёт метаданные
 * и ссылку на СКАН, а PDF — картинка, из которой текст без OCR не достать (OCR отвергнут:
 * путает цифры кодов). На этой читалке стоят ОБА слоя дозора — код-матч по тексту постановления
 * и вывод риска из КоАП.
 *
 * РАБОТАЕТ В ДВА ЗАПРОСА, потому что спросить «дай текст КоАП» нельзя — надо указать, КАКОЙ редакции:
 * 1) redactions — по хешу документа отдаёт список ВСЕХ редакций; берём ту, где actual == true;
 * 2) redtext — по номеру этой редакции (redid) отдаёт сам текст.
 * Наружу торчит только {@link #fetchText(String)}: снаружи нужен текст, а про существование
 * redid внешнему коду знать незачем.
 *
 * ⚠️ API НЕДОКУМЕНТИРОВАН (порт 8000). Адреса и параметры выведены чтением JS самого сайта
 * и проверены живыми запросами. Контракта нет — могут поменять без предупреждения. Это осознанный
 * размен: другого источника текста первого яруса не существует. Отсюда же требование сторожа
 * («канарейки»): он должен заметить, что источник сломался, и сказать РАЗРАБОТЧИКУ, не клиенту.
 *
 * ⚠️ redid — НЕ КОНСТАНТА: он меняется с каждой поправкой акта. Хеш документа стабилен, redid — нет.
 * Поэтому его ищут заново на каждый вызов, а не зашивают в код.
 */
public class PravoEbpiTextFetcher {
    /**
     * Базовый адрес API ЭБПИ. Вынесен в константу: когда сервис переедет (а он недокументирован,
     * значит переедет), править придётся одно место, а не два метода.
     */
    private static final String EBPI_BASE = "http://actual.pravo.gov.ru:8000/api/ebpi/";

    private final HttpTextFetcher httpTextFetcher;
    private final ObjectMapper objectMapper;

    public PravoEbpiTextFetcher(HttpTextFetcher httpTextFetcher, ObjectMapper objectMapper) {
        if (httpTextFetcher == null) {
            throw new IllegalArgumentException("httpTextFetcher не может быть null!");
        }
        this.httpTextFetcher = httpTextFetcher;

        if (objectMapper == null) {
            throw new IllegalArgumentException("objectMapper не может быть null!");
        }
        this.objectMapper = objectMapper;
    }

    /**
     * Находит номер действующей редакции документа.
     *
     * Первый из двух запросов. Сервер отдаёт ВСЕ редакции: прошлые («недействующая»), действующую
     * и даже БУДУЩИЕ — например, у КоАП висит редакция с reddate 20260901: это правка ст. 15.12¹,
     * вступающая в силу 01.09.2026. Нам нужна та, у которой actual == true.
     *
     * ⚠️ Сервер отвечает HTTP 200 ДАЖЕ ПРИ ОШИБКЕ — провал виден только в поле error, код ответа обманет.
     * ⚠️ Поле redactions может прийти null, а НЕ пустым списком — отсюда проверка перед циклом
     *    (цикл по null падает с NullPointerException).
     * ⚠️ actual объявлен обёрткой Boolean, поэтому сравнение идёт через Boolean.TRUE.equals(...):
     *    обычное if (r.actual()) распаковало бы обёртку и упало на null.
     *
     * @param hash отпечаток документа в ЭБПИ
     * @return redid действующей редакции
     */
    private long findActualRedid(String hash) {
        RedactionsResponse redactionsResponse;
        // ttl=1 — режим выдачи для кодексов. ⚠️ Значение подобрано ЧТЕНИЕМ JS сайта и проверено
        // живым запросом; что параметр означает на самом деле — НЕИЗВЕСТНО.
        String json  = """
        {"hash":"%s","ttl":1}""".formatted(hash);
        // JSON нельзя класть в адрес как есть: {, ", : и } там недопустимы, URI.create() подавится.
        String encoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
        String url = EBPI_BASE + "redactions/?bpa=ebpi&t=" + encoded;
        String responseJson = httpTextFetcher.fetch(url);

        try {
            redactionsResponse = objectMapper.readValue(responseJson, RedactionsResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse PravoEbpiTextFetcher redactions", e);
        }

        if (redactionsResponse.error() != null && !redactionsResponse.error().isBlank()) {
            throw new IllegalStateException("RedactionsResponse error: " +
                    redactionsResponse.error() + " url=" + url);
        }

        if (redactionsResponse.redactions() == null) {
            throw new IllegalStateException("Сервер не вернул список редакций");
        }

        for (Redaction r : redactionsResponse.redactions()) {
            if (Boolean.TRUE.equals(r.actual())) {
                return r.redid();
            }
        }

        throw new IllegalStateException("Сервер ответил нормально, но действующей редакции в списке нет " + hash);
    }

    /**
     * По номеру редакции отдаёт её текст.
     *
     * Второй из двух запросов, и он ПРОЩЕ первого: параметр t здесь — просто число, а не JSON,
     * поэтому ни собирать, ни кодировать нечего.
     *
     * ⚠️ Смотри на адрес: у redtext НЕТ косой черты перед «?», в отличие от redactions/? —
     *    это непоследовательность их API, а не опечатка.
     * ⚠️ Ответ большой: текст КоАП — около 4 млн символов (~3.4 сек). Качалка (HttpTextFetcher)
     *    даёт на ответ 20 секунд; если упрёшься в таймаут — причина там, а не здесь.
     *
     * @param redid номер редакции (из {@link #findActualRedid(String)})
     * @return текст акта в HTML
     */
    private String fetchRedText(long redid) {
        RedTextResponse redTextResponse;
        String url = EBPI_BASE + "redtext?bpa=ebpi&t=" + redid + "&ttl=1";
        String responseJson = httpTextFetcher.fetch(url);

        try {
            redTextResponse = objectMapper.readValue(responseJson, RedTextResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Не удалось разобрать ответ redtext", e);
        }

        if (redTextResponse.redtext() == null || redTextResponse.redtext().isBlank()) {
            throw new IllegalStateException("Сервер вернул пустой текст " + url + " " + redid);
        }

        return redTextResponse.redtext();
    }

    /**
     * По хешу документа отдаёт полный текст его ДЕЙСТВУЮЩЕЙ редакции, проверив канарейкой, что это
     * НАСТОЯЩИЙ акт, а не заглушка.
     *
     * Единственная дверь наружу: внутри делает два запроса — сперва ищет номер действующей
     * редакции, потом тянет по нему текст.
     *
     * ⚠️ КАНАРЕЙКА. Когда новая редакция ещё не отрисована, ЭБПИ отдаёт HTTP 200 с {@code error=null}
     * и непустым телом «Подготовка текста редакции» — прежние проверки (error / пустота) её ПРОПУСКАЮТ.
     * Поэтому в конце проверяем ПОЛОЖИТЕЛЬНЫЙ факт: в тексте есть {@code expectedMarker} (кусок офиц.
     * названия акта). Нет — падаем у двери, а не отдаём мусор в локатор. Проверено фактом 2026-07-29:
     * действующая редакция сменилась (494236 → 495398), текст был не готов — канарейка поймала.
     *
     * @param hash           отпечаток документа в ЭБПИ. Берётся со страницы pravo.gov.ru/codex/ — там
     *                       у каждого кодекса ссылка вида #hash=…&ttl=1. У КоАП это
     *                       6639c6c6580e8aa0bdf84170d25823669dfb6a4144b03da245ef4889f24765c0
     * @param expectedMarker строка, которая ОБЯЗАНА быть в настоящем тексте акта. Для КоАП —
     *                       «административных правонарушениях» (часть офиц. названия; в заглушке её нет)
     * @return текст акта в HTML (у КоАП — около 4 млн символов)
     * @throws IllegalStateException если сервер вернул ошибку в поле error, не дал список редакций,
     *                               не имеет действующей редакции, прислал пустой текст ИЛИ текст без
     *                               ожидаемого маркера (редакция не готова / источник изменился)
     * @throws RuntimeException      если ответ не разобрался как JSON или упала сеть
     */
    public String fetchText(String hash, String expectedMarker) {
        long redid =  findActualRedid(hash);
        String text = fetchRedText(redid);

        if (!text.contains(expectedMarker)) {
            throw new IllegalStateException("Текст без ожидаемого маркера \"" + expectedMarker +
                    "\" — редакция не готова или источник изменился. hash=" + hash + " redid=" + redid);
        }

        return text;
    }
}
