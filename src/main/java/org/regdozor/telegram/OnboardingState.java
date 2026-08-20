package org.regdozor.telegram;

import org.regdozor.profile.Subject;
import org.regdozor.profile.UserProduct;

import java.util.ArrayList;
import java.util.List;

/**
 * Память одного разговора: на каком шаге анкета и что человек уже успел ответить.
 *
 * 🚨 ОБЫЧНЫЙ ИЗМЕНЯЕМЫЙ КЛАСС, а не {@code record} — и это осознанное исключение из принятого в проекте
 * подхода. Record'ы ({@code UserProduct}, {@code ObligationRisk}, {@code RiskKey}) описывают ГОТОВЫЙ
 * ФАКТ и не меняются. Состояние разговора, наоборот, меняется на КАЖДОМ сообщении, а record изменить
 * нельзя: «сдвинуть шаг» означало бы пересоздавать объект, перечисляя все шесть значений заново — и это
 * в шести местах диалога, причём четыре значения подряд типа {@code String}, порядок которых компилятор
 * не проверяет. Слишком легко перепутать местами название товара и код.
 *
 * Правило границы: {@code record} — для факта, который не меняется; обычный класс — для того, что живёт
 * и меняется по ходу дела.
 *
 * ⚠️ {@code null} здесь ЗАКОННОЕ значение и означает «ещё не спрашивали». Поэтому проверок на null
 * в конструкторе нет: в начале разговора не известно ничего, кроме первого вопроса. Это отличается от
 * конструкторов помощников, где null — поломка сборки.
 *
 * ⚠️ ПОЧЕМУ ДВА КОНСТРУКТОРА. Пустой нужен ДЖЕКСОНУ: чтобы ПРОЧИТАТЬ объект из файла, ему надо его
 * СОЗДАТЬ, а он умеет либо через конструктор без аргументов (дальше расставит значения сеттерами),
 * либо через компоненты record'а. Проверено фактом: без пустого конструктора запись проходила, а чтение
 * падало с «no Creators, like default constructor, exist». Класс может быть записываемым, но НЕ читаемым —
 * пока не проверишь оба направления, не увидишь.
 *
 * ⚠️ Список товаров создаётся В ОБЪЯВЛЕНИИ ПОЛЯ, а не в конструкторе: Jackson зовёт ПУСТОЙ конструктор,
 * и инициализация внутри второго до него не дошла бы — список остался бы {@code null}, а первый же
 * {@code getUserProducts().add(...)} дал бы NullPointerException.
 *
 * ⚠️ Черновик ({@code draftName}, {@code draftTnved}) существует потому, что товар собирается ИЗ ТРЁХ
 * СООБЩЕНИЙ ПОДРЯД: название → ТН ВЭД → ОКПД2. Между ними бот обязан помнить предыдущие части, иначе
 * собрать {@link UserProduct} не из чего. ОКПД2 своего поля не требует — он приходит последним, и товар
 * тут же складывается целиком, после чего черновик очищается под следующий.
 */
public class OnboardingState {
    // где мы в анкете: он же отвечает на вопрос «что означает пришедший от человека текст»
    private OnboardingStep onboardingStep;
    // ответы, уже полученные (null = ещё не спрашивали)
    private Subject subject;
    private String group;
    // товары, собранные ЦЕЛИКОМ. Создаётся здесь, а не в конструкторе — см. предупреждение выше
    private List<UserProduct> userProducts = new ArrayList<>();
    // товар, который собирается прямо сейчас, по частям
    private String draftName;
    private String draftTnved;

    /** Пустой конструктор — для Jackson: без него прочитать состояние из файла невозможно. */
    public OnboardingState() {

    }

    /** Конструктор нового разговора: задаёт первый шаг, остальное заполнится ответами. */
    public OnboardingState(OnboardingStep onboardingStep) {
        this.onboardingStep = onboardingStep;
    }

    public OnboardingStep getOnboardingStep() {
        return onboardingStep;
    }

    public Subject getSubject() {
        return subject;
    }

    public String getGroup() {
        return group;
    }

    public List<UserProduct> getUserProducts() {
        return userProducts;
    }

    public String getDraftName() {
        return draftName;
    }

    public String getDraftTnved() {
        return draftTnved;
    }

    public void setOnboardingStep(OnboardingStep onboardingStep) {
        this.onboardingStep = onboardingStep;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setUserProducts(List<UserProduct> userProducts) {
        this.userProducts = userProducts;
    }

    public void setDraftName(String draftName) {
        this.draftName = draftName;
    }

    public void setDraftTnved(String draftTnved) {
        this.draftTnved = draftTnved;
    }
}
