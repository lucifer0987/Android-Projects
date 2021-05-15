package com.example.memorygame

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RecoverySystem
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.utils.BitmapScalar
import com.example.memorygame.utils.EXTRA_BOARD_SIZE
import com.example.memorygame.utils.isPermissionGranted
import com.example.memorygame.utils.requestPermission
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object {
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
        private const val PICK_PHOTO_CODE = 123
        private const val READ_PHOTOS_PERMISSION_CODE = 234
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var progressbar:ProgressBar

    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()
    private lateinit var adapter: ImagePickerAdapter
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        //Init
        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        progressbar = findViewById(R.id.progressBar)

        //show back button, then override onOptions
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //getting extra
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        //setting title
        supportActionBar?.title = "Choose pics (0 / ${numImagesRequired})"

        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
        adapter = ImagePickerAdapter(
            this,
            chosenImageUris,
            boardSize,
            object : ImagePickerAdapter.ImageClickListener {
                override fun onPlaceholderClicked() {
                    if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
                        launchIntentForPhotos()
                    } else {
                        requestPermission(
                            this@CreateActivity,
                            READ_PHOTOS_PERMISSION,
                            READ_PHOTOS_PERMISSION_CODE
                        )
                    }
                }

            })
        rvImagePicker.adapter = adapter

        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etGameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }

        })

        btnSave.setOnClickListener {
            saveDataToFirebase()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_PHOTOS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhotos()
            } else {
                Toast.makeText(
                    this,
                    "You need to provide permissions to create game",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            Log.w("this", "did not get data back from the activity")
            return
        }

        val selectedUri: Uri? = data.data
        val clipData: ClipData? = data.clipData
        if (clipData != null) {
            Log.i("this", "numImages : ${clipData.itemCount}")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        } else if (selectedUri != null) {
            Log.i("this", "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }

        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose Pics (${chosenImageUris.size} / ${numImagesRequired})"
        btnSave.isEnabled = shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImageUris.size != numImagesRequired) {
            return false
        } else if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH) {
            return false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // back button working to go back to previous actitivity
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveDataToFirebase() {
        Log.i("this", "image upload started")
        btnSave.isEnabled = false
        //compress images
        val customGameName = etGameName.text.toString()
        //check game name for uniqueness
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if(document != null && document.data != null){
                AlertDialog.Builder(this)
                    .setTitle("Name Taken")
                    .setMessage("A game already exists with the name $customGameName. Please chooseanother")
                    .show()
                btnSave.isEnabled = true
            }else{
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener{
            btnSave.isEnabled = true
            Log.e("this", "Error while fetching unique game name")
            Toast.makeText(this, "Please try again", Toast.LENGTH_LONG).show()
        }
    }

    //upload to storage
    private fun handleImageUploading(customGameName: String) {
        progressbar.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()
        for ((index, photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            val filepath = "images/$customGameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filepath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask{ photoUploadTask ->
                    Log.i("this", "uploading")
                    photoReference.downloadUrl
                }.addOnCompleteListener{downloadUrlTask ->
                    if(!downloadUrlTask.isSuccessful){
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_LONG).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if(didEncounterError){
                        progressbar.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    progressbar.progress = (uploadedImageUrls.size * 100) /chosenImageUris.size
                    Toast.makeText(this, "Image Uploaded", Toast.LENGTH_SHORT).show()

                    if(uploadedImageUrls.size == chosenImageUris.size){
                        handleAllImagesUploaded(customGameName, uploadedImageUrls)
                    }
                }
        }
    }

    //upload to firestore
    private fun handleAllImagesUploaded(customGameName: String, uploadedImageUrls: MutableList<String>) {
        db.collection("games").document(customGameName)
            .set(mapOf("images" to uploadedImageUrls))
            .addOnCompleteListener{ gameCreationTask ->
                progressbar.visibility = View.GONE
                if(!gameCreationTask.isSuccessful){
                    Toast.makeText(this, "Not successfull", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                AlertDialog.Builder(this)
                    .setTitle("upload complete! Let's play your game")
                    .setPositiveButton("OK"){ _, _ ->
                        val resultData = Intent()
                        resultData.putExtra("EXTRA_GAME_NAME", customGameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }

        val scaledBitmap = BitmapScalar.scaleToFitHeight(originalBitmap, 250)

        val byteOutputArray = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputArray)
        return byteOutputArray.toByteArray()
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose Pics"), PICK_PHOTO_CODE)
    }
}