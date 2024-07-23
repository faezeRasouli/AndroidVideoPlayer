package com.arezoonazer.player.view

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.nio.ByteBuffer
import java.security.MessageDigest

class GlideThumbnailTransformations(private val position: Long) : BitmapTransformation() {

        companion object {
            private const val MAX_LINES = 10
            private const val MAX_COLUMNS = 10
            private const val THUMBNAILS_EACH = 5000 // milliseconds
        }

        private var x: Int = 0
        private var y: Int = 0

        init {
            val square = (position / THUMBNAILS_EACH).toInt()
            y = square / MAX_LINES
            x = square % MAX_COLUMNS
        }

        private fun getX(): Int {
            return x
        }

        private fun getY(): Int {
            return y
        }

        override fun transform(pool: BitmapPool, toTransform: Bitmap,
                               outWidth: Int, outHeight: Int): Bitmap {
            val width = toTransform.width / MAX_COLUMNS
            val height = toTransform.height / MAX_LINES
            return Bitmap.createBitmap(toTransform, x * width, y * height, width, height)
        }

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            val data = ByteBuffer.allocate(8)
                .putInt(x)
                .putInt(y)
                .array()
            messageDigest.update(data)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GlideThumbnailTransformations

            if (x != other.x) return false
            if (y != other.y) return false

            return true
        }

        override fun hashCode(): Int {
            var result = x
            result = 31 * result + y
            return result
        }
    }
