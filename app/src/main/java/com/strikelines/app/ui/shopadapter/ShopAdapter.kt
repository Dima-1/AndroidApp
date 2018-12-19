package com.strikelines.app.ui.shopadapter

import android.support.v7.widget.RecyclerView
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.strikelines.app.*
import com.strikelines.app.domain.GlideApp
import com.strikelines.app.domain.models.Chart
import com.strikelines.app.utils.clearTitleForWrecks
import com.strikelines.app.utils.descriptionFilter


class ShopAdapter(val listener:ShopListener?):RecyclerView.Adapter<ShopItemViewHolder>() {

    val dataList = mutableListOf<Chart>()

    fun setData(list:List<Chart>) {
        //todo: add diffUtil
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = ShopItemViewHolder(LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_shopchart, parent, false))

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ShopItemViewHolder, position: Int){
        holder.bind(dataList[position], listener)

    }
}

class ShopItemViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {

    val imageView: ImageView = itemView.findViewById(R.id.shop_card_image)
    val title: TextView = itemView.findViewById(R.id.shop_card_title)
    val description: TextView = itemView.findViewById(R.id.shop_card_description)
    val contentParams: TextView = itemView.findViewById(R.id.shop_card_content_params)
    val detailsButton: TextView = itemView.findViewById(R.id.detailsBtn)
    val downloadButton: View = itemView.findViewById(R.id.downloadBtn)
    val downloadIcon: ImageView = itemView.findViewById(R.id.downloadIcon)

    fun bind(item:Chart, listener: ShopListener?) {
        GlideApp.with(itemView)
                .load(item.imageurl)
                .centerCrop()
                .into(imageView)

        title.text = clearTitleForWrecks(item.name)
        description.text = descriptionFilter(item)
        description.movementMethod = ScrollingMovementMethod()
//        downloadIcon.setBackgroundResource(UiUtils(
//                StrikeLinesApplication.applicationContext())
//                .getIcon(R.drawable.ic_download_chart, R.color.fab_text))
        contentParams.text = "NO DATA" //todo fix after data become available in api
        detailsButton.setOnClickListener { listener?.onDetailsClicked(item) }
        downloadButton.setOnClickListener { listener?.onDownloadClicked(item.weburl) } //todo:change to download link if needed

    }



}

interface ShopListener{
    fun onDetailsClicked(item:Chart)
    fun onDownloadClicked(url:String)
}
