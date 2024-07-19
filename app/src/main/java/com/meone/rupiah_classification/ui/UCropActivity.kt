package com.meone.rupiah_classification.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.meone.rupiah_classification.databinding.ActivityUcropBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

class UCropActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUcropBinding

    lateinit var sourceUri: String
    lateinit var destUri: String
    lateinit var uri: Uri


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUcropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.sourceUri = intent.getStringExtra("Image Data").toString()
        this.uri = Uri.parse(sourceUri)

        this.destUri = StringBuilder(UUID.randomUUID().toString()).append(".jpg").toString()

        val cropOption = UCrop.Options()

        UCrop.of(uri, Uri.fromFile(File(cacheDir, destUri)))
            .withOptions(cropOption)
            .withAspectRatio(16F, 9F)
            .withMaxResultSize(800, 800)
            .start(this@UCropActivity)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri: Uri? = UCrop.getOutput(data!!)
            // Handle the resultUri here

            intent.putExtra("Crop Image", resultUri.toString())
            setResult(101, intent)
            finish()

        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError: Throwable? = UCrop.getError(data!!)
            // Handle the cropError here
        }
    }
}