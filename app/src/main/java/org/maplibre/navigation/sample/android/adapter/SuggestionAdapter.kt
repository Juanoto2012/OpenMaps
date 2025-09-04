package org.maplibre.navigation.sample.android.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.maplibre.navigation.sample.android.R
import org.maplibre.navigation.sample.android.model.Suggestion

class SuggestionAdapter(private var suggestion: List<Suggestion>, private val onItemClick: (Suggestion) -> Unit): RecyclerView.Adapter<SuggestionViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val currentItem = suggestion[position]
        holder.item.text = currentItem.name
        val region = if(!currentItem.state.isNullOrBlank()){
            currentItem.state
        }else {
            currentItem.city
        }
        holder.country.text = "$region, ${currentItem.country}"

        holder.item.setOnClickListener {
            onItemClick(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return suggestion.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<Suggestion>) {
        suggestion = newList
        notifyDataSetChanged()
    }
}

class SuggestionViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
    var item = itemView.findViewById<TextView>(R.id.item_title)
    var country = itemView.findViewById<TextView>(R.id.item_subtitle)
}