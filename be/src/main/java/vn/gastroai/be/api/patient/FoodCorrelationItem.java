package vn.gastroai.be.api.patient;

/**
 * UC0022 - 1 từ khoá món ăn và mức độ hay đi kèm triệu chứng của nó trong khoảng thời gian
 * phân tích. correlationPercent = timesWithSymptom / timesEaten * 100.
 */
public record FoodCorrelationItem(
        String foodKeyword,
        long timesEaten,
        long timesWithSymptom,
        double correlationPercent) {
}
