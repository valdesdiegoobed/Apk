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

    LaunchedEffect(Unit) { clients.addAll(ClientStore.load(context)) }
    fun save() = ClientStore.save(context, clients)
    fun selected() = clients.firstOrNull { it.id == selectedId }

    when (screen) {
        Screen.CLIENTS -> ClientsScreen(clients, { selectedId = null; screen = Screen.FORM }) { selectedId = it; screen = Screen.DETAIL }
        Screen.FORM -> ClientFormScreen(selected(), { screen = if (selectedId == null) Screen.CLIENTS else Screen.DETAIL }) { client ->
            val index = clients.indexOfFirst { it.id == client.id }
            if (index >= 0) clients[index] = client else clients.add(client)
            selectedId = client.id; save(); screen = Screen.DETAIL
        }
        Screen.DETAIL -> selected()?.let { client ->
            ClientDetailScreen(client, { screen = Screen.CLIENTS }, { screen = Screen.FORM }, { screen = Screen.BROWSER }, { updated ->
                val index = clients.indexOfFirst { it.id == updated.id }; if (index >= 0) clients[index] = updated; save()
            }) {
                clients.removeAll { it.id == client.id }; save(); selectedId = null; screen = Screen.CLIENTS
            }
        } ?: run { screen = Screen.CLIENTS }
        Screen.BROWSER -> BrowserScreen(selected()) { screen = Screen.DETAIL }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientsScreen(clients: List<Client>, onNew: () -> Unit, onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = clients.filter { query.isBlank() || it.name.contains(query, true) || it.curp.contains(query, true) || it.rfc.contains(query, true) }
    Scaffold(topBar = { TopAppBar(title = { Text("Expediente Clientes") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Buscar nombre, CURP o RFC") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("Nuevo cliente") }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (filtered.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Text(if (clients.isEmpty()) "Aún no hay clientes." else "Sin resultados.", Modifier.padding(16.dp)) } }
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
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Field("Nombre completo *", name) { name = it } }; item { Field("CURP", curp) { curp = it.uppercase() } }
            item { Field("RFC", rfc) { rfc = it.uppercase() } }; item { Field("NSS", nss) { nss = it } }
            item { Field("Teléfono", phone) { phone = it } }; item { Field("Correo", email) { email = it } }
            item { Field("AFORE", afore) { afore = it } }; item { Field("Observaciones", notes, false) { notes = it } }
            if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
            item { Button(onClick = {
                if (name.isBlank()) error = "El nombre es obligatorio." else onSave(Client(existing?.id ?: UUID.randomUUID().toString(), name.trim(), curp.trim(), rfc.trim(), nss.trim(), phone.trim(), email.trim(), afore.trim(), notes.trim(), existing?.documents.orEmpty()))
            }, modifier = Modifier.fillMaxWidth()) { Text("Guardar expediente") } }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") } }
        }
    }
}

@Composable
private fun Field(label: String, value: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = singleLine)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDetailScreen(client: Client, onBack: () -> Unit, onEdit: () -> Unit, onBrowser: () -> Unit, onUpdate: (Client) -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("Documento") }
    var confirmDelete by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onUpdate(client.copy(documents = client.documents + ClientDocument(label = label.ifBlank { "Documento" }, uri = uri.toString(), mimeType = context.contentResolver.getType(uri).orEmpty())))
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(client.name) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Info("CURP", client.curp); Info("RFC", client.rfc); Info("NSS", client.nss); Info("Teléfono", client.phone); Info("Correo", client.email); Info("AFORE", client.afore); Info("Observaciones", client.notes) } } }
            item { Text("Documentos", style = MaterialTheme.typography.titleLarge) }
            item { Field("Nombre del documento", label) { label = it } }
            item { Button(onClick = { picker.launch(arrayOf("application/pdf", "image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Agregar PDF o imagen") } }
            items(client.documents, key = { it.id }) { doc ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text(doc.label, style = MaterialTheme.typography.titleMedium); Text(doc.mimeType.ifBlank { "Archivo" })
                    Row { TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(doc.uri), doc.mimeType.ifBlank { "*/*" }); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } }) { Text("Abrir") }; TextButton(onClick = { onUpdate(client.copy(documents = client.documents.filterNot { it.id == doc.id })) }) { Text("Quitar") } }
                } }
            }
            item { Button(onClick = onBrowser, modifier = Modifier.fillMaxWidth()) { Text("Abrir portal web") } }
            item { OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Editar datos") } }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") } }
            item { TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar expediente") } }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Eliminar expediente") }, text = { Text("Se eliminarán los datos guardados; los archivos originales no se borrarán.") }, confirmButton = { TextButton(onClick = onDelete) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } })
}

@Composable private fun Info(label: String, value: String) { if (value.isNotBlank()) Text("$label: $value") }

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserScreen(client: Client?, onBack: () -> Unit) {
    var url by remember { mutableStateOf("") }; var loaded by remember { mutableStateOf<String?>(null) }
    var callback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> callback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)); callback = null }
    Scaffold(topBar = { TopAppBar(title = { Text("Portal web${client?.name?.let { " · $it" } ?: ""}") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(8.dp)) {
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Dirección HTTPS oficial") }, singleLine = true)
            Button(onClick = { val t = url.trim(); loaded = when { t.startsWith("https://") -> t; t.startsWith("http://") -> t.replaceFirst("http://", "https://"); t.isNotBlank() -> "https://$t"; else -> null } }, modifier = Modifier.fillMaxWidth()) { Text("Abrir") }
            Text("El selector funciona cuando el portal permite subir archivos. El reconocimiento facial usa la cámara real.", style = MaterialTheme.typography.bodySmall)
            loaded?.let { address -> AndroidView(modifier = Modifier.fillMaxWidth().height(560.dp), factory = { context -> WebView(context).apply {
                settings.javaScriptEnabled = true; settings.domStorageEnabled = true; webViewClient = WebViewClient(); webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                        callback?.onReceiveValue(null); callback = filePathCallback
                        return runCatching { chooser.launch(fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }); true }.getOrDefault(false)
                    }
                }; loadUrl(address)
            } }, update = { if (it.url != address) it.loadUrl(address) }) }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver al expediente") }
        }
    }
}

private object ClientStore {
    fun load(context: Context): List<Client> = runCatching { val a = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLIENTS_KEY, "[]") ?: "[]"); buildList { for (i in 0 until a.length()) add(fromJson(a.getJSONObject(i))) } }.getOrDefault(emptyList())
    fun save(context: Context, clients: List<Client>) { val a = JSONArray(); clients.forEach { a.put(toJson(it)) }; context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLIENTS_KEY, a.toString()).apply() }
    private fun toJson(c: Client) = JSONObject().apply { put("id", c.id); put("name", c.name); put("curp", c.curp); put("rfc", c.rfc); put("nss", c.nss); put("phone", c.phone); put("email", c.email); put("afore", c.afore); put("notes", c.notes); put("documents", JSONArray().apply { c.documents.forEach { d -> put(JSONObject().apply { put("id", d.id); put("label", d.label); put("uri", d.uri); put("mimeType", d.mimeType) }) } }) }
    private fun fromJson(o: JSONObject): Client { val docs = o.optJSONArray("documents") ?: JSONArray(); return Client(o.optString("id", UUID.randomUUID().toString()), o.optString("name"), o.optString("curp"), o.optString("rfc"), o.optString("nss"), o.optString("phone"), o.optString("email"), o.optString("afore"), o.optString("notes"), buildList { for (i in 0 until docs.length()) docs.optJSONObject(i)?.let { d -> add(ClientDocument(d.optString("id", UUID.randomUUID().toString()), d.optString("label", "Documento"), d.optString("uri"), d.optString("mimeType"))) } }) }
}
