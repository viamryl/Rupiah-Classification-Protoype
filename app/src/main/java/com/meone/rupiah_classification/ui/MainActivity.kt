package com.meone.rupiah_classification.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.meone.rupiah_classification.R
import com.meone.rupiah_classification.databinding.ActivityMainBinding
import com.meone.rupiah_classification.helper.ImageClassificationHelper
import com.meone.rupiah_classification.helper.getImageUri
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                showToast("Permission request granted")
            } else {
                showToast("Permission request denied")
            }
        }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    private lateinit var imageClassifierHelper: ImageClassificationHelper
    private lateinit var cropImage: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        binding.galleryButton.setOnClickListener { startGallery() }
        binding.cameraButton.setOnClickListener { startCamera() }
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            val intent = Intent(this@MainActivity, UCropActivity::class.java )
            intent.putExtra("Image Data", uri.toString())
            startActivityForResult(intent, 100)
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }

    private fun showImage() {
        currentImageUri?.let { uri ->
            Log.d("Image URI", "showImage: $uri")
            binding.previewImageView.setImageURI(uri)

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

            imageClassifierHelper = ImageClassificationHelper(
                context = this,
                classifierListener = object : ImageClassificationHelper.ClassifierListener {
                    override fun onError(error: String) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                        runOnUiThread {
                            results?.let { it ->
                                if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {
                                    println(it)
                                    val sortedCategories =
                                        it[0].categories.sortedByDescending { it?.score }
                                    val displayResult = sortedCategories.joinToString(" ") {
                                        val rawResource = when (it.label) {
                                            0.toString() -> R.raw.dua_puluh_ribu
                                            1.toString() -> R.raw.dua_ribu
                                            2.toString() -> R.raw.lima_puluh_ribu
                                            3.toString() -> R.raw.lima_ribu
                                            4.toString() -> R.raw.sepuluh_ribu
                                            5.toString() -> R.raw.seratus_ribu
                                            6.toString() -> R.raw.seribu
                                            else -> null
                                        }
                                        val labelDescription = rawResource?.let { Uri.parse("android.resource://$packageName/$it") }
                                        "$labelDescription"
                                    }


                                    val mp = MediaPlayer()
                                    val displayResultSecond = sortedCategories.joinToString(" ") {
                                        val labelDescription = when (it.label) {
                                            0.toString() -> "dua puluh ribu"
                                            1.toString() -> "dua ribu"
                                            2.toString() -> "Lima Puluh ribu"
                                            3.toString() -> "Lima ribu"
                                            4.toString() -> "Sepuluh ribu"
                                            5.toString() -> "Seratus ribu"
                                            6.toString() -> "Seribu"
                                            else -> it.label.toString()
                                        }
                                        "$labelDescription " + NumberFormat.getPercentInstance()
                                            .format(it.score).trim()
                                    }

                                    mp.setDataSource(this@MainActivity, Uri.parse(displayResult))
                                    mp.prepare()
                                    mp.start()
                                    val textResult = "Hasil Analisis : $displayResultSecond"
                                    binding.tvResult.text = textResult

                                } else {
                                    Log.d("Testing", "Hasil Not Found")
                                }
                            }
                        }
                    }
                }
            )
            imageClassifierHelper.classifyImage(bitmap)
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == 101){
            val result: String = data!!.getStringExtra("Crop Image").toString()
            if (data != null) {
                currentImageUri = Uri.parse(result)
                showImage()
            }
        }
    }
}