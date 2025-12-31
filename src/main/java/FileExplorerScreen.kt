import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// QUESTA FUNZIONE MANCAVA NEL TUO FILE
fun openFile(context: Context, file: File) {
    val uri = try {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } catch (e: Exception) {
        Toast.makeText(context, "Error: Could not get URI for file.", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        val mimeType = context.contentResolver.getType(uri)
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this file.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MainFileExplorerScreen(fileViewModel: FileViewModel = viewModel()) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val checkPermission = {
            hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        checkPermission()
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                checkPermission()
            }
        }
        (context as? LifecycleOwner)?.lifecycle?.addObserver(lifecycleObserver)
        onDispose {
            (context as? LifecycleOwner)?.lifecycle?.removeObserver(lifecycleObserver)
        }
    }

    if (hasPermission) {
        val files by fileViewModel.files.collectAsState()
        val currentPath by fileViewModel.currentPath.collectAsState()
        val searchQuery by fileViewModel.searchQuery.collectAsState()
        val sortType by fileViewModel.sortType.collectAsState()

        FileExplorerUI(
            currentPath = currentPath,
            files = files,
            searchQuery = searchQuery,
            sortType = sortType,
            onSortChange = { fileViewModel.changeSortType(it) },
            onSearchQueryChange = { fileViewModel.onSearchQueryChanged(it) },
            onFileClick = { file ->
                if (file.isDirectory) {
                    fileViewModel.navigateTo(file)
                } else {
                    openFile(context, file)
                }
            },
            onNavigateUp = { fileViewModel.navigateUp() },
            canNavigateUp = fileViewModel.canNavigateUp(),
            viewModel = fileViewModel
        )
    } else {
        PermissionRequestScreen {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            context.startActivity(intent)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileExplorerUI(
    currentPath: String,
    files: List<File>,
    searchQuery: String,
    sortType: SortType,
    onSortChange: (SortType) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFileClick: (File) -> Unit,
    onNavigateUp: () -> Unit,
    canNavigateUp: Boolean,
    viewModel: FileViewModel
) {
    var fileForAction by remember { mutableStateOf<File?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                SearchAppBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = {
                        isSearchActive = false
                        onSearchQueryChange("")
                    }
                )
            } else {
                DefaultAppBar(
                    title = if (File(currentPath).name == "0") "Storage" else File(currentPath).name,
                    onSearchClick = { isSearchActive = true },
                    onNavigateUp = onNavigateUp,
                    canNavigateUp = canNavigateUp,
                    sortType = sortType,
                    onSortChange = onSortChange
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            LazyColumn {
                items(files) { file ->
                    FileListItem(
                        file = file,
                        onItemClick = { onFileClick(it) },
                        onItemLongClick = {
                            fileForAction = it
                            showContextMenu = true
                        }
                    )
                }
            }
            if (showContextMenu) {
                FileContextMenu(
                    onDismiss = { showContextMenu = false },
                    onDeleteClick = {
                        fileForAction?.let { viewModel.deleteFile(it) }
                        showContextMenu = false
                    },
                    onRenameClick = {
                        showRenameDialog = true
                        showContextMenu = false
                    },
                    onPropertiesClick = {
                        showPropertiesDialog = true
                        showContextMenu = false
                    }
                )
            }
        }
    }

    if (showRenameDialog) {
        fileForAction?.let { file ->
            RenameDialog(file = file, onConfirm = { newName ->
                viewModel.renameFile(file, newName)
                showRenameDialog = false
            }, onDismiss = { showRenameDialog = false })
        }
    }

    if (showPropertiesDialog) {
        fileForAction?.let { file ->
            PropertiesDialog(file = file, onDismiss = { showPropertiesDialog = false })
        }
    }
}

/**
 * MODIFICATO: La TopAppBar ora ha anche il menu di ordinamento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultAppBar(
    title: String,
    onSearchClick: () -> Unit,
    onNavigateUp: () -> Unit,
    canNavigateUp: Boolean,
    sortType: SortType,
    onSortChange: (SortType) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (canNavigateUp) {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Up")
                }
            }
        },
        actions = {
            // Icona di ricerca
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            // NUOVO: Icona e menu per l'ordinamento
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Sort Options")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sort by Name") },
                        onClick = {
                            onSortChange(SortType.NAME)
                            showSortMenu = false
                        },
                        // Evidenzia l'opzione attiva
                        leadingIcon = {
                            if (sortType == SortType.NAME) {
                                Icon(Icons.Default.Check, contentDescription = "Selected")
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by Date") },
                        onClick = {
                            onSortChange(SortType.DATE)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortType == SortType.DATE) {
                                Icon(Icons.Default.Check, contentDescription = "Selected")
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by Size") },
                        onClick = {
                            onSortChange(SortType.SIZE)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortType == SortType.SIZE) {
                                Icon(Icons.Default.Check, contentDescription = "Selected")
                            }
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TopAppBar(
        title = {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text("Search in this folder...", color = Color.Gray)
                    }
                    innerTextField()
                }
            )
        },
        navigationIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.padding(start = 16.dp)
            )
        },
        actions = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close Search")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(
    file: File,
    onItemClick: (File) -> Unit,
    onItemLongClick: (File) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onItemClick(file) },
                onLongClick = { onItemLongClick(file) }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun FileContextMenu(
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit,
    onPropertiesClick: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(text = { Text("Rename") }, onClick = onRenameClick)
        DropdownMenuItem(text = { Text("Delete") }, onClick = onDeleteClick)
        DropdownMenuItem(text = { Text("Properties") }, onClick = onPropertiesClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameDialog(file: File, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var newName by remember { mutableStateOf(file.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (newName.isNotBlank()) onConfirm(newName) }) {
                Text("Rename")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PropertiesDialog(file: File, onDismiss: () -> Unit) {
    val size = if (file.isDirectory) {
        "N/A"
    } else {
        android.text.format.Formatter.formatShortFileSize(LocalContext.current, file.length())
    }
    val lastModified = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Properties") },
        text = {
            Column {
                Text("Name: ${file.name}")
                Text("Path: ${file.path}")
                Text("Size: $size")
                Text("Last Modified: $lastModified")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This app needs 'All Files Access' to browse your device's storage. Please grant the permission in the next screen.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}
