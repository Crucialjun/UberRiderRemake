package com.example.uberriderremake.ui.home


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.uberriderremake.*
import com.example.uberriderremake.R
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback, FirebaseDriverInfoListener,
    FirebaseFailedListener {

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var mMap: GoogleMap

    lateinit var mapFragment: SupportMapFragment

    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var previousLocation: Location
    lateinit var currentLocation: Location

    var distance = 1.0
    var isFirstTime = true

    //listener
    lateinit var FireBaseDriverInfoListener: FirebaseDriverInfoListener
    lateinit var FireBaseFailedListener: FirebaseFailedListener

    var cityName = ""


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        homeViewModel.text.observe(viewLifecycleOwner, Observer {


        })



        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        init()
        return root
    }

    private fun init() {
        FireBaseDriverInfoListener = this
        FireBaseFailedListener = this
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.fastestInterval = 3000
        locationRequest.interval = 5000
        locationRequest.smallestDisplacement = 10f


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                val newPos = LatLng(
                    locationResult!!.lastLocation.latitude, locationResult.lastLocation.longitude
                )

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))

                //loadAll drivers
                val geoCoder = Geocoder(requireContext(), Locale.getDefault())
                val addressList: List<Address>

                if (isFirstTime) {
                    previousLocation = locationResult.lastLocation
                    currentLocation = locationResult.lastLocation

                    isFirstTime = false
                } else {
                    previousLocation = currentLocation
                    currentLocation = locationResult.lastLocation
                }

                if (previousLocation.distanceTo(currentLocation) / 1000 <= LIMIT_RANGE) {
                    loadAvailableDrivers()
                }


            }
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )


    }

    private fun loadAvailableDrivers() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient.lastLocation.addOnFailureListener {
            Snackbar.make(
                requireView(), "${it.message}", Snackbar.LENGTH_LONG
            ).show()
        }.addOnSuccessListener {

            val geocoder = Geocoder(requireContext(), Locale.getDefault())

            var addressList: List<Address>

            try {
                addressList = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                cityName = addressList[0].locality

                val driverLocationRef = FirebaseDatabase.getInstance().getReference(
                    DRIVER_LOCATION_REFERENCE
                )

                val geoFire = GeoFire(driverLocationRef)
                val geoQuery =
                    geoFire.queryAtLocation(GeoLocation(it.latitude, it.longitude), distance)
                geoQuery.removeAllListeners()

                geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
                    override fun onKeyEntered(key: String?, location: GeoLocation?) {
                        Common.driversFound.add(DriverGeoModel(key!!, location!!))
                    }

                    override fun onKeyExited(key: String?) {
                        TODO("Not yet implemented")
                    }

                    override fun onKeyMoved(key: String?, location: GeoLocation?) {
                        TODO("Not yet implemented")
                    }

                    override fun onGeoQueryReady() {
                        if (distance <= LIMIT_RANGE) {
                            distance++
                            loadAvailableDrivers()
                        } else {
                            distance = 0.0
                            addDriverMarker()
                        }
                    }

                    override fun onGeoQueryError(error: DatabaseError?) {
                        Snackbar.make(
                            requireView(), "$error", Snackbar.LENGTH_LONG
                        ).show()
                    }

                })

                driverLocationRef.addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        for(child in snapshot.children){
                            val geoQueryModel = child.getValue(GeoQueryModel::class.java)
                            val geoLocation = GeoLocation(
                                geoQueryModel!!.l!![0],
                                geoQueryModel.l!![1]
                            )
                            val driverGeoModel = DriverGeoModel(child.key.toString(), geoLocation)
                            val newDriverLocation = Location("")
                            newDriverLocation.latitude = geoLocation.latitude
                            newDriverLocation.longitude = geoLocation.longitude

                            val newDistance = it.distanceTo(newDriverLocation) / 1000
                            if (newDistance <= LIMIT_RANGE)
                                findDriverByKey(driverGeoModel)
                        }

                    }

                    override fun onChildChanged(
                        snapshot: DataSnapshot,
                        previousChildName: String?
                    ) {

                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {

                    }

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

                    }

                    override fun onCancelled(error: DatabaseError) {
                        Snackbar.make(
                            requireView(), error.message, Snackbar.LENGTH_LONG
                        ).show()
                    }

                })
            } catch (e: IOException) {
                Snackbar.make(
                    requireView(), "${e.message}", Snackbar.LENGTH_LONG
                ).show()
            }

        }
    }

    private fun findDriverByKey(driverGeoModel: DriverGeoModel) {
        FirebaseDatabase
            .getInstance()
            .getReference(DRIVER_INFO_REFERENCE)
            .child(driverGeoModel.key)
            .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.hasChildren()){
                            driverGeoModel.driverInfoModel = (snapshot.getValue(DriverInfoModel::class.java))
                            FireBaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel)
                        }else{
                            FireBaseFailedListener.onFirebaseFailed("Key Not Found ${driverGeoModel.key}")

                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        FireBaseFailedListener.onFirebaseFailed(error.message)
                    }

                })
    }

    @SuppressLint("CheckResult")
    private fun addDriverMarker() {
        if (Common.driversFound.size > 0) {
            Observable
                .fromIterable(Common.driversFound)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { driverGeoModel: DriverGeoModel? ->
                        if (driverGeoModel != null) {
                            findDriverByKey(driverGeoModel)
                        }
                    }, { t: Throwable? ->
                        Snackbar.make(
                            requireView(), "${t!!.message}", Snackbar.LENGTH_LONG
                        ).show()

                    }
                )
        } else {
            Snackbar.make(
                requireView(), "Drivers not found", Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!


        Dexter
            .withContext(requireContext())
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    }
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationButtonClickListener {

                        fusedLocationProviderClient.lastLocation.addOnFailureListener {
                            Snackbar.make(
                                requireView(), "${it.message}", Snackbar.LENGTH_LONG
                            ).show()

                        }.addOnSuccessListener {
                            val userLatlng = LatLng(it.latitude, it.longitude)
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatlng, 18f))
                        }

                        true
                    }

                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Snackbar.make(
                        requireView(),
                        "${p0?.permissionName} needed to run app",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

            }).check()

//        val locationButton = (mapFragment.requireView().findViewById<View>("1".toInt()).parent as View)
//                .findViewById<View>("2".toInt())
//
//        val params = locationButton.layoutParams as RelativeLayout.LayoutParams
//        params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0)
//        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
//        params.bottomMargin = 50

        mMap.uiSettings.isZoomControlsEnabled = true

        try {
            val success = googleMap
                .setMapStyle(
                    MapStyleOptions
                        .loadRawResourceStyle(context, R.raw.uber_maps_style)
                )
            if (!success) {
                Log.e("ERROR", "Style parsing Error")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("Error", e.message.toString())
        }
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel) {
        if(!Common.markerList.containsKey(driverGeoModel.key))
            Common.markerList[driverGeoModel!!.key!!] =
                mMap.addMarker(MarkerOptions()
                    .position(LatLng(driverGeoModel.geoLocation!!.latitude,driverGeoModel.geoLocation!!.longitude))
                    .flat(true)
                    .title(Common.buildName(driverGeoModel.driverInfoModel!!.firstName, driverGeoModel.driverInfoModel!!.lastName))
                    .snippet(driverGeoModel.driverInfoModel!!.phoneNumber)
                    .icon(BitmapDescriptorFactory.fromResource( R.drawable.ic_car_display))

            )

        if(cityName.isNotEmpty())
        {
            val driverLocation = FirebaseDatabase.getInstance().getReference(
                DRIVER_LOCATION_REFERENCE).child(cityName).child(driverGeoModel.key!!)

            driverLocation.addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.hasChildren()){
                        if (Common.markerList[driverGeoModel.key] != null){
                            val marker  = Common.markerList[driverGeoModel.key]
                            marker!!.remove()
                            Common.markerList.remove(driverGeoModel.key)
                            driverLocation.removeEventListener(this)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        }
    }

    override fun onFirebaseFailed(message: String) {
        TODO("Not yet implemented")
    }
}