package com.github.kr328.clash.design.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.design.R
import com.github.kr328.clash.network.PlanData

class PlanDataAdapter( private val onItemClick: (plan: PlanData) -> Unit  ) : RecyclerView.Adapter<SubscriptionViewHolder>() {

    private val subscriptions = mutableListOf<PlanData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item_subscription, parent, false)
        return SubscriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val subscription = subscriptions[position]
        holder.bind(subscription)
        holder.itemView.setOnClickListener {
            onItemClick(subscription) // 将点击的 position 传递给回调函数
        }
    }

    override fun getItemCount(): Int {
        return subscriptions.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: List<PlanData>) {
        subscriptions.clear()
        subscriptions.addAll(newData)
        notifyDataSetChanged()
    }


    @SuppressLint("NotifyDataSetChanged")
    fun addData(newData: List<PlanData>) {
        subscriptions.addAll(newData)
        notifyDataSetChanged()
    }

}

class SubscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val planName: TextView = itemView.findViewById(R.id.subscriptionName)
    private val planPrice: TextView = itemView.findViewById(R.id.subscriptionTime)
    private val planDescription: TextView = itemView.findViewById(R.id.subscriptionDescription)
    private val badge: TextView = itemView.findViewById(R.id.tv_badge)
    private val features: TextView = itemView.findViewById(R.id.tv_features)
    private val btnOrder: TextView = itemView.findViewById(R.id.btn_order)

    private val grouponemonth: LinearLayout =  itemView.findViewById(R.id.grouponemohtthtest)
    private val groupquitytthtest: LinearLayout =  itemView.findViewById(R.id.groupquitytthtest)
    private val grouphelfyear: LinearLayout =  itemView.findViewById(R.id.grouphelfyear)
    private val grouponeyear: LinearLayout =  itemView.findViewById(R.id.grouponeyear)

    private val onemohtthtest: TextView = itemView.findViewById(R.id.onemohtthtest)
    private val onequietythtest: TextView = itemView.findViewById(R.id.onequietythtest)
    private val helfyearthtest: TextView = itemView.findViewById(R.id.helfyearthtest)
    private val oneyearthtest: TextView = itemView.findViewById(R.id.oneyearthtest)


    private val onemonthprice: TextView = itemView.findViewById(R.id.onemonthprice)
    private val onequietyprice: TextView = itemView.findViewById(R.id.onequietyprice)
    private val helfyearprice: TextView = itemView.findViewById(R.id.helfyearprice)
    private val oneyearprice: TextView = itemView.findViewById(R.id.oneyearprice)


    private val onemonthpriceday: TextView = itemView.findViewById(R.id.onemonthpriceday)
    private val onequietypriceday: TextView = itemView.findViewById(R.id.onequietypriceday)
    private val helfyearpriceday: TextView = itemView.findViewById(R.id.helfyearpriceday)
    private val oneyearpriceday: TextView = itemView.findViewById(R.id.oneyearpriceday)


    fun bind(data: PlanData) {
        planName.text = data.name

        val gb = (data.transfer_enable ?: 0L) / 1024L / 1024L / 1024L
        planPrice.text = "流量：${gb}GB"

        planDescription.text = data.tagline ?: data.content ?: ""

        if ((data.hot ?: 0) == 1) {
            badge.visibility = View.VISIBLE
            badge.text = "推荐"
        } else {
            badge.visibility = View.GONE
        }

        val isOnetime = data.onetime_price != null
        if (isOnetime) {
            val p = (data.onetime_price ?: 0).toDouble() / 100
            onemohtthtest.text = "一次性"
            onemonthprice.text = "${p}"
            val trans = if (gb > 0) gb.toInt() else 1
            onemonthpriceday.text = "${String.format("%.2f", p / trans)}元/GB"
        } else {
            val p = (data.month_price ?: 0L).toDouble() / 100
            onemohtthtest.text = "1个月"
            onemonthprice.text = "${p}"
            onemonthpriceday.text = "${String.format("%.2f", p / 30)}元/天"
        }

        val feats = data.features ?: emptyList()
        val sb = StringBuilder()
        for (f in feats) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append("✓  ").append(f)
        }
        features.text = sb.toString()

        btnOrder.setOnClickListener {
            itemView.performClick()
        }
    }

}
