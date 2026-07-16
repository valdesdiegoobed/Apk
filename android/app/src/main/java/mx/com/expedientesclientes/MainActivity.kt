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

private enum class Screen { CLIENTS, EDIT_CLIENT, CLIENT_DETAIL, BROWSER }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val clients = remember { mutableStateListOf<Client>() }
    var screen by remember { mutableStateOf(Screen.CLIENTS) }
    var selectedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        clients.clear()
        clients.addAll(ClientStore.load(context))
    }

    fun saveAll() = ClientStore.save(context, clients)
    fun selectedClient(): Client? = clients.firstOrNull { it.id == selectedId }

    when (screen) {
        Screen.CLIENTS -> ClientsScreen(
            clients = clients,
            onNew = { selectedId = null; screen = Screen.EDIT_CLIENT },
            onOpen = { selectedId = it; screen = Screen.CLIENT_DETAIL }
        )

        Screen.EDIT_CLIENT -> ClientFormScreen(
            existing = selectedClient(),
            onBack = { screen = if (selectedId == null) Screen.CLIENTS else Screen.CLIENT_DETAIL },
            onSave = { client ->
                val index = clients.indexOfFirst { it.id == client.id }
                if (index >= 0) clients[index] = client else clients.add(client)
                selectedId = client.id
                saveAll()
                screen = Screen.CLIENT_DETAIL
            }
        )

        Screen.CLIENT_DETAIL -> {
            val client = selectedClient()
            if (client == null) {
                screen = Screen.CLIENTS
            } else {
                ClientDetailScreen(
                    client = client,
                    onBack = { screen = Screen.CLIENTS },
                    onEdit = { screen = Screen.EDIT_CLIENT },
                    onOpenBrowser = { screen = Screen.BROWSER },
                    onUpdate = { updated ->
                        val index = clients.indexOfFirst { it.id == updated.id }
                        if (index >= 0) clients[index] = updated
                        saveAll()
                    },
                    onDelete = {
                        clients.removeAll { it.id == client.id }
                        saveAll()
                        selectedId = null
                        screen = Screen.CLIENTS
                    }
                )
            }
        }

        Screen.BROWSER -> BrowserScreen(
            client = selectedClient(),
            onBack = { screen = Screen.CLIENT_DETAIL }
        )
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
                        if (name.isBlank()) error = "El nombre es obligatorio."
                        else onSave(
                            Client(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(), curp = curp.trim(), rfc = rfc.trim(), nss = nss.trim(),
                                phone = phone.trim(), email = email.trim(), afore = afore.trim(), notes = notes.trim(),
                                documents = existing?.documents.orEmpty()
                            )
                        )
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
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDetailScreen(
    client: Client,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenBrowser: () -> Unit,
    onUpdate: (Client) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var pendingLabel by remember { mutableStateOf("Documento") }
    var showDelete by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            val mime = context.contentResolver.getType(uri).orEmpty()
            onUpdate(client.copy(documents = client.documents + ClientDocument(label = pendingLabel, uri = uri.toString(), mimeType = mime)))
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
            item {
                OutlinedTextField(
                    value = pendingLabel,
                    onValueChange = { pendingLabel = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tipo o nombre del documento") },
                    singleLine = true
                )
            }
            item {
                Button(
                    onClick = { picker.launch(arrayOf("application/pdf", "image/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Agregar PDF o imagen") }
            }
            items(client.documents, key = { it.id }) { doc ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(doc.label, style = MaterialTheme.typography.titleMedium)
                            Text(doc.mimeType.ifBlank { "Archivo" }, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(doc.uri)).apply {
                                    setDataAndType(Uri.parse(doc.uri), doc.mimeType.ifBlank { "*/*" })
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            }
                        }) { Text("Abrir") }
                        TextButton(onClick = { onUpdate(client.copy(documents = client.documents.filterNot { it.id == doc.id })) }) {
                            Text("Quitar")
                        }
                    }
                }
            }
            item { Button(onClick = onOpenBrowser, modifier = Modifier.fillMaxWidth()) { Text("Abrir portal web") } }
            item { OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Editar datos") } }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver a clientes") } }
            item { TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar expediente") } }
        }
    }

    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text("Eliminar expediente") },
        text = { Text("Se eliminarán los datos guardados de ${client.name}. Los archivos originales del teléfono no se borrarán.") },
        confirmButton = { TextButton(onClick = onDelete) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancelar") } }
    )
}

@Composable
private fun Info(label: String, value: String) {
    if (value.isNotBlank()) Text("$label: $value")
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserScreen(client: Client?, onBack: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var loadedUrl by remember { mutableStateOf<String?>(null) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        fileCallback?.onReceiveValue(uris)
        fileCallback = null
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Portal web${client?.name?.let { " · $it" } ?: ""}") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Dirección HTTPS del portal") },
                    singleLine = true
                )
                Button(onClick = {
                    val trimmed = url.trim()
                    loadedUrl = when {
                        trimmed.startsWith("https://") -> trimmed
                        trimmed.startsWith("http://") -> trimmed.replaceFirst("http://", "https://")
                        trimmed.isNotBlank() -> "https://$trimmed"
                        else -> null
                    }
                }) { Text("Ir") }
            }
            Text(
                "La selección de archivos funciona cuando el portal ofrece Subir archivo. La verificación facial conserva la cámara real.",
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
            loadedUrl?.let { address ->
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    callback: ValueCallback<Array<Uri>>?,
                                    params: FileChooserParams?
                                ): Boolean {
                                    fileCallback?.onReceiveValue(null)
                                    fileCallback = callback
                                    return try {
                                        chooser.launch(params?.createIntent() ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "*/*"
                                        })
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
                    update = { webView -> if (webView.url != address) webView.loadUrl(address) }
                )
            } ?: Card(Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Escribe la dirección oficial del portal para abrirlo de forma segura.", Modifier.padding(16.dp))
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Volver al expediente") }
        }
    }
}

private object ClientStore {
    fun load(context: Context): List<Client> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLIENTS_KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) add(clientFromJson(array.getJSONObject(i)))
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, clients: List<Client>) {
        val array = JSONArray()
        clients.forEach { array.put(clientToJson(it)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLIENTS_KEY, array.toString()).apply()
    }

    private fun clientToJson(c: Client) = JSONObject().apply {
        put("id", c.id); put("name", c.name); put("curp", c.curp); put("rfc", c.rfc); put("nss", c.nss)
        put("phone", c.phone); put("email", c.email); put("afore", c.afore); put("notes", c.notes)
        put("documents", JSONArray().apply { c.documents.forEach { d -> put(JSONObject().apply {
            put("id", d.id); put("label", d.label); put("uri", d.uri); put("mimeType", d.mimeType)
        }) } })
    }

    private fun clientFromJson(o: JSONObject): Client {
        val docs = o.optJSONArray("documents") ?: JSONArray()
        return Client(
            id = o.optString("id", UUID.randomUUID().toString()), name = o.optString("name"),
            curp = o.optString("curp"), rfc = o.optString("rfc"), nss = o.optString("nss"),
            phone = o.optString("phone"), email = o.optString("email"), afore = o.optString("afore"), notes = o.optString("notes"),
            documents = buildList {
                for (i in 0 until docs.length()) docs.optJSONObject(i)?.let { d -> add(ClientDocument(
                    id = d.optString("id", UUID.randomUUID().toString()), label = d.optString("label", "Documento"),
                    uri = d.optString("uri"), mimeType = d.optString("mimeType")
                )) }
            }
        )
    }
}
