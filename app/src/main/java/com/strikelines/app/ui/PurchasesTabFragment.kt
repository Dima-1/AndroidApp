package com.strikelines.app.ui

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.strikelines.app.OsmandHelper.OsmandHelperListener
import com.strikelines.app.R
import com.strikelines.app.ui.adapters.LockableViewPager

class PurchasesTabFragment : Fragment(), OsmandHelperListener {

	private lateinit var tabLayout: TabLayout
	private lateinit var viewPager: LockableViewPager
	private lateinit var pagerAdapter: PurchasesFragmentPagerAdapter
	private lateinit var regionSpinner: Spinner

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {

		val view = inflater.inflate(R.layout.purchases_tab_fragment, container, false)

		regionSpinner = view.findViewById(R.id.regionSpinner) as Spinner
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

	override fun onResume() {
		super.onResume()
		setupRegionSpinner()

	}

	private fun setupRegionSpinner() {
		val spinnerAdapter = ArrayAdapter(
			activity!!, R.layout.spinner_item,
			((activity!! as MainActivity).regionList).toTypedArray()
		)

		spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
		regionSpinner.adapter = spinnerAdapter
		regionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onNothingSelected(p0: AdapterView<*>?) {}

			override fun onItemSelected(
				parent: AdapterView<*>?,
				view: View?,
				position: Int,
				id: Long
			) {
				regionSpinner.setSelection(position)
				Log.i("selectedItem", parent?.getItemAtPosition(position).toString())
				(activity!! as MainActivity).regionToFilter =
						if (parent?.getItemAtPosition(position).toString()!= activity!!.resources.getString(R.string.all_regions))
							parent?.getItemAtPosition(position).toString()
						else ""
				(activity!! as MainActivity).notifyFragmentsOnDataChange()
			}
		}
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