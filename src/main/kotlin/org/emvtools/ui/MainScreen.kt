package org.emvtools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.emvtools.model.NavigationCategory
import org.emvtools.model.NavigationItem
import org.emvtools.ui.screens.*
import org.emvtools.ui.theme.*

@Composable
fun MainScreen() {
    var selectedItem by remember { mutableStateOf(NavigationItem.EMV_TAGS) }
    var darkTheme by remember { mutableStateOf(false) }
    
    EmvToolsTheme(darkTheme = darkTheme) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Navigation Rail
            NavigationSidebar(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                darkTheme = darkTheme,
                onThemeToggle = { darkTheme = !darkTheme }
            )
            
            // Main content
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (selectedItem) {
                    NavigationItem.EMV_TAGS -> EmvTagsScreen()
                    NavigationItem.TLV_DECODER -> TlvDecoderScreen()
                    NavigationItem.CRYPTOGRAM_CALC -> CryptogramScreen()
                    NavigationItem.DES_CALCULATOR -> DesCalculatorScreen()
                    NavigationItem.ASN1_DECODER -> Asn1DecoderScreen()
                    NavigationItem.PIN_TOOLS -> PinToolsScreen()
                    NavigationItem.KEY_SHARES -> KeySharesScreen()
                    NavigationItem.HEX_DUMP -> HexDumpScreen()
                    NavigationItem.CHAR_CONVERTER -> CharConverterScreen()
                    NavigationItem.MRZ_CALCULATOR -> MrzCalculatorScreen()
                    NavigationItem.CAPK_MANAGEMENT -> CapkScreen()
                    NavigationItem.EMV_SIMULATOR -> EmvSimulatorScreen()
                    NavigationItem.APDU_SENDER -> ApduSenderScreen()
                    NavigationItem.AES_CALCULATOR -> AesCalculatorScreen()
                    NavigationItem.DUKPT_TOOLS -> DukptScreen()
                    NavigationItem.ISO8583_BUILDER -> Iso8583Screen()
                    NavigationItem.HSM_TESTER -> HsmTesterScreen()
                    NavigationItem.QR_TOOLS -> QrToolsScreen()
                    NavigationItem.FILE_DIFF -> FileDiffScreen()
                }
            }
        }
    }
}

@Composable
private fun NavigationSidebar(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // App title
            Text(
                text = "BP-Tools",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Navigation items grouped by category
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NavigationCategory.entries.forEach { category ->
                    val itemsInCategory = NavigationItem.entries.filter { it.category == category }
                    
                    item {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = getCategoryColor(category),
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                    
                    items(itemsInCategory) { item ->
                        NavigationItemRow(
                            item = item,
                            isSelected = item == selectedItem,
                            onClick = { onItemSelected(item) }
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Theme toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onThemeToggle() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (darkTheme) "Light Mode" else "Dark Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NavigationItemRow(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getIconForItem(item),
            contentDescription = item.title,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}

@Composable
private fun getCategoryColor(category: NavigationCategory) = when (category) {
    NavigationCategory.EMV -> EmvColor
    NavigationCategory.CRYPTO -> CryptoColor
    NavigationCategory.BANKING -> BankingColor
    NavigationCategory.MISC -> MiscColor
}

private fun getIconForItem(item: NavigationItem): ImageVector = when (item) {
    NavigationItem.EMV_TAGS -> Icons.Default.Label
    NavigationItem.TLV_DECODER -> Icons.Default.Code
    NavigationItem.CRYPTOGRAM_CALC -> Icons.Default.Security
    NavigationItem.DES_CALCULATOR -> Icons.Default.Lock
    NavigationItem.ASN1_DECODER -> Icons.Default.AccountTree
    NavigationItem.PIN_TOOLS -> Icons.Default.Pin
    NavigationItem.KEY_SHARES -> Icons.Default.Key
    NavigationItem.HEX_DUMP -> Icons.Default.DataArray
    NavigationItem.CHAR_CONVERTER -> Icons.Default.TextFields
    NavigationItem.MRZ_CALCULATOR -> Icons.Default.Badge
    NavigationItem.CAPK_MANAGEMENT -> Icons.Default.VerifiedUser
    NavigationItem.EMV_SIMULATOR -> Icons.Default.PlayArrow
    NavigationItem.APDU_SENDER -> Icons.Default.Send
    NavigationItem.AES_CALCULATOR -> Icons.Default.EnhancedEncryption
    NavigationItem.DUKPT_TOOLS -> Icons.Default.VpnKey
    NavigationItem.ISO8583_BUILDER -> Icons.Default.Message
    NavigationItem.HSM_TESTER -> Icons.Default.Memory
    NavigationItem.QR_TOOLS -> Icons.Default.QrCode2
    NavigationItem.FILE_DIFF -> Icons.Default.Compare
}

