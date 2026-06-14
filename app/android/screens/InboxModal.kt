package com.primafocus.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primafocus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxModal() {
    // Assuming this is the content of a ModalBottomSheet
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .padding(16.dp)
            .padding(bottom = 16.dp) // Avoid system navigation overlap
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(MutedText.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /* Start listening */ },
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, MutedText, CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Dictar", tint = PrimaryBlue)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            var text by remember { mutableStateOf("") }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Anotar en 2s", color = MutedText) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AccentGreen.copy(alpha = 0.2f),
                onClick = { /* Change date */ }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "Hoy", fontSize = 14.sp, color = TextPrimary)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = { /* Expand accordion */ }) {
            Text("Más detalles", color = MutedText)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { /* Cancel */ }) {
                Text("Cancelar", color = MutedText)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { /* Save */ },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Guardar", color = Color.White)
            }
        }
    }
}
