package com.primafocus.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primafocus.ui.theme.*

@Composable
fun FeedbackModal() {
    var completedState by remember { mutableStateOf<String?>(null) } // "yes", "partial", "no"
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BackgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Registro Breve", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "¿Completaste el paso?", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { completedState = "yes" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (completedState == "yes") AccentGreen else SurfaceColor,
                        contentColor = if (completedState == "yes") Color.White else TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sí")
                }
                Button(
                    onClick = { completedState = "partial" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (completedState == "partial") WarningYellow else SurfaceColor,
                        contentColor = if (completedState == "partial") Color.White else TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Parcial")
                }
                Button(
                    onClick = { completedState = "no" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (completedState == "no") MutedText else SurfaceColor,
                        contentColor = if (completedState == "no") Color.White else TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("No")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (completedState == "partial" || completedState == "no") {
                Surface(
                    color = WarningYellow.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "¿Quieres posponer o dividir en subtareas?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text("Posponer") })
                            AssistChip(onClick = {}, label = { Text("Dividir") })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Text(text = "¿Cómo te sentiste?", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                IconButton(onClick = {}) { Text("😐", fontSize = 24.sp) }
                IconButton(onClick = {}) { Text("🙂", fontSize = 24.sp) }
                IconButton(onClick = {}) { Text("😃", fontSize = 24.sp) }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { /* Save */ },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Guardar", color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Se guardará en Historial", fontSize = 12.sp, color = MutedText)
        }
    }
}
