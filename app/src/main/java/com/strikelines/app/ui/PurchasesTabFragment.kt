package com.strikelines.app.ui

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.strikelines.app.OsmandHelper.OsmandHelperListener
import com.strikelines.app.R
import com.strikelines.app.ui.adapters.LockableViewPager

class PurchasesTabFragment : Fragment(), OsmandHelperListener {

	private lateinit var tabLayout: TabLayout
	private lateinit var viewPager: LockableViewPager
	private lateinit var pagerAdapter: PurchasesFragmentPagerAdapter

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {

		val view = inflater.inflate(R.layout.purchases_tab_fragment, container, false)

		viewPager = view.findViewById(R.id.pager) as LockableViewPager
		viewPager.offscreenPageLimit = 3
		pagerAdapter = PurchasesFragmentPagerAdapter(childFragmentManager)
		viewPager.adapter = pagerAdapter

		tabLayout = view.findViewById(R.id.tab_layout) as TabLayout
		tabLayout.setupWithViewPager(viewPager)
		viewPager.addOnPageChangeListener(
			object : ViewPager.OnPageChangeListener {
				override fun onPageScrolled(
					position: Int, positionOffset: Float,
					positionOffsetPixels: Int
				) {
				}

				override fun onPageSelected(position: Int) {}

				override fun onPageScrollStateChanged(state: Int) {}
			}
		)

		return view
	}

	inner class PurchasesFragmentPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

		private val fragments = listOf(
                PurchaseGpxFilesFragment(),
                Purchase3DChartsFragment(),
                PurchaseBasemapsFragment()
		)

		private val titleIds = intArrayOf(
			PurchaseGpxFilesFragment.TITLE,
			Purchase3DChartsFragment.TITLE,
			PurchaseBasemapsFragment.TITLE
		)

		override fun getItem(position: Int) = fragments[position]

		override fun getCount() = fragments.size

		override fun getPageTitle(position: Int): CharSequence? {
			return this@PurchasesTabFragment.context?.getString(titleIds[position])
		}
	}

	override fun onOsmandConnectionStateChanged(connected: Boolean) {

	}
}