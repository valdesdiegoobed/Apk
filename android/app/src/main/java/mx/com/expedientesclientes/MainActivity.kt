package mx.com.expedientesclientes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val PREFS = "expedientes_clientes"
private const val CLIENTS_KEY = "clients"

data class Client(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val curp: String = "",
    val rfc: String = "",
    val nss: String = "",
    val phone: String = "",
    val email: String = "",
    val afore: String = "",
    val notes: String = "",
    val documents: List<ClientDocument> = emptyList()
)

data class ClientDocument(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val uri: String,
    val mimeType: String = ""
)

private enum class Screen { CLIENTS, FORM, DETAIL, BROWSER }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ExpedientesApp() } }
    }
}

@Composable
private fun ExpedientesApp() {
    val context = LocalContext.current
    val clients = remember { mutableStateListOf<Client>() }
    var screen by remember { mutableStateOf(Screen.CLIENTS) }
    var selectedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        clients.clear()
        clients.addAll(ClientStore.load(context))
    }

    fun persist() = ClientStore.save(context, clients)
    fun selected() = clients.firstOrNull { it.id == selectedId }

    when (screen) {
        Screen.CLIENTS -> ClientsScreen(
            clients = clients,
            onNew = { selectedId = null; screen = Screen.FORM },
            onOpen = { selectedId = it; screen = Screen.DETAIL }
        )
        Screen.FORM -> ClientFormScreen(
            existing = selected(),
            onBack = { screen = if (selectedId == null) Screen.CLIENTS else Screen.DETAIL },
            onSave = { client ->
                val index = clients.indexOfFirst { it.id == client.id }
                if (index >= 0) clients[index] = client else clients.add(client)
                selectedId = client.id
                persist()
                screen = Screen.DETAIL
            }
        )
        Screen.DETAIL -> {
            val client = selected()
            if (client == null) {
                screen = Screen.CLIENTS
            } else {
                ClientDetailScreen(
                    client = client,
                    onBack = { screen = Screen.CLIENTS },
                    onEdit = { screen = Screen.FORM },
                    onBrowser = { screen = Screen.BROWSER },
                    onUpdate = { updated ->
                        val index = clients.indexOfFirst { it.id == updated.id }
                        if (index >= 0) clients[index] = updated
                        persist()
                    },
                    onDelete = {
                        clients.removeAll { it.id == client.id }
                        persist()
                        selectedId = null
                        screen = Screen.CLIENTS
                    }
                )
            }
        }
        Screen.BROWSER -> BrowserScreen(client = selected(), onBack = { screen = Screen.DETAIL })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientsScreen(clients: List<Client>, onNew: () -> Unit, onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = clients.filter {
        query.isBlank() || it.name.contains(query, true) || it.curp.contains(query, true) || it.rfc.contains(query, true)
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Expediente Clientes") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar por nombre, CURP o RFC") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("Nuevo cliente") }
            Spacer(Modifier.height(16.dp))
            if (filtered.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        if (clients.isEmpty()) "Aún no hay clientes. Pulsa Nuevo cliente." else "No se encontraron resultados.",
                        Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { client ->
                        Card(Modifier.fillMaxWidth().clickable { onOpen(client.id) }) {
                            Column(Modifier.padding(16.dp)) {
                                Text(client.name, style = MaterialTheme.typography.titleMedium)
                                if (client.curp.isNotBlank()) Text("CURP: ${client.curp}")
                                Text("Documentos: ${client.documents.size}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientFormScreen(existing: Client?, onBack: () -> Unit, onSave: (Client) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var curp by remember(existing?.id) { mutableStateOf(existing?.curp.orEmpty()) }
    var rfc by remember(existing?.id) { mutableStateOf(existing?.rfc.orEmpty()) }
    var nss by remember(existing?.id) { mutableStateOf(existing?.nss.orEmpty()) }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone.orEmpty()) }
    var email by remember(existing?.id) { mutableStateOf(existing?.email.orEmpty()) }
    var afore by remember(existing?.id) { mutableStateOf(existing?.afore.orEmpty()) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var error by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text(if (existing == null) "Nuevo cliente" else "Editar cliente") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Field("Nombre completo *", name) { name = it } }
            item { Field("CURP", curp) { curp = it.uppercase() } }
            item { Field("RFC", rfc) { rfc = it.uppercase() } }
            item { Field("NSS", nss) { nss = it } }
            item { Field("Teléfono", phone) { phone = it } }
            item { Field("Correo", email) { email = it } }
            item { Field("AFORE", afore) { afore = it } }
            item { Field("Observaciones", notes, false) { notes = it } }
            if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
            item {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            error = "El nombre es obligatorio."
                        } else {
                            onSave(
                                Client(
                                    id = existing?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(), curp = curp.trim(), rfc = rfc.trim(), nss = nss.trim(),
                                    phone = phone.trim(), email = email.trim(), afore = afore.trim(), notes = notes.trim(),
                                    documents = existing?.documents.orEmpty()
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar expediente") }
            }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") } }
        }
    }
}

@Composable
private fun Field(label: String, value: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = singleLine)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDetailScreen(
    client: Client,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onBrowser: () -> Unit,
    onUpdate: (Client) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var documentLabel by remember { mutableStateOf("Documento") }
    var confirmDelete by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onUpdate(
                client.copy(
                    documents = client.documents + ClientDocument(
                        label = documentLabel.ifBlank { "Documento" },
                        uri = uri.toString(),
                        mimeType = context.contentResolver.getType(uri).orEmpty()
                    )
                )
            )
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(client.name) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Info("CURP", client.curp); Info("RFC", client.rfc); Info("NSS", client.nss)
                        Info("Teléfono", client.phone); Info("Correo", client.email); Info("AFORE", client.afore)
                        Info("Observaciones", client.notes)
                    }
                }
            }
            item { Text("Documentos", style = MaterialTheme.typography.titleLarge) }
            item { Field("Tipo o nombre del documento", documentLabel) { documentLabel = it } }
            item { Button(onClick = { picker.launch(arrayOf("application/pdf", "image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Agregar PDF o imagen") } }
            items(client.documents, key = { it.id }) { doc ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(doc.label, style = MaterialTheme.typography.titleMedium)
                            Text(doc.mimeType.ifBlank { "Archivo" }, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(doc.uri), doc.mimeType.ifBlank { "*/*" })
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            }
                        }) { Text("Abrir") }
                        TextButton(onClick = { onUpdate(client.copy(documents = client.documents.filterNot { it.id == doc.id })) }) { Text("Quitar") }
                    }
                }
            }
            item { Button(onClick = onBrowser, modifier = Modifier.fillMaxWidth()) { Text("Abrir portal web") } }
            item { OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Editar datos") } }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver a clientes") } }
            item { TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar expediente") } }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar expediente") },
            text = { Text("Se eliminarán los datos guardados de ${client.name}. Los archivos originales no se borrarán.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Info(label: String, value: String) {
    if (value.isNotBlank()) Text("$label: $value")
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserScreen(client: Client?, onBack: () -> Unit) {
    var addressText by remember { mutableStateOf("") }
    var loadedAddress by remember { mutableStateOf<String?>(null) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        fileCallback = null
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Portal web${client?.name?.let { " · $it" } ?: ""}") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = addressText,
                    onValueChange = { addressText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Dirección HTTPS del portal") },
                    singleLine = true
                )
                Button(onClick = {
                    val text = addressText.trim()
                    loadedAddress = when {
                        text.startsWith("https://") -> text
                        text.startsWith("http://") -> text.replaceFirst("http://", "https://")
                        text.isNotBlank() -> "https://$text"
                        else -> null
                    }
                }) { Text("Ir") }
            }
            Text(
                "La selección de archivos funciona cuando el portal ofrece Subir archivo. La verificación facial conserva la cámara real.",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
            val address = loadedAddress
            if (address == null) {
                Card(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Escribe la dirección oficial del portal para abrirlo de forma segura.", Modifier.padding(16.dp))
                }
                Spacer(Modifier.weight(1f))
            } else {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    callback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: WebChromeClient.FileChooserParams?
                                ): Boolean {
                                    fileCallback?.onReceiveValue(null)
                                    fileCallback = callback
                                    return try {
                                        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "*/*"
                                        }
                                        chooser.launch(intent)
                                        true
                                    } catch (_: Exception) {
                                        fileCallback = null
                                        false
                                    }
                                }
                            }
                            loadUrl(address)
                        }
                    },
                    update = { webView -> if (webView.url != address) webView.loadUrl(address) },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Volver al expediente") }
        }
    }
}

private object ClientStore {
    fun load(context: Context): List<Client> = runCatching {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLIENTS_KEY, "[]") ?: "[]"
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) add(clientFromJson(array.getJSONObject(index)))
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, clients: List<Client>) {
        val array = JSONArray()
        clients.forEach { array.put(clientToJson(it)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLIENTS_KEY, array.toString()).apply()
    }

    private fun clientToJson(client: Client) = JSONObject().apply {
        put("id", client.id); put("name", client.name); put("curp", client.curp); put("rfc", client.rfc)
        put("nss", client.nss); put("phone", client.phone); put("email", client.email); put("afore", client.afore)
        put("notes", client.notes)
        put("documents", JSONArray().apply {
            client.documents.forEach { document ->
                put(JSONObject().apply {
                    put("id", document.id); put("label", document.label); put("uri", document.uri); put("mimeType", document.mimeType)
                })
            }
        })
    }

    private fun clientFromJson(json: JSONObject): Client {
        val documentsJson = json.optJSONArray("documents") ?: JSONArray()
        val documents = buildList {
            for (index in 0 until documentsJson.length()) {
                val document = documentsJson.optJSONObject(index) ?: continue
                add(
                    ClientDocument(
                        id = document.optString("id", UUID.randomUUID().toString()),
                        label = document.optString("label", "Documento"),
                        uri = document.optString("uri"),
                        mimeType = document.optString("mimeType")
                    )
                )
            }
        }
        return Client(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.optString("name"), curp = json.optString("curp"), rfc = json.optString("rfc"),
            nss = json.optString("nss"), phone = json.optString("phone"), email = json.optString("email"),
            afore = json.optString("afore"), notes = json.optString("notes"), documents = documents
        )
    }
}
