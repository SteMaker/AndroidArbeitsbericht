package com.stemaker.arbeitsbericht.editor_fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.databinding.FragmentPhotoEditorBinding
import com.stemaker.arbeitsbericht.databinding.PhotoLayoutBinding
import com.stemaker.arbeitsbericht.helpers.ImageViewFragment
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import com.stemaker.arbeitsbericht.view_models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "PhotoEditorFrag"

class PhotoEditorFragment(
    private val report: ReportData,
    private val scalePhotos: Boolean,
    private val photoResolution: Int)
    : ReportEditorSectionFragment()
{
    lateinit var dataBinding: FragmentPhotoEditorBinding
    private var contCnt = 1
    private val activityResultContinuation = mutableMapOf<Int, Continuation<Uri?>>()
    var cameraPermissionContinuation: Continuation<Boolean>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        Log.d("Arbeitsbericht","PhotoEditorFragment.onCreateView called")
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentPhotoEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.photo))

        dataBinding.lifecycleOwner = viewLifecycleOwner
        val photoContainerData = report.photoContainer
        val viewModelContainer = ViewModelProvider(this, PhotoContainerViewModelFactory(viewLifecycleOwner, photoContainerData)).get(PhotoContainerViewModel::class.java)
        dataBinding.viewModelContainer = viewModelContainer

        for (viewModel in viewModelContainer) {
            addPhotoView(viewModel, viewModelContainer)
        }

        dataBinding.photoAddButton.setOnClickListener {
            val viewModel = viewModelContainer.addPhoto()
            val v = addPhotoView(viewModel, viewModelContainer)
            listener?.scrollTo(v)
        }
        return root
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.photoContentContainer.visibility = when(vis) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun getVisibility(): Boolean {
        return dataBinding.photoContentContainer.visibility != View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Access to camera granted")
                cameraPermissionContinuation?.resume(true)
            } else {
                Log.d(TAG, "Access to camera denied")
                val toast = Toast.makeText(this.activity, "Berechtigungen abgelehnt, Foto kann nicht erstellt werden", Toast.LENGTH_LONG)
                toast.show()
                cameraPermissionContinuation?.resume(false)
            }
        }
    }

    private fun addPhotoView(viewModel: PhotoViewModel, viewModelContainer: PhotoContainerViewModel): View {
        val inflater = layoutInflater
        val container = dataBinding.photoContentContainer
        val photoDataBinding: PhotoLayoutBinding = PhotoLayoutBinding.inflate(inflater, null, false)
        photoDataBinding.viewModel = viewModel
        photoDataBinding.lifecycleOwner = activity
        photoDataBinding.photoDelButton.setOnClickListener { btn ->
            GlobalScope.launch(Dispatchers.Main) {
                val answer =
                    showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                if (answer == AlertDialog.BUTTON_POSITIVE) {
                    container.removeView(photoDataBinding.root)
                    viewModelContainer.removePhoto(viewModel)
                }
            }
        }

        photoDataBinding.photoTakeButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                var permissionGranted = true
                val ctx = this@PhotoEditorFragment.activity
                ctx?.also { ctxNotNull ->
                    if (ContextCompat.checkSelfPermission(ctxNotNull, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // Request user to grant write external storage permission.
                        Log.d(TAG, "Need to query user for permissions, starting coroutine")
                        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CAMERA_PERMISSION)
                        permissionGranted = suspendCoroutine<Boolean> {
                            Log.d(TAG, "Coroutine: suspended")
                            cameraPermissionContinuation = it
                        }
                    }
                }
                if (permissionGranted) {
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                        try {
                            // Create the File where the photo should go
                            val photoFile: File? = try {
                                createImageFile()
                            } catch (ex: IOException) {
                                val toast = Toast.makeText(activity, "Konnte Datei für Foto nicht erstellen", Toast.LENGTH_LONG)
                                toast.show()
                                null
                            }
                            // Continue only if the File was successfully created
                            photoFile?.also { photoFile ->
                                val photoURI: Uri =
                                    FileProvider.getUriForFile(requireActivity().applicationContext, "com.stemaker.arbeitsbericht.fileprovider", photoFile)
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                                startActivityForResult(takePictureIntent, contCnt)
                                // result is null, if it really failed we'll catch an exception below
                                val result = suspendCoroutine<Uri?> {
                                    activityResultContinuation[contCnt] = it
                                    contCnt++
                                }
                                try {
                                    applyPhotoFile(viewModel, photoFile)
                                } catch (ex: Exception) {
                                    val toast = Toast.makeText(activity, "Konnte Datei für Foto nicht erstellen", Toast.LENGTH_LONG)
                                    toast.show()
                                }
                            }
                        } catch (e: Exception) {
                            val toast = Toast.makeText(activity, "Konnte keine Kamera-App starten", Toast.LENGTH_LONG)
                            toast.show()
                        }
                    }
                }
            }
        }

        photoDataBinding.photoLoadButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                val mimeTypes = arrayOf("image/jpeg")
                val intent = Intent()
                    .setType("*/*")
                    .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                    .setAction(Intent.ACTION_GET_CONTENT)

                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_photo)), contCnt)
                val file = suspendCoroutine<Uri?> {
                    activityResultContinuation[contCnt] = it
                    contCnt++
                }
                if (file == null) {
                    Log.d(TAG, "No photo file was selected")
                } else {
                    Log.d(TAG, "Selected photo: ${file}")
                    try {
                        val photoFile = createImageFile()
                        copyUriToFile(file, photoFile)
                        applyPhotoFile(viewModel, photoFile)
                    } catch (ex: Exception) {
                        val toast = Toast.makeText(activity, "Konnte Datei für Foto nicht erstellen", Toast.LENGTH_LONG)
                        toast.show()
                    }
                }
            }
        }

        photoDataBinding.photoView.setOnClickListener {
            if (viewModel.file.value != "") {
                val tmpFile = File(viewModel.file.value!!) // because old app version stored the path here as well
                val photoView = ImageViewFragment()
                photoView.file = File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), tmpFile.name)
                photoView.show(requireActivity().supportFragmentManager, "PhotoView")
            }
        }

        val pos = container.childCount
        container.addView(photoDataBinding.root, pos)
        return photoDataBinding.root
    }

    private fun applyPhotoFile(viewModel: PhotoViewModel, f: File) {
        val destHeight: Int
        val destWidth: Int
        val options = BitmapFactory.Options()
        val bitmap = BitmapFactory.decodeFile(f.absolutePath, options)
        if(scalePhotos && bitmap.width > photoResolution && bitmap.height > photoResolution) {
            if(bitmap.width < bitmap.height) {
                destWidth = photoResolution
                destHeight = bitmap.height * destWidth / bitmap.width
            } else {
                destHeight = photoResolution
                destWidth = bitmap.width * destHeight / bitmap.height
            }
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, destWidth, destHeight, false)
            val outStream = FileOutputStream(f)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outStream)
            outStream.close()
        } else {
            destHeight = bitmap.height
            destWidth = bitmap.width
        }
        // As last step so that it gets skipped in case we had any exception (catched outside)
        viewModel.imageHeight.setValue(destHeight)
        viewModel.imageWidth.setValue(destWidth)
        viewModel.file.setValue(f.name)
    }

    private fun copyUriToFile(uri: Uri, file: File) {
        val inStream = requireActivity().contentResolver.openInputStream(uri) ?: throw Exception("Could not open input file")
        val outStream = FileOutputStream(file)
        val buf = ByteArray(1024)
        var len: Int = inStream.read(buf)
        while(len > 0) {
            outStream.write(buf,0,len)
            len = inStream.read(buf)
        }
        outStream.close()
        inStream.close()
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("Arbeitsbericht_${timeStamp}", ".jpg", storageDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            activityResultContinuation[requestCode]?.let {
                it.resume(data?.data)
                activityResultContinuation.remove(requestCode)
            }
        }
    }

    companion object {
        const val REQUEST_CODE_CAMERA_PERMISSION = 1
    }
}
