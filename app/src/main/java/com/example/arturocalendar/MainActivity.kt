package com.example.arturocalendar

import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.arturocalendar.ui.theme.MyApplicationTheme
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { 
        result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val authorizationResult = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(result.data)
                handleCalendarAccess(authorizationResult)
            } else {
                Log.d(TAG, "Authorization failed. Result code: ${result.resultCode}")
                Log.d(TAG, "Intent data: ${result.data}")
                result.data?.extras?.let { extras ->
                    for (key in extras.keySet()) {
                        Log.d(TAG, "Extra: $key = ${extras.get(key)}")
                    }
                }
            }
    }
    
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    Greeting("Android")
                }
            }
        }

        requestCalendarPermissions()
    }

    private fun requestCalendarPermissions() {
        val requestedScopes = listOf(Scope(CalendarScopes.CALENDAR_EVENTS_READONLY))
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            // use WEB client id, not android one
            .requestOfflineAccess("127662802395-9vdovnh12hbqvgnd3n4c5qkmk6kbl2h9.apps.googleusercontent.com")
            .build()
            
        Identity.getAuthorizationClient(this)
            .authorize(authorizationRequest)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    // Access needs to be granted by the user
                    val pendingIntent = authorizationResult.pendingIntent

                    if (pendingIntent == null) {
                        Log.d(TAG, "Pending intent is null")
                        return@addOnSuccessListener
                    }

                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        startForResult.launch(intentSenderRequest)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "Couldn't start Authorization UI: ${e.localizedMessage}")
                    }
                } else {
                    // Access already granted, continue with user action
                    handleCalendarAccess(authorizationResult)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to authorize", e)
            }
    }
    
    private fun handleCalendarAccess(authorizationResult: AuthorizationResult) {
        // Handle successful calendar access here
        Log.d(TAG, "Calendar access granted successfully! ${authorizationResult.serverAuthCode}")
        // You can now make Calendar API calls
    }
    

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}
