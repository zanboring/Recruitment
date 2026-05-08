package com.example.recruitment.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SalaryUtil {

    private static final Pattern SALARY_PATTERN = Pattern.compile(
        "(\\d+(?:\\.\\d+)?)\\s*([万千kK])?\\s*[-~到]\\s*(\\d+(?:\\.\\d+)?)\\s*([万千kK])?"
    );

    public static class SalaryRange {
        private BigDecimal min;
        private BigDecimal max;

        public BigDecimal getMin() {
            return min;
        }

        public void setMin(BigDecimal min) {
            this.min = min;
        }

        public BigDecimal getMax() {
            return max;
        }

        public void setMax(BigDecimal max) {
            this.max = max;
        }
    }

    public static SalaryRange parse(String salaryText) {
        SalaryRange range = new SalaryRange();
        if (salaryText == null || salaryText.isBlank() || salaryText.contains("面议")) {
            return range;
        }
        
        Matcher matcher = SALARY_PATTERN.matcher(salaryText);
        if (matcher.find()) {
            double minValue = parseNumericValue(matcher.group(1), matcher.group(2));
            double maxValue = parseNumericValue(matcher.group(3), matcher.group(4));
            range.setMin(BigDecimal.valueOf(minValue * 1000));
            range.setMax(BigDecimal.valueOf(maxValue * 1000));
        }
        return range;
    }

    private static double parseNumericValue(String numStr, String unitStr) {
        double value = Double.parseDouble(numStr);
        if (unitStr == null) return value;
        switch (unitStr.toLowerCase()) {
            case "万": return value * 10000;
            case "千": case "k": return value * 1000;
            default: return value;
        }
    }
}