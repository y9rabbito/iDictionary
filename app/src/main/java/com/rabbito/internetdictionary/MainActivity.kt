package com.rabbito.internetdictionary

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*


class MainActivity : AppCompatActivity() {
    private var bitmap: Bitmap? = null

    companion object {
        //Image Pick Code
        private const val IMAGE_PICK_CODE = 1000

        //Permission Code
        private const val PERMISSION_CODE = 1001
    }

    //audio uri
    private var audioUri: String? = null
    private var tSpeech: TextToSpeech? = null


    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        setBackgroundOfApp()

        menu.setOnClickListener {
            val popUpMenu = androidx.appcompat.widget.PopupMenu(this, it)
            popUpMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.chooseBackground -> {
                        //Check runtime permission
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_DENIED
                            ) {
                                //Permission Denied
                                val permission =
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                //show popup to request runtime permission
                                requestPermissions(permission, PERMISSION_CODE)
                            } else {
                                //Permission Granted
                                pickImageFromGallery()
                            }
                        } else {
                            pickImageFromGallery()
                        }
                        true
                    }

                    R.id.deleteBackground -> {
                        if (bitmap != null) {
                            val builder = AlertDialog.Builder(this)
                            //set title for alert dialog
                            builder.setTitle("Delete Background")
                            //set message for alert dialog
                            builder.setMessage("Are you sure to delete background?")
                            builder.setIcon(R.drawable.ic_warn)

                            //performing positive action
                            builder.setPositiveButton("Yes") { dialogInterface, which ->
                                deleteFileFromInternalStorage("background.png")
                                Toast.makeText(this, "Background Deleted", Toast.LENGTH_SHORT)
                                    .show()
                                setBackgroundOfApp()
                            }
                            //performing cancel action
                            builder.setNeutralButton("Cancel") { dialogInterface, which ->
                            }
                            //performing negative action
                            builder.setNegativeButton("No") { dialogInterface, which ->

                            }
                            // Create the AlertDialog
                            val alertDialog: AlertDialog = builder.create()
                            // Set other dialog properties
                            alertDialog.setCancelable(true)
                            alertDialog.show()

                        } else {
                            Toast.makeText(this, "No Background Found", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }

                    R.id.share -> {
                        val shareIntent = Intent()
                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.putExtra(
                            Intent.EXTRA_TEXT,
                            "https://drive.google.com/drive/folders/1mCNANNvC03iaqs7vwg-D_qyCSy6xdoMX?usp=sharing"
                        )
                        shareIntent.type = "text/plain"
                        startActivity(shareIntent)
                        true
                    }

                    R.id.info -> {
                        if (isNetworkConnected()) {
                            val intent = Intent(this, Pdf_Viewer_Activity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                this,
                                "Check your internet Connection!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }
                    else -> false
                }
            }

            popUpMenu.inflate(R.menu.menu_popup)
            try {
                val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldPopup.isAccessible = true
                val mPopup = fieldPopup.get(popUpMenu)
                mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(mPopup, true)
            } catch (e: Exception) {
            } finally {
                popUpMenu.show()
            }
        }

        //Text change
        search_word_text.addTextChangedListener {
            val xrp: XmlResourceParser = resources.getXml(R.drawable.textview_selector)
            val csl: ColorStateList = ColorStateList.createFromXml(resources, xrp)
            search_word_text.setTextColor(csl)
            search_word_text.setBackgroundResource(R.drawable.custom_search_layout)

        }

        tSpeech = TextToSpeech(
            applicationContext
        ) { status ->
            if (status != TextToSpeech.ERROR) {
                tSpeech?.language = Locale.UK
            }
        }

        readAloud.setOnClickListener {
            var toSpeak = definition.text.toString()
            tSpeech!!.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null)
        }

        word_speak_button.setOnClickListener {
            if (audioUri != null) {
                try {
                    var player = MediaPlayer()
                    player.setDataSource(audioUri)
                    player.prepare()
                    player.start()
                } catch (e: Exception) {
                    Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show()
                }
            }
        }


        //Handling the search button
        searchButton.setOnClickListener {
            if (isNetworkConnected()) {
                var word = search_word_text.text.toString()
                word = word.trim()
                if (word.isEmpty()) {
                    search_word_text.setBackgroundResource(R.drawable.custom_search_layout)
                    word_show.visibility = View.VISIBLE
                    word_show.text = "Please Enter a Word"
                    word_show.setTextColor(ContextCompat.getColor(this, R.color.yellow))
                    var typeFace: Typeface? = ResourcesCompat.getFont(this, R.font.nunito_sans)
                    word_show.typeface = typeFace
                    scrollView.visibility = View.GONE
                    word_speak_button.visibility = View.GONE
                } else {
                    getMeaning(word.capitalize())
                }
            } else {
                Toast.makeText(this, "No Internet!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun getMeaning(word: String) {

        var url = "https://api.dictionaryapi.dev/api/v2/entries/en/$word"

        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, {
            //get the word and set the word
            var jsonOBJECT1 = it.getJSONObject(0)
            var word = jsonOBJECT1.getString("word")
            word_show.text = word.capitalize()
            var typeFace2: Typeface? = ResourcesCompat.getFont(this, R.font.nunito_sans_bold)
            word_show.typeface = typeFace2
            word_show.setTextColor(ContextCompat.getColor(this, R.color.white))
            word_show.visibility = View.VISIBLE

            //Getting the audio object
            var jsonArray1 = jsonOBJECT1.getJSONArray("phonetics")
            var audioObject = jsonArray1.getJSONObject(0)
            var audio = audioObject.getString("audio")
            audioUri = audio
            if (audioUri != null) {
                word_speak_button.visibility = View.VISIBLE
            }

            try {
                //Getting the meaning
                var jsonArray2 = jsonOBJECT1.getJSONArray("meanings")
                var jsonObject2 = jsonArray2.getJSONObject(0)
                var jsonArray3 = jsonObject2.getJSONArray("definitions")
                var jsonObject3 = jsonArray3.getJSONObject(0)
                var mean = jsonObject3.getString("definition")
                definition.text = mean.capitalize()
            } catch (e: Exception) {
                definition.text = " "
            }
            //Getting the origin
            try {
                var originWord = jsonOBJECT1.getString("origin")

                origin.text = originWord.capitalize()
            } catch (e: Exception) {
                origin.text = " "
            }

            try {
                //Getting the parts of speech
                var jsonArray2 = jsonOBJECT1.getJSONArray("meanings")
                var jsonObject2 = jsonArray2.getJSONObject(0)
                var partsofspeechWord = jsonObject2.getString("partOfSpeech")
                partsofSpeech.text = partsofspeechWord.capitalize()
            } catch (e: Exception) {
                partsofSpeech.text = " "
            }

            //Getting the example
            try {
                var jsonArray2 = jsonOBJECT1.getJSONArray("meanings")
                var jsonObject2 = jsonArray2.getJSONObject(0)
                var jsonArray3 = jsonObject2.getJSONArray("definitions")
                var jsonObject3 = jsonArray3.getJSONObject(0)
                var exampleWord = jsonObject3.getString("example")
                exampleWord = exampleWord + "."
                example.text = exampleWord.capitalize()
            } catch (e: Exception) {
                example.text = " "
            }
            scrollView.visibility = View.VISIBLE


        }, {
            search_word_text.setTextColor(ContextCompat.getColor(this, R.color.red))
            search_word_text.setBackgroundResource(R.drawable.search_layout_error)
            word_show.visibility = View.VISIBLE
            word_show.text = "No Such Word Found"
            word_show.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            var typeFace: Typeface? = ResourcesCompat.getFont(this, R.font.nunito_sans)
            word_show.typeface = typeFace
            word_speak_button.visibility = View.GONE
            scrollView.visibility = View.GONE

        })
        search_word_text.hideKeyboard()
        MySingleton.getInstance(this).addToRequestQueue(jsonArrayRequest)

    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)

    }


    //Network Connection Check
    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    private fun setBackgroundOfApp() {

        bitmap = loadImageBitmap(this)
        if (bitmap != null) {
            imageView.visibility = View.VISIBLE
            imageView.setImageBitmap(bitmap)
            definition.setTextColor(ContextCompat.getColor(this, R.color.white))
            partsofSpeech.setTextColor(ContextCompat.getColor(this, R.color.white))
            example.setTextColor(ContextCompat.getColor(this, R.color.white))
            origin.setTextColor(ContextCompat.getColor(this, R.color.white))
            cardViewDefinition.elevation = 0F
            cardViewExample.elevation = 0F
            cardViewPartsOfSpeech.elevation = 0F
            cardViewSource.elevation = 0F
            cardViewDefinition.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.transparent
                )
            )
            cardViewExample.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.transparent
                )
            )

            cardViewPartsOfSpeech.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.transparent
                )
            )
            cardViewSource.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.transparent
                )
            )
        } else {
            imageView.visibility = View.GONE
            cardViewSource.elevation = 10F
            cardViewExample.elevation = 10F
            cardViewPartsOfSpeech.elevation = 10F
            cardViewDefinition.elevation = 10F

            definition.setTextColor(ContextCompat.getColor(this, R.color.black))
            partsofSpeech.setTextColor(ContextCompat.getColor(this, R.color.black))
            example.setTextColor(ContextCompat.getColor(this, R.color.black))
            origin.setTextColor(ContextCompat.getColor(this, R.color.black))
            cardViewDefinition.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.white
                )
            )
            cardViewExample.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.white
                )
            )

            cardViewPartsOfSpeech.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.white
                )
            )
            cardViewSource.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.white
                )
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    //permission from popup denied
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun pickImageFromGallery() {
        //Intent to pick Image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /*
        Log.d("Height", "$height")
        Log.d("Height", "$width")
       */
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE && data != null) {
            var selectedImageUri = data.data
            var bitmap: Bitmap? =
                getBitmapFromUri(this, selectedImageUri)
            if (bitmap != null) {
                saveImageToInternalStorage(bitmap)
                setBackgroundOfApp()
            }

        }
    }


    @Throws(FileNotFoundException::class, IOException::class)
    fun getBitmapFromUri(ac: Activity, uri: Uri?): Bitmap? {
        var input: InputStream? = ac.contentResolver.openInputStream(uri!!)
        val onlyBoundsOptions = BitmapFactory.Options()
        onlyBoundsOptions.inJustDecodeBounds = true
        onlyBoundsOptions.inDither = true //optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
        input?.close()
        val originalWidth = onlyBoundsOptions.outWidth
        val originalHeight = onlyBoundsOptions.outHeight
        if (originalWidth == -1 || originalHeight == -1) return null
        //Image resolution is based on 480x800
        val hh = 800f //The height is set as 800f here
        val ww = 480f //Set the width here to 480f
        //Zoom ratio. Because it is a fixed scale, only one data of height or width is used for calculation
        var be = 1 //be=1 means no scaling
        if (originalWidth > originalHeight && originalWidth > ww) { //If the width is large, scale according to the fixed size of the width
            be = (originalWidth / ww).toInt()
        } else if (originalWidth < originalHeight && originalHeight > hh) { //If the height is high, scale according to the fixed size of the width
            be = (originalHeight / hh).toInt()
        }
        if (be <= 0) be = 1
        //Proportional compression
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inSampleSize = be //Set scaling
        bitmapOptions.inDither = true //optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
        input = ac.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
        input?.close()
        return bitmap?.let { compressImage(it) } //Mass compression again
    }

    private fun compressImage(image: Bitmap): Bitmap? {
        val baos = ByteArrayOutputStream()
        image.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            baos
        ) //Quality compression method, here 100 means no compression, store the compressed data in the BIOS
        var options = 100
        while (baos.toByteArray().size / 1024 > 100) {  //Cycle to determine if the compressed image is greater than 100kb, greater than continue compression
            baos.reset() //Reset the BIOS to clear it
            //First parameter: picture format, second parameter: picture quality, 100 is the highest, 0 is the worst, third parameter: save the compressed data stream
            image.compress(
                Bitmap.CompressFormat.JPEG,
                options,
                baos
            ) //Here, the compression options are used to store the compressed data in the BIOS
            options -= 10 //10 less each time
        }
        val isBm =
            ByteArrayInputStream(baos.toByteArray()) //Store the compressed data in ByteArrayInputStream
        return BitmapFactory.decodeStream(isBm, null, null)
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap) {
        try {
            val fos: FileOutputStream = openFileOutput("background.png", Context.MODE_PRIVATE)
            //Writing the bitmap to the output stream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Something went Wrong!", Toast.LENGTH_SHORT).show()
        }
    }

    //To retrieve the image
    private fun loadImageBitmap(context: Context): Bitmap? {
        var fileInputStream: FileInputStream? = null
        var bitmap: Bitmap? = null;
        try {
            fileInputStream = context.openFileInput("background.png");
            bitmap = BitmapFactory.decodeStream(fileInputStream);
            fileInputStream.close();
        } catch (e: Exception) {

        }
        return bitmap;
    }

    private fun deleteFileFromInternalStorage(filename: String): Boolean {
        return try {
            deleteFile(filename)
        } catch (e: Exception) {
            Toast.makeText(this, "Something went Wrong!", Toast.LENGTH_SHORT).show()
            false
        }
    }


}