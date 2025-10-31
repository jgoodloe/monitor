package com.monitor.ui.settings

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ConfigPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ConfigListFragment.newInstance(ConfigListFragment.ConfigType.URL)
            1 -> ConfigListFragment.newInstance(ConfigListFragment.ConfigType.DNS)
            2 -> ConfigListFragment.newInstance(ConfigListFragment.ConfigType.CRL)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}




