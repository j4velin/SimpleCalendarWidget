package de.j4velin.calendarWidget.settings;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class PageAdapter extends FragmentPagerAdapter {

    public PageAdapter(final FragmentManager fm) {
        super(fm);
    }

    public static final WidgetSettingsFragment[] fragments = new WidgetSettingsFragment[3];

    @Override
    public Fragment getItem(int pos) {
        switch (pos) {
            default:
            case 0:
                fragments[0] = new Fragment_Events();
                return fragments[0];
            case 1:
                fragments[1] = new Fragment_Appearance();
                return fragments[1];
            case 2:
                fragments[2] = new Fragment_Settings();
                return fragments[2];
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    public CharSequence getPageTitle(int position) {
        return null;
    }

}
