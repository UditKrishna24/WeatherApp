package com.example.app

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import com.example.app.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.tasks.Task

import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val LOCATION_SETTINGS_REQUEST_CODE = 101
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupSearchBarStyle()

        getCurrentLocation()
        searchCity()
    }

    private fun setupSearchBarStyle() {
        val searchEditText = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setHintTextColor(Color.GRAY)
        searchEditText.setTextColor(Color.BLACK)
        searchEditText.textSize = 16f

        binding.searchView.setOnClickListener {
            binding.searchView.isIconified = false
            searchEditText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun searchCity() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val city = query?.trim()
                if (!city.isNullOrEmpty()) {
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

    private fun getCurrentLocation() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // Permissions check
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                return@addOnSuccessListener
            }

            // ✨ Add slight delay to allow location services to settle
            binding.root.postDelayed({
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        fetchWeatherByCoordinates(location.latitude, location.longitude)
                    } else {
                        Toast.makeText(this, "Retrying to get location...", Toast.LENGTH_SHORT).show()

                        // Retry once after a short delay
                        binding.root.postDelayed({
                            fusedLocationClient.lastLocation.addOnSuccessListener { retryLocation: Location? ->
                                retryLocation?.let {
                                    fetchWeatherByCoordinates(it.latitude, it.longitude)
                                } ?: Toast.makeText(this, "Still unable to detect location", Toast.LENGTH_SHORT).show()
                            }
                        }, 3000) // Retry after 3 seconds
                    }
                }
            }, 2000) // Initial wait of 2 seconds
        }

        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this, LOCATION_SETTINGS_REQUEST_CODE)
                } catch (sendEx: Exception) {
                    Toast.makeText(this, "Location request failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔄 This method gets called when user returns from location enable popup
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            getCurrentLocation()
        }
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
                response.body()?.name?.let { fetchWeatherData(it) }
            }

            override fun onFailure(call: Call<weatherapp>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to fetch weather", Toast.LENGTH_SHORT).show()
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
                val data = response.body()
                if (response.isSuccessful && data != null) {
                    val temp = data.main.temp.toString()
                    val condition = data.weather.firstOrNull()?.main ?: "unknown"
                    val maxTemp = data.main.temp_max
                    val minTemp = data.main.temp_min

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
