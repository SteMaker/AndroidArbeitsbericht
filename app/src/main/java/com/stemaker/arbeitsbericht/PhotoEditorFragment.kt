package com.stemaker.arbeitsbericht

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
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
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import com.stemaker.arbeitsbericht.data.PhotoContainerData
import com.stemaker.arbeitsbericht.data.PhotoData
import com.stemaker.arbeitsbericht.databinding.FragmentPhotoEditorBinding
import com.stemaker.arbeitsbericht.databinding.PhotoLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PhotoEditorFragment : ReportEditorSectionFragment(),
    ReportEditorSectionFragment.OnExpandChange {
    private var listener: OnPhotoEditorInteractionListener? = null
    var photoContainerData: PhotoContainerData? = null
    lateinit var dataBinding: FragmentPhotoEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht","PhotoEditorFragment.onCreate called")
        photoContainerData = listener!!.getPhotoContainerData()
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
        dataBinding.photoContainerData = photoContainerData!!

        for(p in photoContainerData!!.items) {
            addPhotoView(p)
        }

        dataBinding.root.findViewById<ImageButton>(R.id.photo_add_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val p = photoContainerData!!.addPhoto()
                addPhotoView(p)
            }
        })

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

    interface OnPhotoEditorInteractionListener {
        fun getPhotoContainerData(): PhotoContainerData
    }

    val REQUEST_TAKE_PHOTO = 1
    var continuation: Continuation<Boolean>? = null

    fun addPhotoView(p: PhotoData) {
        val inflater = layoutInflater
        val container = dataBinding.root.findViewById<LinearLayout>(R.id.photo_content_container)
        val photoDataBinding: PhotoLayoutBinding = PhotoLayoutBinding.inflate(inflater, null, false)
        photoDataBinding.photo = p
        photoDataBinding.lifecycleOwner = activity
        photoDataBinding.root.findViewById<ImageButton>(R.id.photo_del_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer = showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
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
                GlobalScope.launch(Dispatchers.Main) {
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                        // Ensure that there's a camera activity to handle the intent
                        takePictureIntent.resolveActivity(activity!!.packageManager)?.also {
                            // Create the File where the photo should go
                            val photoFile: File? = try {
                                createImageFile()
                            } catch (ex: IOException) {
                                // TODO: Error occurred while creating the File
                                null
                            }
                            // Continue only if the File was successfully created
                            photoFile?.also {
                                val photoURI: Uri = FileProvider.getUriForFile(activity!!.applicationContext, "com.android.stemaker.arbeitsbericht.fileprovider", it)
                                Log.d("Arbeitsbericht.PhotoEditorFragment.onClickTakePhoto", "PhotoURI: ${photoURI}")
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                                val success = suspendCoroutine<Boolean> {
                                    Log.d("Arbeitsbericht.PhotoEditorFragment.addPhotoView", "Coroutine: suspended")
                                    continuation = it
                                }
                                Log.d("Arbeitsbericht.PhotoEditorFragment.addPhotoView", "Coroutine: resumed with success=${success}")
                                if(success) {
                                    p.file.value = photoFile.absolutePath
                                } else {
                                    // TODO: Error occurred while taking the photo
                                }
                            }
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
            continuation!!.resume(true)
        } else {
            Log.w("Arbeitsbericht.PhotoEditorFragment.onActivityResult", "No success taking the photo. requestCode=$requestCode, resultCode=$resultCode")
            continuation!!.resume(false)
        }
    }

}
