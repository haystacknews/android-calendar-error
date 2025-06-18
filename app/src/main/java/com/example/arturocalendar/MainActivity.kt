package com.example.arturocalendar

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.arturocalendar.ui.theme.MyApplicationTheme
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }

    val credentialManager = CredentialManager.create(this)
    
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { 
        result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val authorizationResult = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(result.data)
                handleCalendarAccess(authorizationResult)
            } else {
                Log.d(TAG, "Authorization failed. Result code: $result. Details: ${result.data}")
            }
    }
    
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId("127662802395-0llqborkr87bfmbuufibrl98qp1nbsc8.apps.googleusercontent.com")
            .setAutoSelectEnabled(true)
        .build()

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


        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity,
                )

                val credential = result.credential

                if (credential is CustomCredential) {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            // Use googleIdTokenCredential and extract the ID to validate and
                            // authenticate on your server.
                            val googleIdTokenCredential = GoogleIdTokenCredential
                                .createFrom(credential.data)

                            Log.d(TAG, "got token: $googleIdTokenCredential")

                            // Request calendar permissions
                            requestCalendarPermissions()
                        } catch (e: GoogleIdTokenParsingException) {
                            Log.e(TAG, "Received an invalid google id token response", e)
                        }
                    } else {
                        // Catch any unrecognized custom credential type here.
                        Log.e(TAG, "Unexpected type of credential")
                    }
                }
            } catch (e: GetCredentialException) {
                Log.d(TAG, "sign in error: $e")
            }
        }
    }
    
    private fun requestCalendarPermissions() {
        val requestedScopes = listOf(Scope(CalendarScopes.CALENDAR_READONLY))
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
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
        Log.d(TAG, "Calendar access granted successfully")
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
