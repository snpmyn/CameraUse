package com.qtone.camerause.util.edittext;

import android.text.InputType;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

/**
 * Created on 2021/8/27
 *
 * @author zsp
 * @desc EditText 工具类
 */
public class EditTextUtils {
    /**
     * 清 EditText 焦点
     *
     * @param view 焦点所在 View
     * @param ids  输入框
     */
    public static void clearViewFocus(View view, int... ids) {
        if ((null != view) && (null != ids)) {
            for (int id : ids) {
                if (view.getId() == id) {
                    view.clearFocus();
                    break;
                }
            }
        }
    }

    /**
     * 隐键盘
     *
     * @param view 焦点所在 View
     * @param ids  输入框
     * @return true 表焦点在 EditText
     */
    public static boolean isFocusEditText(View view, int... ids) {
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            for (int id : ids) {
                if (editText.getId() == id) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 显隐
     *
     * @param editText 输入框
     * @return true 显、false 隐
     */
    @NonNull
    public static Boolean showOrHide(@NonNull EditText editText) {
        boolean flag;
        if (editText.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            flag = false;
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            flag = true;
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
        return flag;
    }
}