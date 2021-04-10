package com.stemaker.arbeitsbericht.editor_fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.stemaker.arbeitsbericht.helpers.ImageViewFragment
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.PhotoContainerData
import com.stemaker.arbeitsbericht.data.PhotoData
import com.stemaker.arbeitsbericht.databinding.FragmentPhotoEditorBinding
import com.stemaker.arbeitsbericht.databinding.PhotoLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PhotoEditorFragment : ReportEditorSectionFragment(),
    ReportEditorSectionFragment.OnExpandChange {
    private var listener: OnPhotoEditorInteractionListener? = null
    lateinit var dataBinding: FragmentPhotoEditorBinding
    var activePhoto: PhotoData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht","PhotoEditorFragment.onCreate called")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        Log.d("Arbeitsbericht","PhotoEditorFragment.onCreateView called")
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentPhotoEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.photo))

        dataBinding.lifecycleOwner = this
        GlobalScope.launch(Dispatchers.Main) {
            val photoContainerData = listener!!.getPhotoContainerData()
            dataBinding.photoContainerData = photoContainerData

            for (p in photoContainerData.items) {
                addPhotoView(p, photoContainerData)
            }

            dataBinding.root.findViewById<ImageButton>(R.id.photo_add_button).setOnClickListener(object : View.OnClickListener {
                override fun onClick(btn: View) {
                    val p = photoContainerData.addPhoto()
                    addPhotoView(p, photoContainerData)
                }
            })
        }

        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context, this)
        if (context is OnPhotoEditorInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnPhotoEditorInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.root.findViewById<LinearLayout>(R.id.photo_content_container).setVisibility(if(vis) View.VISIBLE else View.GONE)
    }

    override fun getVisibility(): Boolean {
        return dataBinding.root.findViewById<LinearLayout>(R.id.photo_content_container).visibility != View.GONE
    }

    interface OnPhotoEditorInteractionListener {
        suspend fun getPhotoContainerData(): PhotoContainerData
    }

    val REQUEST_TAKE_PHOTO = 1

    fun addPhotoView(p: PhotoData, photoContainerData: PhotoContainerData) {
        val inflater = layoutInflater
        val container = dataBinding.root.findViewById<LinearLayout>(R.id.photo_content_container)
        val photoDataBinding: PhotoLayoutBinding = PhotoLayoutBinding.inflate(inflater, null, false)
        photoDataBinding.photo = p
        photoDataBinding.lifecycleOwner = activity
        photoDataBinding.root.findViewById<ImageButton>(R.id.photo_del_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer =
                        showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        Log.d("Arbeitsbericht.PhotoEditorFragment.photo_del_button.onClick", "deleting work item element")
                        container.removeView(photoDataBinding.root)
                        photoContainerData!!.removePhoto(p)
                    } else {
                        Log.d("Arbeitsbericht.WorkTimeEditorFragment.work_time_del_button.onClick", "cancelled deleting work item element")
                    }
                }
            }
        })

        photoDataBinding.root.findViewById<ImageButton>(R.id.photo_take_button).setOnClickListener(object : View.OnClickListener {
            override fun onClick(btn: View) {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    // Ensure that there's a camera activity to handle the intent
                    takePictureIntent.resolveActivity(activity!!.packageManager)?.also {
                        // Create the File where the photo should go
                        val photoFile: File? = try {
                            createImageFile()
                        } catch (ex: IOException) {
                            val toast = Toast.makeText(activity, "Konnte Datei für Foto nicht erstellen", Toast.LENGTH_LONG)
                            toast.show()
                            null
                        }
                        // Continue only if the File was successfully created
                        photoFile?.also {
                            val photoURI: Uri = FileProvider.getUriForFile(activity!!.applicationContext, "com.stemaker.arbeitsbericht.fileprovider", it)
                            Log.d("Arbeitsbericht.PhotoEditorFragment.onClickTakePhoto", "PhotoURI: ${photoURI}")
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                            p.file.value = photoFile.absolutePath
                            activePhoto = p
                            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                        }
                    }
                }
            }
        })

        photoDataBinding.root.findViewById<ImageView>(R.id.photo_view).setOnClickListener(object : View.OnClickListener {
            override fun onClick(btn: View) {
                Log.d("Arbeitsbericht.PhotoEditorFragment.photo_view.onClick", "Image clicked")
                if(p.file.value != "") {
                    val photoView = ImageViewFragment(p.file.value!!)
                    photoView.show(activity!!.supportFragmentManager, "PhotoView")
                }
            }
        })

        val pos = container.getChildCount()
        Log.d("Arbeitsbericht", "Adding work item card $pos to UI")
        container.addView(photoDataBinding.root, pos)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = activity!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("Arbeitsbericht_${timeStamp}", ".jpg", storageDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            // Trigger a redraw
            activePhoto?.file?.value = activePhoto?.file?.value
            // calc and store image dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(activePhoto?.file?.value, options)
            activePhoto?.imageHeight = options.outHeight
            activePhoto?.imageWidth = options.outWidth
        }
    }

}
