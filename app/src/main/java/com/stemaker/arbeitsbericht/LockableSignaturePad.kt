package com.stemaker.arbeitsbericht

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import com.caverock.androidsvg.SVG
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.File
import java.io.FileOutputStream

class LockableSignaturePad(context: Context, attrs: AttributeSet): SignaturePad(context, attrs) {
    private var sigSvg: String = ""
    var locked = false
    private var w: Int = 0
    private var h: Int = 0

    fun setSvg(_svg: String) {
        sigSvg = _svg
        w = width
        h = height
        drawSvg(sigSvg, w, h)
    }

    override fun onSizeChanged(_w: Int, _h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(_w, _h, oldw, oldh)
        w = _w
        h = _h
        drawSvg(sigSvg, w, h)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(locked)
            return true
        return super.onTouchEvent(event)
    }

    fun saveBitmapToFile(file: File) {
        val fos = FileOutputStream(file)
        if(isEmpty) {
            val w2 = when {
                w > 0-> w
                else -> 100
            }
            val h2 = when {
                h > 0-> h
                else -> w/3
            }
            val bitmap = Bitmap.createBitmap(w2, h2, Bitmap.Config.RGB_565)
            bitmap.eraseColor(Color.WHITE)
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fos)
        } else {
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 85, fos)
        }
        fos.flush()
        fos.close()
    }

    private fun drawSvg(svg: String, w: Int, h: Int) {
        if(w != 0 && h != 0 && svg != "") {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            val cCanvas = Canvas(bitmap)
            val cSvg = SVG.getFromString(svg)
            cSvg.renderToCanvas(cCanvas)
            signatureBitmap = bitmap
        }
    }
}