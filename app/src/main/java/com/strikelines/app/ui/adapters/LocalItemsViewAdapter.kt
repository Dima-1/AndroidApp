package com.strikelines.app.ui.adapters

import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes
import android.support.v7.widget.AppCompatCheckBox
import android.support.v7.widget.AppCompatRadioButton
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import com.strikelines.app.R

class LocalItemsViewAdapter : RecyclerView.Adapter<LocalItemsViewAdapter.BaseViewHolder>() {

	companion object {
		private const val ITEM_VIEW_TYPE_HEADER = 0
		private const val ITEM_VIEW_TYPE_GPX = 1
		private const val ITEM_VIEW_TYPE_3D_CHART = 2
		private const val ITEM_VIEW_TYPE_BASEMAP = 3
	}

	interface OnCheckedListener {
		fun onCheckedChanged(listItem: ListItem, isChecked: Boolean)
	}

	var listener: OnCheckedListener? = null

	var items: List<ListItem> = emptyList()
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	override fun getItemViewType(position: Int): Int {
		return when (items[position]) {
			is LocalGpxItem -> ITEM_VIEW_TYPE_GPX
			is Local3DChartItem -> ITEM_VIEW_TYPE_3D_CHART
			is LocalBasemapItem -> ITEM_VIEW_TYPE_BASEMAP
			else -> ITEM_VIEW_TYPE_HEADER
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		return when (viewType) {
			ITEM_VIEW_TYPE_GPX -> LocalGpxViewHolder(
				inflater.inflate(R.layout.local_file_list_item, parent, false)
			)
			ITEM_VIEW_TYPE_3D_CHART -> Local3DChartViewHolder(
				inflater.inflate(R.layout.local_file_list_item, parent, false)
			)
			ITEM_VIEW_TYPE_BASEMAP -> LocalBasemapViewHolder(
				inflater.inflate(R.layout.local_file_list_item, parent, false)
			)
			else -> HeaderViewHolder(
				inflater.inflate(R.layout.local_header_list_item, parent, false)
			)
		}
	}

	override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
		var firstItem = position == 0
		var lastItem = position == itemCount - 1
		val item = items[position]
		val isHeader = getItemViewType(position) == ITEM_VIEW_TYPE_HEADER;
		if (!lastItem) {
			if (getItemViewType(position + 1) == ITEM_VIEW_TYPE_HEADER) {
				lastItem = true
			}
		}

		if (isHeader) holder.shadowTop?.visibility = View.VISIBLE else holder.shadowTop?.visibility = View.GONE
		if (lastItem) holder.shadowBottom?.visibility = View.VISIBLE else holder.shadowBottom?.visibility = View.GONE
		if (lastItem) holder.divider?.visibility = View.GONE else holder.divider?.visibility = View.VISIBLE

		when {
			item.imageId != 0 -> holder.image?.setImageResource(item.imageId)
			item.imageDrawable != null -> holder.image?.setImageDrawable(item.imageDrawable)
			else -> holder.image?.visibility = View.GONE
		}
		if (item.imageTintColor != 0) {
			holder.image?.setColorFilter(item.imageTintColor, android.graphics.PorterDuff.Mode.SRC_IN)
		}

		holder.title?.text = item.title
		if (item.description == null) {
			holder.description?.visibility = View.GONE
		} else {
			holder.description?.text = item.description
		}

		if (getItemViewType(position) != ITEM_VIEW_TYPE_HEADER) {
			holder.infoContainer?.setOnClickListener {
				val button: CompoundButton? = if (item.multiselection) holder.checkbox else holder.radioButton
				button?.toggle()
			}
			if (item.multiselection) {
				holder.checkbox?.visibility = View.VISIBLE
				holder.radioButton?.visibility = View.GONE
				holder.checkbox?.tag = position
				holder.checkbox?.setOnCheckedChangeListener(null)
				holder.checkbox?.isChecked = item.selected
				holder.checkbox?.setOnCheckedChangeListener(onCheckedListener)
			} else {
				holder.radioButton?.tag = position
				holder.radioButton?.setOnCheckedChangeListener(null)
				holder.radioButton?.isChecked = item.selected
				holder.radioButton?.setOnCheckedChangeListener(onCheckedListener)
				holder.checkbox?.visibility = View.GONE
				holder.radioButton?.visibility = View.VISIBLE
			}
		}
	}

	override fun getItemCount() = items.size

	private val onCheckedListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
		val position = buttonView.tag
		if (position != null) {
			val item = items[position as Int]
			listener?.onCheckedChanged(item, isChecked)
		}
	}

	abstract inner class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val shadowTop: View? = view.findViewById(R.id.shadow_top)
		val shadowBottom: View? = view.findViewById(R.id.shadow_bottom)
		val infoContainer: View? = view.findViewById(R.id.info_container)
		val image: ImageView? = view.findViewById(R.id.image)
		val title: TextView? = view.findViewById(R.id.title)
		val description: TextView? = view.findViewById(R.id.description)
		val checkbox: AppCompatCheckBox? = view.findViewById(R.id.checkbox)
		val radioButton: AppCompatRadioButton? = view.findViewById(R.id.radio_button)
		val divider: View? = view.findViewById(R.id.divider)
	}

	inner class LocalGpxViewHolder(view: View) : BaseViewHolder(view)

	inner class Local3DChartViewHolder(view: View) : BaseViewHolder(view)

	inner class LocalBasemapViewHolder(view: View) : BaseViewHolder(view)

	inner class HeaderViewHolder(view: View) : BaseViewHolder(view)

	abstract class ListItem {
		@DrawableRes
		var imageId: Int = 0
			internal set
		@ColorInt
		var imageTintColor: Int = 0
			internal set
		var imageDrawable: Drawable? = null
			internal set
		var title: CharSequence = ""
			internal set
		var description: CharSequence? = null
			internal set
		var selected: Boolean = false
			internal set
		var multiselection: Boolean = false
			internal set
		var data: Any? = null
			internal set
	}

	class LocalGpxItem : ListItem()

	class Local3DChartItem : ListItem()

	class LocalBasemapItem : ListItem()

	class HeaderItem : ListItem()
}