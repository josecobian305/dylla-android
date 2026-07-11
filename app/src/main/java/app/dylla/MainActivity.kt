package app.dylla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.dylla.ui.theme.DyllaTheme
import app.dylla.ui.screens.LoginScreen
import app.dylla.ui.screens.OnboardingScreen
import app.dylla.ui.screens.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DyllaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DyllaAppContent()
                }
            }
        }
    }
}

@Composable
fun DyllaAppContent() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dylla_prefs", android.content.Context.MODE_PRIVATE) }

    val firebaseUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }

    var userUid by remember { mutableStateOf(prefs.getString("dylla_user_uid", null) ?: firebaseUser?.uid) }
    var userName by remember { mutableStateOf(prefs.getString("dylla_user_name", null) ?: firebaseUser?.displayName ?: "") }
    var userEmail by remember { mutableStateOf(prefs.getString("dylla_user_email", null) ?: firebaseUser?.email ?: "") }
    var onboardingComplete by remember { mutableStateOf(prefs.getBoolean("onboarding_complete", false)) }

    LaunchedEffect(userUid) {
        if (userUid != null && firebaseUser != null) {
            prefs.edit()
                .putString("dylla_user_uid", userUid)
                .putString("dylla_user_email", firebaseUser.email ?: userEmail)
                .putString("dylla_user_name", firebaseUser.displayName ?: userName)
                .apply()
        }
    }

    when {
        userUid == null -> {
            LoginScreen(
                onLoginSuccess = { uid, email, name ->
                    prefs.edit()
                        .putString("dylla_user_uid", uid)
                        .putString("dylla_user_email", email)
                        .putString("dylla_user_name", name)
                        .apply()
                    userUid = uid
                    userEmail = email
                    userName = name
                }
            )
        }
        !onboardingComplete -> {
            OnboardingScreen(
                onComplete = {
                    prefs.edit().putBoolean("onboarding_complete", true).apply()
                    onboardingComplete = true
                }
            )
        }
        else -> {
            MainScreen(
                onLogout = {
                    prefs.edit().clear().apply()
                    userUid = null
                    userName = ""
                    userEmail = ""
                    onboardingComplete = false
                }
            )
        }
    }
}
