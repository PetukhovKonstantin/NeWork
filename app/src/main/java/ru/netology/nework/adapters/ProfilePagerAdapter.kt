package ru.netology.nework.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.netology.nework.activities.EventsFragment
import ru.netology.nework.activities.FeedFragment
import ru.netology.nework.activities.JobsFragment

class ProfilePagerAdapter(
    fragmentActivity: FragmentActivity,
    private val userId: Long
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        val fromPager = true
        return when (position) {
            0 -> FeedFragment.newInstance(fromPager, userId)
            1 -> EventsFragment.newInstance(fromPager, userId)
            2 -> JobsFragment.newInstance(fromPager, userId)
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}