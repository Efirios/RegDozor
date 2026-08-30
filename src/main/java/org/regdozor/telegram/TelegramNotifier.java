package org.regdozor.telegram;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * «Оповещатель»: отправляет готовый текст в ОДИН чат Telegram через Bot API (метод sendMessage).
 * botToken — СЕКРЕТ, приходит снаружи (App читает его из переменной окружения), в коде его нет.
 * Класс знает только «КАК отправить одному названному чату»; «КОМУ рассылать» решает Broadcaster.
 */
public class TelegramNotifier {
    /** Токен бота — секрет. Вместе с ним формируется URL запроса, поэтому URL нельзя писать в логи. */
    private final String botToken;
    /** Переиспользуемый HTTP-клиент (один на всё время жизни объекта). */
    private final HttpClient client;

    public TelegramNotifier(String botToken) {
        if (botToken == null || botToken.isBlank()){
            throw new IllegalStateException("не задана переменная окружения botToken");
        }
        this.botToken = botToken;

        this.client = HttpClient.newBuilder().
                version(HttpClient.Version.HTTP_1_1).
                followRedirects(HttpClient.Redirect.ALWAYS).
                connectTimeout(Duration.ofSeconds(10)).
                build();
    }

    /**
     * Отправляет сообщение в указанный чат. Текст может содержать HTML-разметку Telegram (теги &lt;b&gt;, &lt;a&gt;…).
     *
     * @param chatId идентификатор чата-получателя
     * @param text   готовый текст сообщения (напр. собранный AlertFormatter)
     */
    public void send(String chatId, String text) {
        send(chatId, text, List.of());
    }

    /**
     * Отправляет сообщение и показывает под ним КЛАВИАТУРУ СНИЗУ — набор кнопок вместо обычной
     * клавиатуры собеседника.
     *
     * Здесь живёт вся логика отправки; короткий {@link #send(String, String)} — её частный случай
     * с пустым списком. Перегрузка, а не два разных метода: операция одна, различается лишь наличие
     * кнопок, и четыре существующих места вызова остались нетронутыми.
     *
     * ⚠️ Кнопки — ИМЕННО «клавиатура снизу» ({@code keyboard}), а НЕ инлайновые под сообщением.
     * Решающий довод: нажатие такой кнопки приходит боту ОБЫЧНЫМ ТЕКСТОМ, и разбирается тем же путём,
     * что и напечатанный ответ. Инлайновые прислали бы {@code callback_query} — другой тип обновления,
     * ради которого пришлось бы завести второй путь разбора и обязательный {@code answerCallbackQuery}.
     * А свободный текст (название товара, коды) читать всё равно нужно.
     *
     * ⚠️ ПУСТОЙ СПИСОК = «УБРАТЬ КЛАВИАТУРУ» ({@code remove_keyboard}), а не «оставить как есть».
     * Умолчание работает на нас: любое обычное сообщение попутно подчищает кнопки предыдущего вопроса,
     * иначе «Добавить ещё товар?» висело бы у человека внизу навсегда.
     *
     * ⚠️ Двойные скобки {@code [[…]]} обязательны: внешние — массив РЯДОВ, внутренние — один ряд.
     * ⚠️ {@code String.join} ставит разделитель только МЕЖДУ элементами, поэтому крайние кавычки
     * дописываются вручную — иначе первая и последняя надписи остались бы без кавычек, и json был бы битым.
     * ⚠️ Кодируется json ОДИН раз, при вставке в url; в переменной лежит чистый текст.
     *
     * Проверено разбором через Jackson: пустой список, одна, две и три кнопки — во всех случаях
     * валидный json, кириллица цела.
     *
     * @param chatId  идентификатор чата-получателя
     * @param text    готовый текст сообщения (может содержать HTML-разметку Telegram)
     * @param buttons надписи кнопок в ОДИН ряд; пустой список убирает клавиатуру
     */
    public void send(String chatId, String text, List<String> buttons) {
        String linkPreview = URLEncoder.encode("{\"is_disabled\":true}", StandardCharsets.UTF_8);
        String keyboard;

        if (buttons.isEmpty()) {
            keyboard = "{\"remove_keyboard\":true}";
        } else {
            keyboard = "{\"keyboard\":[[\"" + String.join("\",\"", buttons) + "\"]],\"one_time_keyboard\":true," +
                    "\"resize_keyboard\":true}";
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" +
                URLEncoder.encode(text, StandardCharsets.UTF_8) + "&parse_mode=HTML" + "&link_preview_options=" +
                linkPreview + "&reply_markup=" + URLEncoder.encode(keyboard, StandardCharsets.UTF_8);

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(20)).build();

        try {
            HttpResponse<String> httpResponse = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (httpResponse.statusCode() != 200) {
                throw new IllegalStateException("Telegram sendMessage failed: status=" + httpResponse.statusCode() +
                        " body=" + httpResponse.body());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to send Telegram message", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}
