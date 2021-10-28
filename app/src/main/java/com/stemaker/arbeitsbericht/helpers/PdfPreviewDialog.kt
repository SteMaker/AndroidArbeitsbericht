package com.stemaker.arbeitsbericht.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.stemaker.arbeitsbericht.R
import java.io.File
import kotlin.math.abs

class PdfPreviewDialog(val file: File, val ctx: Context) : DialogFragment() {
    private val fd: ParcelFileDescriptor? = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer: PdfRenderer? = fd?.let { PdfRenderer(it) }
    private var currentPage = 0
    private var bitmap: Bitmap? = null
    private lateinit var imgV: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
            val toast = Toast.makeText(ctx, R.string.pdf_viewer_fail, Toast.LENGTH_LONG)
            toast.show()
            dismiss()
        } else {
            renderPage(currentPage)
            imgV.setOnTouchListener(object: OnSwipeTouchListener(ctx) {
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

    private fun previousPage() {
        currentPage--
        if(currentPage < 0)
            currentPage = 0
        renderPage(currentPage)
    }

    private fun nextPage() {
        renderer?.also {
            currentPage++
            if (currentPage >= renderer.pageCount)
                currentPage = renderer.pageCount - 1
            renderPage(currentPage)
        }
    }

    private fun renderPage(page: Int) {
        if (renderer != null) {
            val page = renderer.openPage(page)
            bitmap?.eraseColor(Color.WHITE)?:run {
                bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            }
            bitmap?.also {
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