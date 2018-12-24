package com.strikelines.app.ui.adapters

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.strikelines.app.R
import com.strikelines.app.R.id.downloadTV
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.domain.GlideApp
import com.strikelines.app.domain.models.Chart
import com.strikelines.app.utils.UiUtils
import com.strikelines.app.utils.clearTitleForWrecks
import com.strikelines.app.utils.descriptionFilter


class ShopAdapter(val listener: ShopListener?) : RecyclerView.Adapter<ShopItemViewHolder>() {

    private val dataList = mutableListOf<Chart>()

    fun setData(list: List<Chart>) {
        if(list.isNotEmpty()) {
            val diffUtil= DiffUtil.calculateDiff(ChartDiff(dataList, list.toMutableList()))
            dataList.clear()
            dataList.addAll(list)
            diffUtil.dispatchUpdatesTo(this)
        } else {
            dataList.clear()
            notifyDataSetChanged()
        }

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ShopItemViewHolder(
            LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_shopchart, parent, false)
    )

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ShopItemViewHolder, position: Int) {
        holder.bind(dataList[position], listener)

    }

    inner class ChartDiff(
            private val oldList: MutableList<Chart>,
            private val newList: MutableList<Chart>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition].name == newList[newItemPosition].name


        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition] == newList[newItemPosition]
    }
}

class ShopItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val imageView: ImageView = itemView.findViewById(R.id.shop_card_image)
    private val title: TextView = itemView.findViewById(R.id.shop_card_title)
    private val description: TextView = itemView.findViewById(R.id.shop_card_description)
    private val contentParams: TextView = itemView.findViewById(R.id.shop_card_content_params)
    private val detailsButton: TextView = itemView.findViewById(R.id.detailsBtn)
    private val downloadButton: View = itemView.findViewById(R.id.downloadBtn)
    private val downloadTextView: TextView = itemView.findViewById(R.id.downloadTV)
    private val downloadIcon: ImageView = itemView.findViewById(R.id.downloadIcon)

    fun bind(item: Chart, listener: ShopListener?) {
        GlideApp.with(itemView)
                .load(item.imageurl)
                .placeholder(R.drawable.img_placeholder)
                .centerCrop()
                .into(imageView)

        title.text = clearTitleForWrecks(item.name)
        description.text = descriptionFilter(item)
        description.movementMethod = ScrollingMovementMethod()
        downloadIcon.setImageDrawable(
                UiUtils(StrikeLinesApplication.applicationContext())
                        .getIcon(R.drawable.ic_download_chart, R.color.fab_text))
        val contentText = "${item.region} • ${if (item.price.toInt() != 0) "$${item.price}"
        else itemView.context.getString(R.string.shop_item_tag_freemap)}"
        contentParams.text = contentText
        detailsButton.setOnClickListener { listener?.onDetailsClicked(item) }
        downloadButton.setOnClickListener { listener?.onDownloadClicked(item) }
        if(item.downloadurl.isEmpty()) downloadTextView.text =
                StrikeLinesApplication.applicationContext().getString(R.string.get_chart_from_web_btn_tag)
    }
}


interface ShopListener {
    fun onDetailsClicked(item: Chart)
    fun onDownloadClicked(item: Chart)
}
