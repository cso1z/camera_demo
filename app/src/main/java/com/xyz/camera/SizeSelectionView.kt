package com.xyz.camera

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SizeSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var tvLabel: TextView
    private lateinit var tvSelected: TextView
    private lateinit var ivArrow: ImageView
    private lateinit var layoutList: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SizeSelectionAdapter

    private var isExpanded = false
    private var onSizeSelectedListener: ((SizeItem) -> Unit)? = null

    init {
        initView()
    }

    private fun initView() {
        orientation = VERTICAL

        // 创建头部布局
        val headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            setOnClickListener { toggleExpanded() }
        }

        tvLabel = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            textSize = 14f
        }

        tvSelected = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            text = "未选择"
            setTextColor(android.graphics.Color.parseColor("#666666"))
            setPadding(0, 0, 16, 0)
        }

        ivArrow = ImageView(context).apply {
            layoutParams = LayoutParams(40, 40)
            setImageResource(android.R.drawable.arrow_down_float)
            rotation = 0f
        }

        headerLayout.addView(tvLabel)
        headerLayout.addView(tvSelected)
        headerLayout.addView(ivArrow)

        // 创建列表布局
        layoutList = LinearLayout(context).apply {
            orientation = VERTICAL
            background = android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)
            visibility = View.GONE
            setPadding(0, 8, 0, 0)
        }

        recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 3) // 3列布局
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        layoutList.addView(recyclerView)

        // 添加到主布局
        addView(headerLayout)
        addView(layoutList)
    }

    fun setLabel(label: String) {
        tvLabel.text = label
    }

    fun setSizeList(sizeList: List<SizeItem>) {
        adapter = SizeSelectionAdapter(sizeList) { sizeItem ->
            tvSelected.text = sizeItem.toString()
            onSizeSelectedListener?.invoke(sizeItem)
            toggleExpanded()
        }
        recyclerView.adapter = adapter
    }

    fun setOnSizeSelectedListener(listener: (SizeItem) -> Unit) {
        onSizeSelectedListener = listener
    }

    fun setSelectedSize(sizeItem: SizeItem?) {
        tvSelected.text = sizeItem?.toString() ?: "未选择"
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        layoutList.visibility = if (isExpanded) View.VISIBLE else View.GONE
        ivArrow.rotation = if (isExpanded) 180f else 0f
    }

    fun isExpanded(): Boolean = isExpanded

    fun collapse() {
        if (isExpanded) {
            toggleExpanded()
        }
    }
}
