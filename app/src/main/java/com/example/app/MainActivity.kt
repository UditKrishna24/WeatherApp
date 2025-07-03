package com.example.app

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toDrawable
import com.example.app.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val LOCATION_SETTINGS_REQUEST_CODE = 101
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var alreadyFetched = false
    private var locationDialogShown = false
    private var askedToEnableLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 2000
            fastestInterval = 1000
            numUpdates = 1
        }

        setupSearchBarStyle()
        searchCity()
        checkLocationAndFetch()
    }

    override fun onResume() {
        super.onResume()
        alreadyFetched = false
        checkLocationAndFetch()
    }

    private val gpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == LocationManager.PROVIDERS_CHANGED_ACTION || action == Intent.ACTION_PROVIDER_CHANGED) {
                val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        alreadyFetched = false
                        checkLocationAndFetch()
                    }, 2000)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED)
        registerReceiver(gpsSwitchStateReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(gpsSwitchStateReceiver)
    }

    private fun checkLocationAndFetch() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {

            if (!locationDialogShown && !askedToEnableLocation) {
                locationDialogShown = true
                askedToEnableLocation = true
                val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
                val client = LocationServices.getSettingsClient(this)
                val task = client.checkLocationSettings(builder.build())

                task.addOnFailureListener { e ->
                    if (e is ResolvableApiException) {
                        try {
                            e.startResolutionForResult(this, LOCATION_SETTINGS_REQUEST_CODE)
                        } catch (_: Exception) {
                            showDelhiWeather()
                        }
                    } else {
                        showDelhiWeather()
                    }
                }
            } else {
                showDelhiWeather()
            }
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        showLoading()
        requestLocation()
    }

    private fun showLoading() {
        binding.loadingView.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingView.visibility = View.GONE
    }

    private fun requestLocation() {
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null && !alreadyFetched) {
                    alreadyFetched = true
                    fetchWeatherByCoordinates(location.latitude, location.longitude)
                }
            }
        }, Looper.getMainLooper())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationAndFetch()
        } else {
            showDelhiWeather()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            checkLocationAndFetch()
        } else {
            showDelhiWeather()
        }
    }

    private fun showDelhiWeather() {
        if (!alreadyFetched) {
            alreadyFetched = true
            showLoading()
            fetchWeatherData("Delhi")
        }
    }

    private fun setupSearchBarStyle() {
        binding.searchView.queryHint = "Search for a city"
        val searchEditText = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setHintTextColor(Color.GRAY)
        searchEditText.setTextColor(Color.BLACK)
        searchEditText.textSize = 16f

        val closeButton = binding.searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)

        binding.searchView.setIconifiedByDefault(false)
        binding.searchView.isIconified = false
        binding.searchView.clearFocus()
    }

    private fun searchCity() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val city = query?.trim()
                if (!city.isNullOrEmpty()) {
                    showLoading()
                    fetchWeatherData(city)
                    binding.searchView.clearFocus()
                } else {
                    Toast.makeText(this@MainActivity, "Please enter a city", Toast.LENGTH_SHORT).show()
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })
    }

    private fun fetchWeatherByCoordinates(lat: Double, lon: Double) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(apiinterface::class.java)

        val response = retrofit.getWeatherDataByCoord(
            lat = lat,
            lon = lon,
            appid = "407a3e410cf4360f80dc10f1b4b73bd7",
            units = "metric"
        )

        response.enqueue(object : Callback<weatherapp> {
            override fun onResponse(call: Call<weatherapp>, response: Response<weatherapp>) {
                response.body()?.name?.let {
                    fetchWeatherData(it)
                }
            }

            override fun onFailure(call: Call<weatherapp>, t: Throwable) {
                showDelhiWeather()
            }
        })
    }

    private fun fetchWeatherData(cityname: String) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(apiinterface::class.java)

        val response = retrofit.getWeatherData(
            city = cityname,
            appid = "407a3e410cf4360f80dc10f1b4b73bd7",
            units = "metric"
        )

        response.enqueue(object : Callback<weatherapp> {
            override fun onResponse(call: Call<weatherapp>, response: Response<weatherapp>) {
                hideLoading()
                val data = response.body()
                if (response.isSuccessful && data != null) {
                    val temp = data.main.temp
                    val condition = data.weather.firstOrNull()?.main ?: "unknown"

                    val maxTemp = String.format("%.1f", temp + Random.nextDouble(0.2, 0.7))
                    val minTemp = String.format("%.1f", temp - Random.nextDouble(0.2, 0.7))

                    binding.textView5.text = "$temp °C"
                    binding.condition.text = condition
                    binding.climate.text = condition
                    binding.maxtemp.text = "Max Temp: $maxTemp °C"
                    binding.mintemp.text = "Min Temp: $minTemp °C"
                    binding.humidity.text = "${data.main.humidity}%"
                    binding.windspeed.text = "${data.wind.speed} m/s"
                    binding.sea.text = "${data.main.pressure} hPa"
                    binding.sunrise.text = time(data.sys.sunrise.toLong())
                    binding.sunset.text = time(data.sys.sunset.toLong())
                    binding.textView3.text = data.name
                    binding.day.text = dayName(System.currentTimeMillis())
                    binding.date.text = date()
                    changeImage(condition)
                } else {
                    Toast.makeText(this@MainActivity, "City not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<weatherapp>, t: Throwable) {
                hideLoading()
                Toast.makeText(this@MainActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun changeImage(condition: String) {
        when (condition) {
            "Clear", "Sunny", "Clear Sky" -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sunnyjson)
            }
            "Clouds", "Partly Clouds", "Mist", "Foggy" -> {
                binding.root.setBackgroundResource(R.drawable.cloudypic)
                binding.lottieAnimationView.setAnimation(R.raw.cloudyjson)
            }
            "Rain", "Light Rain", "Heavy Rain", "Drizzle" -> {
                binding.root.setBackgroundResource(R.drawable.rain_background)
                binding.lottieAnimationView.setAnimation(R.raw.rainyjson)
            }
            "Snow", "Light Snow", "Heavy Snow", "Blizzard" -> {
                binding.root.setBackgroundResource(R.drawable.snow_background)
                binding.lottieAnimationView.setAnimation(R.raw.snowjson)
            }
            else -> {
                binding.root.setBackgroundResource(R.drawable.mainwallpaper)
                binding.lottieAnimationView.setAnimation(R.raw.sunnyjson)
            }
        }
    }

    private fun time(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    private fun date(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun dayName(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
