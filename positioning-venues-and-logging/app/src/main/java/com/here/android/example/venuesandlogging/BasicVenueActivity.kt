/*
 * Copyright (c) 2011-2019 HERE Europe B.V.
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

package com.here.android.example.venuesandlogging

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.GeoPosition
import com.here.android.mpa.common.LocationDataSourceHERE
import com.here.android.mpa.common.OnEngineInitListener
import com.here.android.mpa.common.PositioningManager
import com.here.android.mpa.common.PositioningManager.OnPositionChangedListener
import com.here.android.mpa.common.ViewObject
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.Map.OnTransformListener
import com.here.android.mpa.mapping.MapGesture.OnGestureListener
import com.here.android.mpa.mapping.MapState
import com.here.android.mpa.routing.RouteOptions
import com.here.android.mpa.venues3d.BaseLocation
import com.here.android.mpa.venues3d.CombinedRoute
import com.here.android.mpa.venues3d.DeselectionSource
import com.here.android.mpa.venues3d.Level
import com.here.android.mpa.venues3d.LevelLocation
import com.here.android.mpa.venues3d.OutdoorLocation
import com.here.android.mpa.venues3d.RoutingController
import com.here.android.mpa.venues3d.Space
import com.here.android.mpa.venues3d.SpaceLocation
import com.here.android.mpa.venues3d.Venue
import com.here.android.mpa.venues3d.VenueController
import com.here.android.mpa.venues3d.VenueInfo
import com.here.android.mpa.venues3d.VenueMapFragment
import com.here.android.mpa.venues3d.VenueMapFragment.VenueListener
import com.here.android.mpa.venues3d.VenueRouteOptions
import com.here.android.mpa.venues3d.VenueService
import com.here.android.positioning.StatusListener
import com.here.android.positioning.helpers.RadioMapLoadHelper
import com.here.android.positioning.radiomap.RadioMapLoader
import com.here.android.ui.ZumtobelViewModel
import kotlinx.android.synthetic.main.activity_main.header
import kotlinx.android.synthetic.main.activity_main.lightButton
import kotlinx.android.synthetic.main.activity_main.myLocationButton
import kotlinx.android.synthetic.main.activity_main.secondarySubtitle
import kotlinx.android.synthetic.main.activity_main.spaceName

import java.io.File
import java.lang.ref.WeakReference

class BasicVenueActivity : AppCompatActivity(), VenueListener, OnGestureListener, OnPositionChangedListener, OnTransformListener, RoutingController.RoutingControllerListener {

    // map embedded in the map fragment
    private var mMap: Map? = null

    // Venue map fragment embedded in this activity
    private var mVenueMapFragment: VenueMapFragment? = null

    // Venue map fragment routing controller
    internal var mRoutingController: RoutingController? = null

    // True if route is shown
    private var mRouteShown: Boolean = false

    // Widget for selecting floors of the venue
    private var mFloorsControllerWidget: FloorsControllerWidget? = null

    // positioning manager instance
    private var mPositioningManager: PositioningManager? = null

    // Flag for using indoor positioning
    private var mIndoorPositioning: Boolean = true

    // Flag for using indoor routing
    private var mIndoorRouting: Boolean = false

    // HERE location data source instance
    private var mHereLocation: LocationDataSourceHERE? = null

    // Instance of Venue Service
    private var mVenueService: VenueService? = null

    // Current activity
    private var mActivity: BasicVenueActivity? = null

    // Location method currently in use
    private var mLocationMethod: PositioningManager.LocationMethod? = null

    // Menu
    private var add_private_venues: MenuItem? = null
    private var remove_private_venues: MenuItem? = null
    private var follow_position: MenuItem? = null
    private var add_indoor_to_position: MenuItem? = null
    private var indoor_routing: MenuItem? = null

    // Last known map center
    private var mLastMapCenter: GeoCoordinate? = null

    // Last received position update
    private var mLastReceivedPosition: GeoPosition? = null

    // flag that indicates whether maps is being transformed
    private var mTransforming: Boolean = false

    // Flag for usage of Private Venues context
    private var mPrivateVenues: Boolean = true

    // Flag for user control over the map
    private var mUserControl: Boolean = true

    // callback that is called when transforming ends
    private var mPendingUpdate: Runnable? = null

    /** Positioning status listener.  */
    private val mPositioningStatusListener = object : StatusListener {

        override fun onOfflineModeChanged(offline: Boolean) {
            Log.v(TAG, "StatusListener.onOfflineModeChanged: %b", offline)
        }

        override fun onAirplaneModeEnabled() {
            Log.v(TAG, "StatusListener.onAirplaneModeEnabled")
        }

        override fun onWifiScansDisabled() {
            Log.v(TAG, "StatusListener.onWifiScansDisabled")
        }

        override fun onBluetoothDisabled() {
            Log.v(TAG, "StatusListener.onBluetoothDisabled")
        }

        override fun onCellDisabled() {
            Log.v(TAG, "StatusListener.onCellDisabled")
        }

        override fun onGnssLocationDisabled() {
            Log.v(TAG, "StatusListener.onGnssLocationDisabled")
        }

        override fun onNetworkLocationDisabled() {
            Log.v(TAG, "StatusListener.onNetworkLocationDisabled")
        }

        override fun onServiceError(error: StatusListener.ServiceError) {
            Log.v(TAG, "StatusListener.onServiceError: %s", error)
        }

        override fun onPositioningError(error: StatusListener.PositioningError) {
            Log.v(TAG, "StatusListener.onPositioningError: %s", error)
        }

        override fun onWifiIndoorPositioningNotAvailable() {
            Log.v(TAG, "StatusListener.onWifiIndoorPositioningNotAvailable")
        }

        override fun onWifiIndoorPositioningDegraded() {
            // called when running on Android 9.0 (Pie) or newer
        }
    }

    // Venue load listener to request radio map loading for the loaded venue.
    private val mVenueLoadListener = VenueService.VenueLoadListener { venue, venueInfo, venueLoadStatus ->
        if (venueLoadStatus != VenueService.VenueLoadStatus.FAILED) {
            Log.v(TAG, "onVenueLoadCompleted: loading radio maps for " + venue.id)
            mRadioMapLoader!!.load(venue)
        }
    }

    // Radio map loader helper instance.
    private var mRadioMapLoader: RadioMapLoadHelper? = null

    // Latest selected venue
    private var mSelectedVenue: Venue? = null

    // Latest selected space
    private var mSelectedSpace: Space? = null

    // Google has deprecated android.app.Fragment class. It is used in current SDK implementation.
    // Will be fixed in future SDK version.
    private val mapFragment: VenueMapFragment
        get() = fragmentManager.findFragmentById(R.id.mapfragment) as VenueMapFragment

    /**
     * An example list view adapter for floor switcher
     */
    inner class VenueFloorAdapter(context: Context, levels: List<Level>, private val mFloorItem: Int,
                                  private val mFloorName: Int, private val mFloorGroundSep: Int) : BaseAdapter() {

        internal val mLevels: Array<Level?>
        private val mInflater: LayoutInflater

        init {
            mLevels = arrayOfNulls(levels.size)
            for (i in levels.indices) {
                mLevels[levels.size - i - 1] = levels[i]
            }
            mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        override fun getCount(): Int {
            return mLevels.size
        }

        override fun getItem(position: Int): Any {
            return mLevels[position]!!
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        fun getLevelIndex(level: Level): Int {
            for (i in mLevels.indices) {
                if (mLevels[i]?.floorNumber == level.floorNumber) {
                    return i
                }
            }

            return NOT_FOUND
        }

        /**
         * Changing font in floor changing widget depending on text size
         * @param text shown in floor changing widget
         */
        fun updateFont(text: TextView) {
            val size = text.text.length
            when (size) {
                1 -> text.textSize = 24f
                2 -> text.textSize = 21f
                3 -> text.textSize = 18f
                4 -> text.textSize = 15f
                else -> text.textSize = 12f
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var mView: View? = convertView
            if (mView == null)
                mView = mInflater.inflate(mFloorItem, null)
            val text = mView!!.findViewById<View>(mFloorName) as TextView
            text.text = mLevels[position]?.floorSynonym
            updateFont(text)
            val showSep = if (mLevels[position]?.floorNumber == 0 && position != mLevels.size - 1)
                View.VISIBLE
            else
                View.INVISIBLE
            val separator = mView.findViewById<View>(mFloorGroundSep)
            separator.visibility = showSep
            return mView
        }
    }

    /** An example implementation for floor controller widget. Needs to implement
     * VenueMapFragment.VenueListener interface in order
     * to update floor selection
     */
    inner class FloorsControllerWidget(private val mActivity: Context, private val mVenueMapFragment: VenueMapFragment,
                                       private val mFloorListView: ListView, private val mFloorItem: Int, private val mFloorName: Int, private val mFloorGroundSep: Int) : AdapterView.OnItemClickListener, VenueListener {

        init {
            mVenueMapFragment.addListener(this)
            mFloorListView.onItemClickListener = this

            if (mVenueMapFragment.selectedVenue != null) {
                onVenueSelected(mVenueMapFragment.selectedVenue)
            }
        }

        override fun onItemClick(arg0: AdapterView<*>, view: View, index: Int, arg3: Long) {
            view.isSelected = true
            val controller = mVenueMapFragment
                    .getVenueController(mVenueMapFragment.selectedVenue)
            if (controller != null) {
                val levelIndex = controller.venue.levels.size - 1 - index
                val level = controller.venue.levels[levelIndex]
                controller.selectLevel(level)
            }
        }

        override fun onVenueSelected(venue: Venue?) {
            mSelectedVenue = venue
            mFloorListView.adapter = VenueFloorAdapter(mActivity, venue!!.levels,
                    mFloorItem, mFloorName, mFloorGroundSep)
            updateSelectedLevel(mVenueMapFragment.getVenueController(venue)!!)

            mFloorListView.visibility = View.VISIBLE
        }

        /**
         * Updating controller view based on level selection
         * @param controller to be updated
         */
        private fun updateSelectedLevel(controller: VenueController) {
            val selectedLevel = controller.selectedLevel
            if (selectedLevel != null) {
                val pos = (mFloorListView.adapter as VenueFloorAdapter)
                        .getLevelIndex(selectedLevel)
                if (pos != -1) {
                    mFloorListView.setSelection(pos)
                    mFloorListView.smoothScrollToPosition(pos)
                    mFloorListView.setItemChecked(pos, true)
                }
            }
        }

        override fun onVenueDeselected(venue: Venue, source: DeselectionSource) {
            mSelectedVenue = null
            mSelectedSpace = null
            mFloorListView.adapter = null
            mFloorListView.visibility = View.INVISIBLE
            mVenueMapFragment.map.tilt = 0f
            clearRoute()
        }

        override fun onFloorChanged(venue: Venue, oldLevel: Level, newLevel: Level) {
            val controller = mVenueMapFragment.getVenueController(venue)
            if (controller != null) {
                updateSelectedLevel(controller)
                mUserControl = true
                invalidateOptionsMenu()
            }
        }

        override fun onVenueTapped(venue: Venue, x: Float, y: Float) {}

        override fun onSpaceSelected(venue: Venue, space: Space) {
            clearRoute()
            mSelectedSpace = space
            showOrHideRoutingButton()
            header.visibility = View.VISIBLE
            spaceName.text = space.content.name

            var roomInfo = ""

            zumtobelViewModel.devicesLiveData.observe(this@BasicVenueActivity, Observer { devices ->
                Log.d("BCX2019", "Total number of devices: ${devices?.size}")

                devices?.forEach {device ->
                    if (device.type.equals("SENSOR")) {
                        var value = ""
                        when (device.name) {
                            "VOC" -> value = device.vocCapability?.voc.toString()
                            "CO2" -> value = device.co2Capability?.co2.toString()
                            "light sensor" -> value = device.brightnessCapability?.brightness.toString()
                            "presence left", "presence right" -> value = device.presenceCapability?.presence.toString()
                            "humidity" -> value = device.humidityCapability?.humidity.toString()
                            "temp" -> value = device.temperatureCapability?.temperature.toString()
                        }
                        if (value.isNotEmpty()) roomInfo += "${device.name}: ${value}\n"
                    }
                }
                secondarySubtitle.text = roomInfo
            })
        }

        override fun onSpaceDeselected(venue: Venue, space: Space) {
            clearRoute()
            mSelectedSpace = null
            showOrHideRoutingButton()
        }

        override fun onVenueVisibleInViewport(venue: Venue?, visible: Boolean) {}
    }

    /**
     * Clears the route if shown
     */
    private fun clearRoute() {
        if (mRouteShown) {
            if (mRoutingController != null) {
                mRoutingController!!.hideRoute()
            }
            mRouteShown = false
        }
    }

    /**
     * Show or hide indoor routing button
     */
    fun showOrHideRoutingButton() {
        val showRouteButton = findViewById<Button>(R.id.buttonShowRoute)
        if (showRouteButton != null) {
            if (mSelectedSpace != null && mIndoorRouting) {
                showRouteButton.visibility = View.VISIBLE
            } else {
                showRouteButton.visibility = View.GONE
            }
        }
    }

    private lateinit var zumtobelViewModel: ZumtobelViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable logging...
        Log.mEnabled = true

        setContentView(R.layout.activity_main)
        // checking dynamically controlled permissions
        if (hasPermissions(this, *RUNTIME_PERMISSIONS)) {
            startVenueMaps()
        } else {
            ActivityCompat
                    .requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS)
        }

        myLocationButton.setOnClickListener {
            followPosition()
        }

        zumtobelViewModel = ViewModelProviders.of(this).get(ZumtobelViewModel::class.java)

        zumtobelViewModel.fetchDevices()

        var intensity = 100.0
        lightButton.setOnClickListener {
            zumtobelViewModel.changeLightIntensity("5acc063b-e3df-43f0-903b-7b9450e9c4c2", intensity)
            intensity = 100 - intensity
        }
    }

    override fun onPause() {
        Log.v(TAG, "onPause")
        super.onPause()
    }

    override fun onResume() {
        Log.v(TAG, "onResume")
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onBackPressed() {
        if (mRouteShown) {
            clearRoute()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        add_private_venues = menu.findItem(R.id.action_add_private_venues)
        remove_private_venues = menu.findItem(R.id.action_remove_private_venues)
        follow_position = menu.findItem(R.id.follow_position)
        add_indoor_to_position = menu.findItem(R.id.add_indoor_to_position)
        indoor_routing = menu.findItem(R.id.indoor_routing)
        if (!mPrivateVenues) {
            add_private_venues!!.isVisible = true
            remove_private_venues!!.isVisible = false
        } else {
            add_private_venues!!.isVisible = false
            remove_private_venues!!.isVisible = true
        }
//        if (!mUserControl) {
//            follow_position!!.isChecked = true
//        } else {
//            follow_position!!.isChecked = false
//        }
        if (mIndoorPositioning) {
            add_indoor_to_position!!.isChecked = true
        } else {
            add_indoor_to_position!!.isChecked = false
        }
        if (mIndoorRouting) {
            indoor_routing!!.isChecked = true
        } else {
            indoor_routing!!.isChecked = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.v(TAG, "onOptionsItemSelected")
        if (mHereLocation == null) {
            return false
        }
        // Handle item selection
        when (item.itemId) {
            R.id.action_add_private_venues, R.id.action_remove_private_venues -> {
                changeVenuesContent()
                return true
            }
            R.id.follow_position -> {
                followPosition()
                return true
            }
            R.id.add_indoor_to_position -> {
                indoorToPosition()
                return true
            }
            R.id.indoor_routing -> {
                indoorRouting()
                showOrHideRoutingButton()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Changing venue map content by adding or removing private venues support
     */
    private fun changeVenuesContent() {
        Log.v(TAG, "changeVenuesContent")
        if (!mPrivateVenues) {
            mPrivateVenues = true
            Log.v(TAG, "Private Venues content added")
        } else {
            mPrivateVenues = false
            Log.v(TAG, "Private Venues content removed")
        }
        // Reinitialization of the map
        refreshMapView()
    }

    /**
     * Option for map to follow position updates
     */
    private fun followPosition() {
        Log.v(TAG, "followPosition")
        if (mUserControl) {
            mUserControl = false
            mMap!!.setCenter(mLastMapCenter, Map.Animation.NONE)
            Log.v(TAG, "Following position enabled")
        } else {
            mUserControl = true
            Log.v(TAG, "Following position disabled")
        }
        invalidateOptionsMenu()
    }

    /**
     * Adding indoor position to position manager
     */
    private fun indoorToPosition() {
        Log.v(TAG, "indoorToPosition")
        if (mIndoorPositioning) {
            mIndoorPositioning = false
            Log.v(TAG, "Indoor positioning removed")
        } else {
            mIndoorPositioning = true
            Log.v(TAG, "Indoor positioning added")
        }
        stopPositioningUpdates()
        startPositionUpdates()
        invalidateOptionsMenu()
    }

    /**
     * Enable or disable indoor routing
     */
    private fun indoorRouting() {
        Log.v(TAG, "indoorRouting")
        if (mIndoorRouting) {
            mIndoorRouting = false
            Log.v(TAG, "Indoor routing disabled")
        } else {
            mIndoorRouting = true
            Log.v(TAG, "Indoor routing enabled")
        }
        invalidateOptionsMenu()
    }

    /**
     * Refreshing map view if parameters for venue map service changed
     */
    protected fun refreshMapView() {
        Log.v(TAG, "refreshMapView")
        // Set main activity on pause
        onPause()
        // Remember center of the map for future
        if (mMap != null) {
            mLastMapCenter = mMap!!.center
        }
        // Reinitialization of the map
        stopVenueMaps()
        startVenueMaps()
        // Resume main activity
        onResume()
    }

    override fun onPositionUpdated(locationMethod: PositioningManager.LocationMethod, geoPosition: GeoPosition, mapMatched: Boolean) {
        mLastReceivedPosition = geoPosition
        val receivedCoordinate = mLastReceivedPosition!!.coordinate
        if (mTransforming) {
            mPendingUpdate = Runnable { onPositionUpdated(locationMethod, geoPosition, mapMatched) }
        } else {
            if (mVenueMapFragment != null) {
                mLastMapCenter = receivedCoordinate
                if (!mUserControl) {
                    // when "follow position" options selected than map centered according to position updates
                    mMap!!.setCenter(receivedCoordinate, Map.Animation.NONE)
                    // Correctly displaying indoor position inside the venue
                    if (geoPosition.positionSource == GeoPosition.SOURCE_INDOOR) {
                        if (!geoPosition.buildingId.isEmpty() && mPrivateVenues) {
                            mVenueMapFragment!!.selectVenueAsync(geoPosition.buildingId)
                            mVenueMapFragment!!.getVenueController(mVenueMapFragment!!.selectedVenue)
                            selectLevelByFloorId(geoPosition.floorId!!)
                        }
                    }
                }
            }
        }
    }

    /**
     * Selecting venue level by given floorID
     * @param floorId current indoor position
     */
    protected fun selectLevelByFloorId(floorId: Int) {
        if (mVenueMapFragment != null) {
            val venue = mVenueMapFragment!!.selectedVenue
            if (venue != null) {
                mVenueMapFragment!!.setFloorChangingAnimation(true)
                val levels = venue.levels
                for (item in levels) {
                    if (item != null) {
                        if (item.floorNumber == floorId) {
                            mVenueMapFragment!!.getVenueController(venue)!!.selectLevel(item)
                            break
                        }
                    }
                }
            }
        }
    }

    override fun onPositionFixChanged(locationMethod: PositioningManager.LocationMethod, locationStatus: PositioningManager.LocationStatus) {
        if (locationStatus == PositioningManager.LocationStatus.OUT_OF_SERVICE) {
            // Out of service, last received position no longer valid for route calculation
            mLastReceivedPosition = null
        }
    }

    override fun onMapTransformStart() {
        Log.v(TAG, "onMapTransformStart")
        mTransforming = true
    }

    override fun onMapTransformEnd(mapState: MapState) {
        Log.v(TAG, "onMapTransformEnd")
        mTransforming = false
        if (mPendingUpdate != null) {
            mPendingUpdate!!.run()
            mPendingUpdate = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {
                for (index in permissions.indices) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /*
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat
                                        .shouldShowRequestPermissionRationale(this, permissions[index])) {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                    + " not granted. "
                                    + "Please go to settings and turn on for sample app",
                                    Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                    + " not granted", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                startVenueMaps()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onDoubleTapEvent(point: PointF): Boolean {
        return false
    }

    override fun onLongPressEvent(point: PointF): Boolean {
        return false
    }

    override fun onLongPressRelease() {}

    override fun onMapObjectsSelected(arg0: List<ViewObject>): Boolean {
        return false
    }

    override fun onMultiFingerManipulationEnd() {}

    override fun onMultiFingerManipulationStart() {}

    override fun onPanEnd() {}

    override fun onPanStart() {
        if (mMap != null) {
            // User takes control of map instead of position updates
            mUserControl = true
            invalidateOptionsMenu()
        }

    }

    override fun onPinchLocked() {}

    override fun onPinchZoomEvent(scaleFactor: Float, point: PointF): Boolean {
        return false
    }

    override fun onRotateEvent(angle: Float): Boolean {
        return false
    }

    override fun onRotateLocked() {}

    override fun onTiltEvent(angle: Float): Boolean {
        return false
    }

    override fun onTwoFingerTapEvent(arg0: PointF): Boolean {
        return false
    }

    override fun onTapEvent(p: PointF): Boolean {
        if (mMap == null) {
            Toast.makeText(this@BasicVenueActivity, "Initialization of venue service is in progress...", Toast.LENGTH_SHORT).show()
            return false
        }
        val touchLocation = mMap!!.pixelToGeo(p)
        val lat = touchLocation.latitude
        val lon = touchLocation.longitude
        val StrGeo = String.format("%.6f, %.6f", lat, lon)
        Toast.makeText(applicationContext, StrGeo, Toast.LENGTH_SHORT).show()
        mUserControl = true
        invalidateOptionsMenu()
        return false
    }

    override fun onVenueTapped(venue: Venue, x: Float, y: Float) {
        Log.v(TAG, "onVenueTapped")
        mMap!!.pixelToGeo(PointF(x, y))
        mVenueMapFragment!!.selectVenue(venue)
    }

    override fun onSpaceDeselected(venue: Venue, space: Space) {}

    override fun onSpaceSelected(venue: Venue, space: Space) {
        Log.v(TAG, "onSpaceSelected")
        onSpaceSelectedMapMode(space)
    }

    override fun onFloorChanged(venue: Venue, oldLevel: Level, newLevel: Level) {}

    override fun onVenueVisibleInViewport(venue: Venue?, visible: Boolean) {}

    override fun onVenueSelected(venue: Venue) {
        Log.v(TAG, "onVenueSelected: %s", venue.id)
        if (mVenueMapFragment == null) {
            return
        }
        val venueId = venue.id
        val venueName = venue.content.name
        Toast.makeText(applicationContext, "$venueId: $venueName", Toast.LENGTH_SHORT)
                .show()
        Log.v(TAG, "Venue selected: %s: %s", venueId, venueName)
    }

    override fun onVenueDeselected(var1: Venue, var2: DeselectionSource) {
        mUserControl = true
        invalidateOptionsMenu()
    }

    /**
     * Showing information about space selected from the venue map
     * and logging it
     * @param space selected space
     */
    private fun onSpaceSelectedMapMode(space: Space) {
        Log.v(TAG, "onSpaceSelectedMapMode")
        val spaceName = space.content.name
        val parentCategory = space.content.parentCategoryId
        val placeCategory = space.content.placeCategoryId
        Toast.makeText(applicationContext, "Space " + spaceName + ", parent category: "
                + parentCategory + ", place category: " + placeCategory, Toast.LENGTH_SHORT).show()
        Log.v(TAG, "Space selected: %s, Parent category: %s, Place category: %s", spaceName, parentCategory, placeCategory)
    }

    /**
     * Converting position source to string
     * @param geoPosition latest position
     * @return string representation of position source
     */
    private fun positionSourceToString(geoPosition: GeoPosition): String {
        val sources = geoPosition.positionSource
        if (sources == GeoPosition.SOURCE_NONE) {
            return "NONE"
        }
        val result = StringBuilder()
        if (sources and GeoPosition.SOURCE_CACHE != 0) {
            result.append("CACHE ")
        }
        if (sources and GeoPosition.SOURCE_FUSION != 0) {
            result.append("FUSION ")
        }
        if (sources and GeoPosition.SOURCE_HARDWARE != 0) {
            result.append("HARDWARE ")
        }
        if (sources and GeoPosition.SOURCE_INDOOR != 0) {
            result.append("INDOOR ")
        }
        if (sources and GeoPosition.SOURCE_OFFLINE != 0) {
            result.append("OFFLINE ")
        }
        if (sources and GeoPosition.SOURCE_ONLINE != 0) {
            result.append("ONLINE ")
        }
        return result.toString().trim { it <= ' ' }
    }

    /**
     * Converting position technology to string
     * @param geoPosition latest position
     * @return string representation of position technology
     */
    private fun positionTechnologyToString(geoPosition: GeoPosition): String {
        val technologies = geoPosition.positionTechnology
        if (technologies == GeoPosition.TECHNOLOGY_NONE) {
            return "NONE"
        }
        val result = StringBuilder()
        if (technologies and GeoPosition.TECHNOLOGY_BLE != 0) {
            result.append("BLE ")
        }
        if (technologies and GeoPosition.TECHNOLOGY_CELL != 0) {
            result.append("CELL ")
        }
        if (technologies and GeoPosition.TECHNOLOGY_GNSS != 0) {
            result.append("GNSS ")
        }
        if (technologies and GeoPosition.TECHNOLOGY_WIFI != 0) {
            result.append("WIFI ")
        }
        if (technologies and GeoPosition.TECHNOLOGY_SENSORS != 0) {
            result.append("SENSORS ")
        }
        return result.toString().trim { it <= ' ' }
    }

    /**
     * Initializes HERE Venue Maps. Called after permission check.
     */
    private fun startVenueMaps() {
        Log.v(TAG, "InitializeVenueMaps")
        mActivity = this

        mVenueMapFragment = mapFragment

        // Set path of isolated disk cache
        val diskCacheRoot = (Environment.getExternalStorageDirectory().path
                + File.separator + ".isolated-here-maps")
        // Retrieve intent name from manifest
        var intentName: String? = ""
        try {
            val ai = packageManager.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA)
            val bundle = ai.metaData
            intentName = bundle.getString("INTENT_NAME")
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to find intent name, NameNotFound: " + e.message)
        }

        val success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                diskCacheRoot, intentName)
        if (!success) {
            Toast.makeText(mActivity, "Operation 'setIsolatedDiskCacheRootPath' was not successful",
                    Toast.LENGTH_SHORT).show()
            return
        }

        mVenueMapFragment!!.init({ error ->
            if (error == OnEngineInitListener.Error.NONE) {
                Log.v(TAG, "InitializeVenueMaps: OnEngineInitializationCompleted")

                mVenueService = mVenueMapFragment!!.venueService
                mRoutingController = mVenueMapFragment!!.routingController
                if (mRoutingController != null) {
                    mRoutingController!!.addListener(this@BasicVenueActivity)
                }
                // Setting venue service content based on menu option
                if (!mPrivateVenues) {
                    setVenueServiceContent(false, false)// Public only
                } else {
                    setVenueServiceContent(true, true)// Private + public
                }
            } else {
                Log.e(TAG, "onEngineInitializationCompleted: %s", error)
            }
        }, { result ->
            Log.v(TAG, "VenueServiceListener: OnInitializationCompleted with result: %s", result)
            when (result) {
                VenueService.InitStatus.IN_PROGRESS -> {
                    Log.v(TAG, "Initialization of venue service is in progress...")
                    Toast.makeText(this@BasicVenueActivity, "Initialization of venue service is in progress...", Toast.LENGTH_SHORT).show()
                }
                VenueService.InitStatus.OFFLINE_SUCCESS, VenueService.InitStatus.ONLINE_SUCCESS -> {
                    // Adding venue listener to map fragment
                    mVenueMapFragment!!.addListener(mActivity)
                    // Set animations on for floor change and venue entering
                    mVenueMapFragment!!.setFloorChangingAnimation(true)
                    mVenueMapFragment!!.setVenueEnteringAnimation(true)
                    // Ask notification when venue visible; this notification is
                    // part of VenueMapFragment.VenueListener
                    mVenueMapFragment!!.setVenuesInViewportCallback(true)

                    // Add Gesture Listener for map fragment
                    mVenueMapFragment!!.mapGesture.addOnGestureListener(mActivity, 0, false)

                    // retrieve a reference of the map from the map fragment
                    mMap = mVenueMapFragment!!.map

                    mMap!!.addTransformListener(mActivity)
                    mMap!!.zoomLevel = mMap!!.maxZoomLevel - 3


                    // Create floors controller widget
                    mFloorsControllerWidget = FloorsControllerWidget(mActivity!!,
                            mVenueMapFragment!!,
                            findViewById<View>(R.id.floorListView) as ListView, R.layout.floor_item,
                            R.id.floorName, R.id.floorGroundSep)

                    // Start of Position Updates
                    try {
                        startPositionUpdates()
                        mVenueMapFragment!!.positionIndicator.isVisible = true
                    } catch (ex: Exception) {
                        Log.w(TAG, "startPositionUpdates: Could not register for location " +
                                "updates: %s", Log.getStackTraceString(ex)!!)
                    }

                    followPosition()
                }
            }
        })
    }

    /**
     * Setting usage of Private Venues along with Public Venues
     * @param mCombined used for setting Combined content
     * @param mPrivate used for setting Private content
     * VenueServiceListener should be initialized after setting this content
     * More details in documentation
     */
    private fun setVenueServiceContent(mCombined: Boolean, mPrivate: Boolean) {
        try {
            Log.v(TAG, "setVenueServiceContent: Combined = %s, Private = %s", mCombined, mPrivate)
            mVenueService!!.isPrivateContent = mPrivate
            mVenueService!!.setIsCombinedContent(mCombined)
        } catch (e: java.security.AccessControlException) {
            e.printStackTrace()
            Toast.makeText(this@BasicVenueActivity, e.message, Toast.LENGTH_LONG).show()
            Log.e(TAG, "SetVenueServiceContent error: %s", Log.getStackTraceString(e)!!)
        }

    }

    /** Initialization of Position Updates
     * Called after map initialization
     */
    private fun startPositionUpdates() {
        Log.v(TAG, "Start of Positioning Updates")
        mPositioningManager = PositioningManager.getInstance()
        if (mPositioningManager == null) {
            Log.w(TAG, "startPositionUpdates: PositioningManager is null")
            return
        }

        mHereLocation = LocationDataSourceHERE.getInstance(mPositioningStatusListener)

        if (mHereLocation == null) {
            Log.w(TAG, "startPositionUpdates: LocationDataSourceHERE is null")
            finish()
            return
        }

        mHereLocation!!.setDiagnosticsListener { event -> Log.v(TAG, "onDiagnosticEvent: %s", event.description) }

        mPositioningManager!!.dataSource = mHereLocation
        try {
            mPositioningManager!!.addListener(WeakReference(mActivity))
            if (mIndoorPositioning) {
                mLocationMethod = PositioningManager.LocationMethod.GPS_NETWORK_INDOOR
                Log.v(TAG, "Location method set to GPS_NETWORK_INDOOR")
            } else {
                mLocationMethod = PositioningManager.LocationMethod.GPS_NETWORK
                Log.v(TAG, "Location method set to GPS_NETWORK")
            }
            if (!mPositioningManager!!.start(mLocationMethod)) {
                Log.e(TAG, "startPositionUpdates: PositioningManager.start returned error")
            }
        } catch (ex: Exception) {
            Log.w(TAG, "startPositionUpdates: Could not register for location updates: %s", Log.getStackTraceString(ex)!!)
        }

        try {
            mRadioMapLoader = RadioMapLoadHelper(LocationDataSourceHERE.getInstance().radioMapLoader,
                    object : RadioMapLoadHelper.Listener {
                        override fun onError(venue: Venue, status: RadioMapLoader.Status) {
                            // Radio map loading failed with status.
                        }

                        override fun onProgress(venue: Venue, progress: Int) {
                            // Radio map loading progress.
                        }

                        override fun onCompleted(venue: Venue, status: RadioMapLoader.Status) {
                            Log.i(TAG, "Radio map for venue: " + venue.id + ", completed with status: " + status)
                            // Radio map loading completed with status.
                        }
                    })
            mVenueService!!.addVenueLoadListener(mVenueLoadListener)
        } catch (ex: Exception) {
            Log.e(TAG, "startPositionUpdates: setting up radio map loader failed", ex)
            mRadioMapLoader = null
        }

    }

    /**
     * Stop position updates
     * and remove listener
     */
    protected fun stopPositioningUpdates() {
        Log.v(TAG, "stopPositioningUpdates")
        if (mPositioningManager != null) {
            mPositioningManager!!.stop()
            mPositioningManager!!.removeListener(mActivity)
        }
        if (mRadioMapLoader != null) {
            mVenueService!!.removeVenueLoadListener(mVenueLoadListener)
            mRadioMapLoader = null
        }
    }

    /**
     * Stop VenueMaps service
     */
    protected fun stopVenueMaps() {
        Log.v(TAG, "stopVenueMaps")
        stopPositioningUpdates()
        mVenueMapFragment = null
        mMap = null
        mFloorsControllerWidget = null
    }

    // Setup routing parameters and calculate route.
    fun onCalculateRouteClick(v: View) {
        if (mLastReceivedPosition != null && mSelectedVenue != null && mSelectedSpace != null) {
            val venueRouteOptions = VenueRouteOptions()
            val options = venueRouteOptions.routeOptions
            // Set algorithm mode shortest and transport mode pedestrian in this case
            options.routeType = RouteOptions.Type.SHORTEST
            options.transportMode = RouteOptions.TransportMode.PEDESTRIAN
            options.routeCount = 1
            venueRouteOptions.routeOptions = options
            val selectedVenueController = mVenueMapFragment!!.getVenueController(mSelectedVenue)
            if (selectedVenueController != null && mRoutingController != null) {
                Toast.makeText(this@BasicVenueActivity, "Calculating route...", Toast.LENGTH_SHORT).show()
                // Determine start location either from the venue as
                // LevelLocation, or from outside as OutdoorLocation
                val startLocation: BaseLocation
                if (mLastReceivedPosition!!.positionSource == GeoPosition.SOURCE_INDOOR) {
                    var startLevel = selectedVenueController.selectedLevel
                    for (level in selectedVenueController.venue.levels) {
                        if (level.floorNumber == mLastReceivedPosition!!.floorId) {
                            startLevel = level
                            break
                        }
                    }
                    startLocation = LevelLocation(startLevel,
                            mLastReceivedPosition!!.coordinate, selectedVenueController)
                } else {
                    startLocation = OutdoorLocation(mLastReceivedPosition!!.coordinate)
                }
                // End location is in this case always the selected space
                val endLocation = SpaceLocation(mSelectedSpace!!, selectedVenueController)
                // This is an async function, the logic to display route is in callback
                // onCombinedRouteCompleted(CombinedRoute route)
                mRoutingController!!.calculateCombinedRoute(startLocation, endLocation, venueRouteOptions)
            }
        } else {
            Toast.makeText(this@BasicVenueActivity, "Unable to calculate route", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onCombinedRouteCompleted(route: CombinedRoute) {
        Log.v(TAG, "onCombinedRouteCompleted")
        val error = route.error
        if (error == CombinedRoute.VenueRoutingError.NO_ERROR) {
            if (mVenueMapFragment != null) {
                val selectedVenueController = mVenueMapFragment!!.getVenueController(mSelectedVenue)
                if (selectedVenueController != null && mRoutingController != null) {
                    Log.i(TAG, "onCombinedRouteCompleted route found")
                    Toast.makeText(this@BasicVenueActivity, "Route found", Toast.LENGTH_SHORT).show()
                    // Use RoutingController to show route
                    mRoutingController!!.showRoute(route)
                    mRouteShown = true
                }
            }
        } else {
            Toast.makeText(this@BasicVenueActivity, "No route found", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {

        // TAG string for logging purposes
        private val TAG = "VenuesAndLogging.BasicVenueActivity"

        // permissions request code
        private val REQUEST_CODE_ASK_PERMISSIONS = 1

        private val RUNTIME_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE)

        // Constant for not found
        val NOT_FOUND = -1

        /**
         * Permissions that need to be explicitly requested from end user.
         */
        private val REQUIRED_SDK_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        /**
         * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
         * needs when the app is running.
         */
        private fun hasPermissions(context: Context, vararg permissions: String): Boolean {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        return false
                    }
                }
            }
            return true
        }
    }
}
