package com.example.zimarix_1.ui.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.zimarix_1.R


private val SETTINGS_TAB_TITLES = arrayOf(
    R.string.Stab_text_1,
    R.string.Stab_text_2,
    R.string.Stab_text_3
)

class SettingsPagerAdapter (private val context: Context, fm: FragmentManager):
    FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return SettingsFragment.newInstance(position + 1)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return context.resources.getString(SETTINGS_TAB_TITLES[position])
        }

        override fun getCount(): Int {
            // Show 2 total pages.
            return 3
        }
}