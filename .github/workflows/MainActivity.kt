package com.example.scanconnect

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class MainActivity : AppCompatActivity() {
    private data class ScanPage(val bitmap: Bitmap, var selected: Boolean = true)
    private val pages = mutableListOf<ScanPage>()
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var pageList: LinearLayout

    private val galleryPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        var added = 0
        uris.forEach { uri -> try { contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it)?.let { bmp -> pages.add(ScanPage(bmp)); added++ } } } catch (_: Exception) {} }
        renderPages(); say(if (added > 0) "$added page(s) added." else "No image added.")
    }
    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            var added = 0
            scan?.pages?.forEach { page -> try { contentResolver.openInputStream(page.imageUri)?.use { BitmapFactory.decodeStream(it)?.let { bmp -> pages.add(ScanPage(bmp)); added++ } } } catch (_: Exception) {} }
            renderPages(); say(if (added > 0) "$added scanned page(s) added." else "No scanned page returned.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 16, 18, 16); setBackgroundColor(Color.rgb(247,249,253)) }
        showHome()
    }
    private fun title(text: String, sub: String? = null) {
        root.removeAllViews()
        root.addView(TextView(this).apply { this.text=text; textSize=26f; gravity=Gravity.CENTER; setTextColor(Color.rgb(25,54,110)); setPadding(0,8,0,5) })
        if (sub != null) root.addView(TextView(this).apply { this.text=sub; textSize=14f; gravity=Gravity.CENTER; setTextColor(Color.DKGRAY); setPadding(0,0,0,12) })
    }
    private fun showHome() {
        title("ScanConnect", "Offline document scanner • image tools • PDF")
        button(root,"📄  Scan Document") { showDocuments() }
        button(root,"💬  Online Chat & Calls Setup") { startActivity(Intent(this, SetupInfoActivity::class.java)) }
        setContentView(root)
    }
    private fun showDocuments() {
        title("Scan Documents", "Add multiple pages, edit, select, save and share")
        val scroll=ScrollView(this); val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        status=TextView(this).apply { text="Add pages using camera or gallery. Pages appear below."; textSize=14f; setTextColor(Color.DKGRAY); setPadding(4,8,4,10) }; body.addView(status)
        button(body,"📷  Camera Scan / Auto Crop — Add Pages") { startScanner() }
        button(body,"🖼  Gallery se multiple pages add karein") { galleryPicker.launch(arrayOf("image/*")) }
        button(body,"☑️  Select All") { pages.forEach { it.selected=true }; renderPages() }
        button(body,"☐  Deselect All") { pages.forEach { it.selected=false }; renderPages() }
        button(body,"⚫  Black & White — selected pages") { transformSelected(true) }
        button(body,"✨  Extra Clear / Enhance — selected pages") { transformSelected(false) }
        button(body,"🔄  Rotate — selected pages") { rotateSelected() }
        button(body,"💾  Save selected as Gallery images") { saveImages() }
        button(body,"📄  Save selected as one PDF") { savePdf() }
        button(body,"↗  Share selected pages as PDF") { sharePdf() }
        button(body,"🗑  Remove selected pages") { pages.removeAll { it.selected }; renderPages(); say("Selected pages removed.") }
        body.addView(TextView(this).apply { text="Scanned Pages"; textSize=19f; setTextColor(Color.DKGRAY); setPadding(4,14,4,8) })
        pageList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; body.addView(pageList)
        renderPages(); scroll.addView(body)
        root.removeAllViews(); root.addView(TextView(this).apply { text="Scan Documents"; textSize=24f; gravity=Gravity.CENTER; setTextColor(Color.rgb(25,54,110)); setPadding(0,0,0,8) })
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f)); button(root,"←  Main Page") { showHome() }; setContentView(root)
    }
    private fun button(parent: LinearLayout, label: String, action: () -> Unit) {
        parent.addView(Button(this).apply { text=label; isAllCaps=false; textSize=15f; setOnClickListener { action() } },LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin=6 })
    }
    private fun renderPages() {
        if (!::pageList.isInitialized) return
        pageList.removeAllViews()
        if (pages.isEmpty()) { pageList.addView(TextView(this).apply { text="Abhi koi page add nahi hua."; setPadding(8,12,8,20) }); return }
        pages.forEachIndexed { i,p ->
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(6,8,6,8); setBackgroundColor(Color.WHITE) }
            row.addView(CheckBox(this).apply { text="Page ${i+1}"; isChecked=p.selected; setOnCheckedChangeListener { _,checked -> p.selected=checked } },LinearLayout.LayoutParams(0,-2,1f))
            row.addView(ImageView(this).apply { setImageBitmap(p.bitmap); scaleType=ImageView.ScaleType.CENTER_CROP },LinearLayout.LayoutParams(100,125))
            pageList.addView(row,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=5 })
        }
    }
    private fun startScanner() {
        val options=GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(50).setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,GmsDocumentScannerOptions.RESULT_FORMAT_PDF).setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(this).addOnSuccessListener { sender -> scannerLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(sender).build()) }.addOnFailureListener { e -> say("Scanner start nahi hua: ${e.localizedMessage}") }
    }
    private fun chosen()=pages.filter { it.selected }
    private fun transformSelected(bw:Boolean) {
        val selected=chosen(); if(selected.isEmpty()){say("Pehle pages select karein.");return}
        selected.forEach { item -> val src=item.bitmap; val out=Bitmap.createBitmap(src.width,src.height,Bitmap.Config.ARGB_8888); val px=IntArray(src.width*src.height); src.getPixels(px,0,src.width,0,0,src.width,src.height)
            for(i in px.indices){val c=px[i];var r=Color.red(c);var g=Color.green(c);var b=Color.blue(c);if(bw){val gray=(.299*r+.587*g+.114*b).toInt().coerceIn(0,255);val v=(((gray-128)*1.45)+128).toInt().coerceIn(0,255);r=v;g=v;b=v}else{r=(((r-128)*1.28)+137).toInt().coerceIn(0,255);g=(((g-128)*1.28)+137).toInt().coerceIn(0,255);b=(((b-128)*1.28)+137).toInt().coerceIn(0,255)};px[i]=Color.argb(Color.alpha(c),r,g,b)};out.setPixels(px,0,src.width,0,0,src.width,src.height);item.bitmap=out }
        renderPages();say(if(bw) "Black & White applied." else "Extra Clear applied.")
    }
    private fun rotateSelected(){val selected=chosen();if(selected.isEmpty()){say("Pehle pages select karein.");return};selected.forEach{item->val src=item.bitmap;val out=Bitmap.createBitmap(src.height,src.width,Bitmap.Config.ARGB_8888);val c=Canvas(out);c.rotate(90f,out.width/2f,out.height/2f);c.drawBitmap(src,(out.width-src.width)/2f,(out.height-src.height)/2f,Paint(Paint.FILTER_BITMAP_FLAG));item.bitmap=out};renderPages();say("Selected pages rotated.")}
    private fun saveImages(){val selected=chosen();if(selected.isEmpty()){say("Pehle pages select karein.");return};var saved=0;selected.forEach{p->try{val v=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"ScanConnect_${System.currentTimeMillis()}_${saved+1}.jpg");put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");if(Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/ScanConnect")};val u=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);if(u!=null)contentResolver.openOutputStream(u)?.use{p.bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)};saved++}catch(_:Exception){}};say("$saved image(s) saved in Pictures/ScanConnect.")}
    private fun makePdf():android.net.Uri? {val selected=chosen();if(selected.isEmpty()){say("Pehle pages select karein.");return null};var doc:PdfDocument?=null;return try{doc=PdfDocument();selected.forEachIndexed{i,p->val b=p.bitmap;val info=PdfDocument.PageInfo.Builder(595,842,i+1).create();val page=doc.startPage(info);val s=minOf(555f/b.width,790f/b.height);val w=(b.width*s).toInt();val h=(b.height*s).toInt();page.canvas.drawBitmap(Bitmap.createScaledBitmap(b,w,h,true),(595-w)/2f,(842-h)/2f,Paint(Paint.FILTER_BITMAP_FLAG));doc.finishPage(page)};val v=ContentValues().apply{put(MediaStore.Downloads.DISPLAY_NAME,"ScanConnect_${System.currentTimeMillis()}.pdf");put(MediaStore.Downloads.MIME_TYPE,"application/pdf");if(Build.VERSION.SDK_INT>=29)put(MediaStore.Downloads.RELATIVE_PATH,"Download")};val uri=contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v)?:return null;contentResolver.openOutputStream(uri)?.use{doc.writeTo(it)};uri}catch(e:Exception){say("PDF failed: ${e.localizedMessage}");null}finally{doc?.close()}}
    private fun savePdf(){if(makePdf()!=null)say("Selected pages ki single PDF Downloads mein save ho gayi.")}
    private fun sharePdf(){val uri=makePdf()?:return;val intent=Intent(Intent.ACTION_SEND).apply{type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)};startActivity(Intent.createChooser(intent,"Share PDF"))}
    private fun say(msg:String){if(::status.isInitialized)status.text=msg;Toast.makeText(this,msg,Toast.LENGTH_LONG).show()}
}
