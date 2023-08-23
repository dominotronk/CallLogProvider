package com.example.calllogprovider

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


import android.content.Context;

class CallLogAdapter(context:Context, data: List<CallLogData>) :
    ArrayAdapter<CallLogData>(context, R.layout.mylayout, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: ViewHolder

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.mylayout, parent, false)
            viewHolder = ViewHolder()
            viewHolder.textView1 = view.findViewById(R.id.textView1)
            viewHolder.textView2 = view.findViewById(R.id.textView2)
            viewHolder.textView3 = view.findViewById(R.id.textView3)
            viewHolder.textView4 = view.findViewById(R.id.textView4)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        val callLogData = getItem(position)

        viewHolder.textView1.text = "Numer: ${callLogData?.number}"
        viewHolder.textView2.text = "\n\nTyp: ${callLogData?.type}"
        viewHolder.textView3.text = "\n\nCzas połączenia: ${callLogData?.duration} sek"
        viewHolder.textView4.text = "\n\nData połączenia: ${callLogData?.date}"

        return view!!
    }

    private class ViewHolder {
        lateinit var textView1: TextView
        lateinit var textView2: TextView
        lateinit var textView3: TextView
        lateinit var textView4: TextView
    }
}
