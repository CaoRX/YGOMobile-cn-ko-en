package cn.garymb.ygomobile.ex_card;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.google.android.material.tabs.TabLayout;

import cn.garymb.ygomobile.lite.R;

/**
 * ���п����Fragment��Tab��������������ʵ��ҳ���л�
 */
public class ExPackageTabAdapter extends FragmentStatePagerAdapter {
    TabLayout tabLayout;
    /* �����ڻ�ȡstrings.xml�е��ַ�����It's used just for getting strings from strings.xml */
    Context context;

    public ExPackageTabAdapter(FragmentManager fm, TabLayout _tabLayout, Context context) {
        super(fm);
        this.tabLayout = _tabLayout;
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        if (position == 0) {
            fragment = new ExCardListFragment();
        } else if (position == 1) {
            fragment = new ExCardLogFragment();
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title = null;
        if (position == 0) {

            title = context.getString(R.string.ex_card_list_title);
        } else if (position == 1) {
            title = context.getString(R.string.ex_card_log_title);
        }
        return title;
    }
}
