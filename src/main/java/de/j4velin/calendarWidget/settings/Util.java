package de.j4velin.calendarWidget.settings;

import android.os.Build;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.List;

public class Util {
    /**
     * messed up shit we need to do to get the checkboxed to be actually checked and not just
     * "outlined" on material theme for this fragment
     */
    public static void updateCOmpoundButtonsView(List<CompoundButton> compoundButtons) {
        if (Build.VERSION.SDK_INT >= 21) {
            for (CompoundButton cb : compoundButtons) {
                if (cb.isChecked()) {
                    if (cb instanceof RadioButton) {
                        RadioGroup rg = (RadioGroup) cb.getParent();
                        rg.clearCheck();
                        rg.check(cb.getId());
                    } else {
                        cb.setChecked(false);
                        cb.setChecked(true);
                    }
                }
            }
            compoundButtons.clear();
        }
    }
}
