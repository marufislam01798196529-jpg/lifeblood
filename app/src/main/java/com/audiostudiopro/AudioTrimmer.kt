package com.audiostudiopro

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.nio.ByteBuffer

/**
 * Compressed-sample trim for compatible AAC/AMR audio tracks.
 * It avoids full PCM decoding and uses one bounded direct buffer.
 */
object AudioTrimmer {
    fun trim(context: Context, source: Uri, destination: Uri, startUs: Long, endUs: Long) {
        require(startUs >= 0L && endUs > startUs) { "Invalid trim range" }

        val input = context.contentResolver.openFileDescriptor(source, "r")
            ?: error("Cannot open selected source")
        val output = context.contentResolver.openFileDescriptor(destination, "w")
            ?: error("Cannot create output file")

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var started = false
        try {
            extractor.setDataSource(input.fileDescriptor)
            var trackIndex = -1
            var trackFormat: MediaFormat? = null

            for (index in 0 until extractor.trackCount) {
                val candidate = extractor.getTrackFormat(index)
                val mime = candidate.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = index
                    trackFormat = candidate
                    break
                }
            }

            require(trackIndex >= 0 && trackFormat != null) { "No audio track found" }
            val format = trackFormat!!
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            require(mime == "audio/mp4a-latm" || mime == "audio/3gpp" || mime == "audio/amr-wb") {
                "This fast trim currently supports AAC/AMR tracks only."
            }

            muxer = MediaMuxer(output.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            extractor.selectTrack(trackIndex)
            val destinationTrack = muxer.addTrack(format)
            muxer.start()
            started = true

            val requestedBuffer = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 256 * 1024
            val bufferSize = requestedBuffer.coerceIn(64 * 1024, 1024 * 1024)
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val info = MediaCodec.BufferInfo()

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            var samplesWritten = 0
            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime < 0L || sampleTime >= endUs) break

                if (sampleTime >= startUs) {
                    info.set(0, sampleSize, sampleTime - startUs, extractor.sampleFlags)
                    buffer.position(0)
                    buffer.limit(sampleSize)
                    muxer.writeSampleData(destinationTrack, buffer, info)
                    samplesWritten++
                }
                if (!extractor.advance()) break
            }

            require(samplesWritten > 0) { "No samples found in the selected time range" }
            muxer.stop()
            started = false
            muxer.release()
            muxer = null
            output.fileDescriptor.sync()
            require(output.statSize > 0L) { "The exported file is empty" }
        } finally {
            if (started) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            extractor.release()
            input.close()
            output.close()
        }
    }
}
