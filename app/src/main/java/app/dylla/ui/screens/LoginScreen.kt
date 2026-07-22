package app.dylla.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.ui.theme.*
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest

private const val WEB_CLIENT_ID = "86169052590-s7b4iv3jpdb4ng8v9ubpjibakupr0uo7.apps.googleusercontent.com"

private enum class AuthMode { LOGIN, REGISTER }

@Composable
fun LoginScreen(
    onLoginSuccess: (uid: String, email: String, name: String) -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val user = auth.currentUser
                        onLoginSuccess(
                            user?.uid ?: "",
                            user?.email ?: account.email ?: "",
                            user?.displayName ?: account.displayName ?: ""
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val auth = remember { FirebaseAuth.getInstance() }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFAFAF9),
            Color(0xFFF5F5F4)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Brand logo
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = DyllaBlue.copy(alpha = 0.3f),
                        spotColor = DyllaBlue.copy(alpha = 0.3f)
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(DyllaBlue, DyllaBlue.copy(alpha = 0.8f))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "D",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Dylla",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = DyllaOnSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (mode == AuthMode.LOGIN) "Sign in to your account" else "Create your account",
                fontSize = 14.sp,
                color = DyllaOnSurfaceSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Google sign-in button
            OutlinedButton(
                onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(WEB_CLIENT_ID)
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(client.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text(
                    text = "G",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4),
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(
                    text = "Continue with Google",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = DyllaOnSurface
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // "or" divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFD1D1D6)
                )
                Text(
                    text = "or",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 12.sp,
                    color = DyllaOnSurfaceSecondary
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFD1D1D6)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Email form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Full name field (register mode only)
                    AnimatedVisibility(visible = mode == AuthMode.REGISTER) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full name") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    // Error display
                    if (error != null) {
                        Text(
                            text = error ?: "",
                            fontSize = 12.sp,
                            color = DyllaRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = DyllaRed.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    // Sign in / Create account button
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                error = "Enter email and password"
                                return@Button
                            }
                            loading = true
                            error = null

                            if (mode == AuthMode.REGISTER) {
                                auth.createUserWithEmailAndPassword(email.trim(), password)
                                    .addOnSuccessListener { result ->
                                        val user = result.user
                                        if (user != null && name.isNotBlank()) {
                                            val profileUpdates = UserProfileChangeRequest.Builder()
                                                .setDisplayName(name.trim())
                                                .build()
                                            user.updateProfile(profileUpdates).addOnCompleteListener {
                                                loading = false
                                                onLoginSuccess(
                                                    user.uid,
                                                    user.email ?: email.trim(),
                                                    name.trim()
                                                )
                                            }
                                        } else if (user != null) {
                                            loading = false
                                            onLoginSuccess(
                                                user.uid,
                                                user.email ?: email.trim(),
                                                ""
                                            )
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        loading = false
                                        error = e.localizedMessage ?: "Registration failed"
                                    }
                            } else {
                                auth.signInWithEmailAndPassword(email.trim(), password)
                                    .addOnSuccessListener { result ->
                                        loading = false
                                        val user = result.user
                                        if (user != null) {
                                            onLoginSuccess(
                                                user.uid,
                                                user.email ?: email.trim(),
                                                user.displayName ?: ""
                                            )
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        loading = false
                                        error = e.localizedMessage ?: "Sign in failed"
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DyllaOnSurface,
                            contentColor = Color.White
                        ),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (mode == AuthMode.LOGIN) "Sign in" else "Create account",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle between login/register
            TextButton(
                onClick = {
                    mode = if (mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
                    error = null
                }
            ) {
                Text(
                    text = if (mode == AuthMode.LOGIN) "Don't have an account? Sign up"
                    else "Already have an account? Sign in",
                    fontSize = 14.sp,
                    color = DyllaOnSurfaceSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Free plan includes 100 calls/day",
                fontSize = 12.sp,
                color = DyllaOnSurfaceSecondary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
