package md.bnm.converter.service;

public enum RateLoadErrorType {
    NETWORK_ERROR("Не удалось подключиться к серверу НБМ. Проверьте сетевое подключение."),
    TIMEOUT("Время ожидания ответа от сервера НБМ истекло."),
    SERVER_ERROR("Сервер НБМ вернул ошибку при запросе курсов валют."),
    EMPTY_RESPONSE("Курсы валют на указанную дату не опубликованы."),
    MALFORMED_RESPONSE("Ответ сервера НБМ имеет некорректный формат данных."),
    CACHE_UNAVAILABLE("Локальные сохранённые курсы валют недоступны."),
    CURRENCY_NOT_AVAILABLE("Выбранная валюта отсутствует в списке актуальных курсов.");

    private final String defaultMessage;

    RateLoadErrorType(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
