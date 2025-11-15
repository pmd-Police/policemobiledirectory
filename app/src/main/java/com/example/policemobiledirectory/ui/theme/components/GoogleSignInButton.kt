package com.example.policemobiledirectory.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border // Keep this import for BorderStroke if used elsewhere, though Button's border doesn't need it directly.
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.policemobiledirectory.R
import androidx.compose.foundation.BorderStroke // Added import for BorderStroke

@Composable
fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "Sign in with Google"
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,  // Google button background
            contentColor = Color.Black
        ),
        border = BorderStroke(1.dp, Color.LightGray), // Corrected to use BorderStroke
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Commented out due to missing R.drawable.ic_google_logo
                /*
                Image(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp)
                )
                */
                Spacer(modifier = Modifier.width(8.dp)) // Keep spacer or adjust as needed without logo
                Text(buttonText)
            }
        }
    }
}
