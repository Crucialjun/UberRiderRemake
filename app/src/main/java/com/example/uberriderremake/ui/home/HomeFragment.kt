package com.example.uberriderremake.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.uberriderremake.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var mMap: GoogleMap

    lateinit var mapFragment : SupportMapFragment

    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

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

        init()

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return root
    }

    private fun init() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.fastestInterval = 3000
        locationRequest.interval = 5000
        locationRequest.smallestDisplacement = 10f


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                val newPos = LatLng(locationResult!!.lastLocation.latitude
                    ,locationResult.lastLocation.longitude)

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

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
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())


    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!


        Dexter
            .withContext(requireContext())
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener{
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
                            Snackbar.make(requireView()
                                ,"${it.message}"
                                ,Snackbar.LENGTH_LONG).show()

                        }.addOnSuccessListener {
                            val userLatlng = LatLng(it.latitude,it.longitude)
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatlng,18f))
                        }

                        true
                    }

                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Snackbar.make(requireView(),"${p0?.permissionName} needed to run app",Snackbar.LENGTH_LONG).show()
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

        try{
            val success = googleMap
                .setMapStyle(
                    MapStyleOptions
                        .loadRawResourceStyle(context,R.raw.uber_maps_style))
            if(!success){
                Log.e("ERROR", "Style parsing Error" )
            }
        }catch (e: Resources.NotFoundException){
            Log.e("Error", e.message.toString())
        }
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
}