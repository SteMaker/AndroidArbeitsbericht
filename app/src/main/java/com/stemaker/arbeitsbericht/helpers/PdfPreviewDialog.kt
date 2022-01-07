package com.stemaker.arbeitsbericht.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.stemaker.arbeitsbericht.R
import java.io.File
import kotlin.math.abs

class PdfPreviewDialog : DialogFragment() {
    var file: File? = null
        set(value) {
            field = value
            fd = ParcelFileDescriptor.open(value, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = fd?.let { PdfRenderer(it) }
        }

    private var fd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var currentPage = 0
    private var bitmap: Bitmap? = null
    private lateinit var imgV: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(file == null) {
            savedInstanceState?.getSerializable("FILE")?.let { file = it as File }
        }
        val v = inflater.inflate(R.layout.fragment_pdf_preview_dialog, container, false)
        val btnClose: View = v.findViewById(R.id.close_button)
        btnClose.setOnClickListener {
            renderer?.close()
            dismiss()
        }
        val btnPrev: View = v.findViewById(R.id.prev_button)
        btnPrev.setOnClickListener {
            previousPage()
        }
        val btnNext: View = v.findViewById(R.id.next_button)
        btnNext.setOnClickListener {
            nextPage()
        }
        imgV = v.findViewById(R.id.pdf_preview_image_view)

        if(renderer == null) {
            val toast = Toast.makeText(activity, R.string.pdf_viewer_fail, Toast.LENGTH_LONG)
            toast.show()
            dismiss()
        } else {
            renderPage(currentPage)
            imgV.setOnTouchListener(object: OnSwipeTouchListener(activity as Context) {
                override fun onSwipeLeft() {
                    nextPage()
                }
                override fun onSwipeRight() {
                    previousPage()
                }
            })
        }
        return v
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("FILE", file)
    }

    private fun previousPage() {
        currentPage--
        if(currentPage < 0)
            currentPage = 0
        renderPage(currentPage)
    }

    private fun nextPage() {
        renderer?.let {
            currentPage++
            if (currentPage >= it.pageCount)
                currentPage = it.pageCount - 1
            renderPage(currentPage)
        }
    }

    private fun renderPage(pagenum: Int) {
        renderer?.let { pdfRenderer ->
            val page = pdfRenderer.openPage(pagenum)
            bitmap?.eraseColor(Color.WHITE) ?: run {
                bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            }
            bitmap?.let {
                page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                imgV.setImageBitmap(it)
            }
            page.close()
        }
    }

    override fun onResume() {
        super.onResume()
        val params = dialog!!.window!!.attributes!!
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog!!.window!!.attributes = params
    }

    open class OnSwipeTouchListener(ctx: Context) : View.OnTouchListener {
        private val gestureDetector: GestureDetector

        companion object {
            private const val SWIPE_THRESHOLD = 100
            private const val SWIPE_VELOCITY_THRESHOLD = 100
        }

        init {
            gestureDetector = GestureDetector(ctx, GestureListener())
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return gestureDetector.onTouchEvent(event)
        }

        private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {


            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                var result = false
                try {
                    val diffY = e2.y - e1.y
                    val diffX = e2.x - e1.x
                    if (abs(diffX) > abs(diffY)) {
                        if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight()
                            } else {
                                onSwipeLeft()
                            }
                            result = true
                        }
                    } else if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeBottom()
                        } else {
                            onSwipeTop()
                        }
                        result = true
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
                return result
            }
        }

        open fun onSwipeRight() {}
        open fun onSwipeLeft() {}
        open fun onSwipeTop() {}
        open fun onSwipeBottom() {}
    }
}