package com.qtone.camerause.util.data;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Created on 2019/3/11.
 *
 * @author 郑少鹏
 * @desc BigDecimalUtils
 */
public class BigDecimalUtils {
    private static final String EXPR_PATTERN = "0.##########E0";
    private static final String PATTERN = "0.##########";
    private static final String INTEGER_MIN_VALUE_CHANGE_TO_EXPR = "10000000";
    private static final String DECIMAL_MIN_VALUE_CHANGE_TO_EXPR = "0.0001";

    /**
     * Judging number is able to convert to expr display or not.
     *
     * @param bigDecimal BigDecimal
     * @return boolean
     */
    private static boolean bigDecimalCanConvertToString(BigDecimal bigDecimal) {
        if (null == bigDecimal) {
            return false;
        }
        boolean result = false;
        BigDecimal bigDecimalAbs = bigDecimal.abs();
        if ((bigDecimalAbs.compareTo(new BigDecimal(DECIMAL_MIN_VALUE_CHANGE_TO_EXPR)) <= 0) || (bigDecimalAbs.compareTo(new BigDecimal(INTEGER_MIN_VALUE_CHANGE_TO_EXPR)) >= 0)) {
            result = true;
        }
        if (bigDecimalAbs.compareTo(new BigDecimal(0)) == 0) {
            result = false;
        }
        return result;
    }

    /**
     * BigDecimal to String.
     *
     * @param bigDecimal BigDecimal
     * @return String
     */
    public static String bigDecimalToString(BigDecimal bigDecimal) {
        if (null == bigDecimal) {
            return null;
        }
        DecimalFormat decimalFormat = new DecimalFormat();
        if (bigDecimalCanConvertToString(bigDecimal)) {
            decimalFormat.applyPattern(EXPR_PATTERN);
        } else {
            decimalFormat.applyPattern(PATTERN);
        }
        return decimalFormat.format(bigDecimal);
    }

    /**
     * 加
     *
     * @param bigDecimalOne BigDecimal
     * @param bigDecimalTwo BigDecimal
     * @return 结果
     */
    public static BigDecimal add(@NotNull BigDecimal bigDecimalOne, BigDecimal bigDecimalTwo) {
        return bigDecimalOne.add(bigDecimalTwo, new MathContext(8, RoundingMode.HALF_UP));
    }
}