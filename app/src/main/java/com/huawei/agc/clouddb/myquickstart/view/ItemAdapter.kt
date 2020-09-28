package com.huawei.agc.clouddb.myquickstart.view

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.huawei.agc.clouddb.myquickstart.R
import com.huawei.agc.clouddb.myquickstart.model.Book
import com.huawei.agc.clouddb.myquickstart.util.CloudDB
import kotlinx.android.synthetic.main.item.view.*

/**
 * Simple adapter
 */
class ItemAdapter(var context: Context, var items: ArrayList<Book>): RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    init {
        CloudDB.createObjectType()
        CloudDB.openCloudDBZone()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        return ItemViewHolder(view, context)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ItemViewHolder(view: View, cont: Context): RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private val title = view.title
        private val desc = view.description
        private val context = cont
        private val editButton = view.editButton
        private val deleteButton = view.deleteButton

        init {
            editButton.setOnClickListener(this)
            deleteButton.setOnClickListener(this)
        }

        fun bind(book: Book) {
            title.text = book.bookName
            desc.text = book.description
        }

        override fun onClick(v: View?) {
            var item: Book

            if (v?.id == R.id.editButton) {
                item = items[adapterPosition]
                editItem(item)
            }
            if (v?.id == R.id.deleteButton) {
                item = items[adapterPosition]
                deleteItem(item)
            }
        }

        private fun deleteItem(item: Book) {
            CloudDB.deleteBook(listOf(item))
            items.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
        }

        private fun editItem(item: Book) {

            val builder = AlertDialog.Builder(context)
            val inflater = LayoutInflater.from(context);
            val view = inflater?.inflate(R.layout.popup, null)

            builder.setView(view)
            val alertDialog = builder.create()
            alertDialog?.show()

            val saveButton: Button? = view?.findViewById(R.id.saveButton)
            val title: EditText? = view?.findViewById(R.id.titleBook)
            val description: EditText? = view?.findViewById(R.id.descriptionBook)
            val titlePage: TextView? = view?.findViewById(R.id.titlePage)

            titlePage?.text = context.getString(R.string.edit_book)
            title?.setText(item.bookName)
            description?.setText(item.description)
            saveButton?.text = context.getString(R.string.update)

            saveButton?.setOnClickListener {
                item.bookName = title?.text.toString().trim()
                item.description = description?.text.toString().trim()

                CloudDB.insertBook(item)
                notifyItemChanged(adapterPosition)
                alertDialog?.dismiss()
            }
        }
    }
}