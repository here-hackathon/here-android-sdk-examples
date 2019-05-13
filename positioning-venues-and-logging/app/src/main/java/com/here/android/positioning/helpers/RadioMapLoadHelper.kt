/*
 * Copyright (c) 2011-2018 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.here.android.positioning.helpers

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Pair

import com.here.android.mpa.venues3d.Venue
import com.here.android.positioning.radiomap.RadioMapLoader
import java.util.LinkedHashSet

/**
 * Helper class for handling radio map downloading in sequence.
 */
class RadioMapLoadHelper
/**
 * Constructor
 * @param api Radio map loader
 * @param listener Helper listener
 */
(// Radio map loader
        private val mApi: RadioMapLoader, // Helper listener
        private val mListener: Listener) {

    // Android handler
    private val mHandler: Handler

    // Loader
    private val mLoader = Loader()

    // Loader implementation
    private inner class Loader : RadioMapLoader.Listener, Runnable {

        // Current venue queue
        private val mLoadQue = LinkedHashSet<Venue>()

        // Active download job
        private var mCurrentJob: Pair<Venue, RadioMapLoader.Job>? = null

        override fun onProgressUpdated(job: RadioMapLoader.Job) {
            Log.i(TAG, "onProgressUpdated: " + mCurrentJob!!.first.id + ", progress: " + job.progress)
            mListener.onProgress(mCurrentJob!!.first, job.progress)
        }

        override fun onCompleted(job: RadioMapLoader.Job) {
            if (mCurrentJob == null) {
                Log.i(TAG, "onCompleted: status: " + job.status)
            } else {
                Log.i(TAG, "onCompleted: " + mCurrentJob!!.first.id + ", status: " + job.status)
                mListener.onCompleted(mCurrentJob!!.first, job.status)
                mCurrentJob = null
            }
            schedule()
        }

        override fun run() {
            if (mCurrentJob != null) {
                Log.w(TAG, "Loader.run: downloading already in process")
                return
            }
            val it = mLoadQue.iterator()
            if (!it.hasNext()) {
                Log.v(TAG, "Loader.run: everything downloaded")
                return
            }
            try {
                val venue = it.next()
                val job = mApi.load(this, venue)
                when (job.status) {
                    RadioMapLoader.Status.OK, RadioMapLoader.Status.PENDING -> mCurrentJob = Pair(venue, job)
                    else -> {
                        mListener.onError(venue, job.status)
                        schedule()
                    }
                }
            } finally {
                it.remove()
            }
        }

        // Load venue
        internal fun load(venue: Venue): Boolean {
            return if (mLoadQue.add(venue)) {
                schedule()
            } else false
        }

        // Cancels current download and clears download que.
        internal fun cancel() {
            mLoadQue.clear()
            mHandler.removeCallbacks(this)
            if (mCurrentJob != null) {
                mCurrentJob!!.second.cancel()
                mCurrentJob = null
            }
        }

        // Schedule downloading
        private fun schedule(): Boolean {
            if (mCurrentJob != null) {
                return true
            }
            mHandler.removeCallbacks(this)
            return mHandler.post(this)
        }
    }

    /**
     * Helper listener interface definition.
     */
    interface Listener {
        fun onError(venue: Venue, status: RadioMapLoader.Status)
        fun onProgress(venue: Venue, progress: Int)
        fun onCompleted(venue: Venue, status: RadioMapLoader.Status)
    }

    init {
        mHandler = Handler(Looper.getMainLooper())
    }

    /**
     * Que venue for loading
     * @param venue Venue instance
     * @return True if venue queued successfully
     */
    fun load(venue: Venue): Boolean {
        Log.v(TAG, "load: " + venue.id)
        return mLoader.load(venue)
    }

    /**
     * Cancel downloads and clear download que
     */
    fun cancel() {
        Log.v(TAG, "cancel")
        mLoader.cancel()
    }

    companion object {

        private val TAG = "RadioMapLoadHelper"
    }

}
