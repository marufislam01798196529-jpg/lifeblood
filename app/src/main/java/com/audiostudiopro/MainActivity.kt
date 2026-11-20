package com.audiostudiopro

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var inputUri: Uri? = null
    private var player: MediaPlayer? = null
    private lateinit var status: TextView
    private lateinit var startField: EditText
    private lateinit var endField: EditText
    private lateinit var trimButton: Button
    private var pendingStartUs = 0L
    private var pendingEndUs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        startField = findViewById(R.id.startSeconds)
        endField = findViewById(R.id.endSeconds)
        trimButton = findViewById(R.id.trimButton)

        findViewById<Button>(R.id.importButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQUEST_AUDIO)
        }
        findViewById<Button>(R.id.playButton).setOnClickListener { playSelected() }
        trimButton.setOnClickListener { validateAndAskDestination() }
    }

    @Deprecated("Legacy result callback retained for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_AUDIO && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                inputUri = uri
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) { }
                status.text = "Audio selected\n${uri.lastPathSegment ?: "Selected audio"}"
            }
        } else if (requestCode == REQUEST_SAVE && resultCode == RESULT_OK) {
            val output = data?.data ?: return
            val source = inputUri ?: return
            val from = pendingStartUs
            val to = pendingEndUs
            trimButton.isEnabled = false
            status.text = "Trimming audio…"
            worker.execute {
                val result = runCatching { AudioTrimmer.trim(this, source, output, from, to) }
                runOnUiThread {
                    trimButton.isEnabled = true
                    result.onSuccess {
                        status.text = "Trim completed. Output file verified."
                        Toast.makeText(this, "Audio saved successfully", Toast.LENGTH_LONG).show()
                    }.onFailure { error ->
                        status.text = "Trim failed: ${error.message ?: "Unsupported file"}"
                        Toast.makeText(this, "Could not trim this audio", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun validateAndAskDestination() {
        if (inputUri == null) {
            status.text = "Import an audio file first."
            return
        }
        val start = startField.text.toString().toDoubleOrNull()
        val end = endField.text.toString().toDoubleOrNull()
        if (start == null || end == null || !start.isFinite() || !end.isFinite() || start < 0.0 || end <= start) {
            status.text = "Enter valid times. End time must be greater than start time."
            return
        }
        pendingStartUs = (start * 1_000_000.0).toLong()
        pendingEndUs = (end * 1_000_000.0).toLong()
        val saveIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/mp4"
            putExtra(Intent.EXTRA_TITLE, "AudioStudio_trimmed.m4a")
        }
        startActivityForResult(saveIntent, REQUEST_SAVE)
    }

    private fun playSelected() {
        val uri = inputUri
        if (uri == null) {
            status.text = "Import an audio file first."
            return
        }
        runCatching {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(this@MainActivity, uri)
                setOnPreparedListener {
                    it.start()
                    status.text = "Playing selected audio…"
                }
                setOnCompletionListener { status.text = "Playback finished." }
                setOnErrorListener { _, _, _ ->
                    status.text = "Playback failed. This format may not be supported."
                    true
                }
                prepareAsync()
            }
        }.onFailure { status.text = "Unable to open this audio file." }
    }

    override fun onStop() {
        super.onStop()
        runCatching { player?.pause() }
    }

    override fun onDestroy() {
        runCatching { player?.release() }
        player = null
        worker.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_AUDIO = 100
        private const val REQUEST_SAVE = 101
    }
}
