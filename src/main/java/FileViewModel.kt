import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import java.io.File

// NUOVO: Definiamo i criteri di ordinamento
enum class SortType {
    NAME,
    DATE,
    SIZE
}

class FileViewModel : ViewModel() {

    private val rootPath = Environment.getExternalStorageDirectory().path

    private val _currentPath = MutableStateFlow(rootPath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // NUOVO: StateFlow per il tipo di ordinamento, di default per nome
    private val _sortType = MutableStateFlow(SortType.NAME)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _allFiles = MutableStateFlow<List<File>>(emptyList())

    // MODIFICATO: Il combine ora usa anche _sortType per ordinare i file
    val files: StateFlow<List<File>> = combine(_allFiles, _searchQuery, _sortType) { files, query, sort ->
        val filteredList = if (query.isBlank()) {
            files
        } else {
            files.filter { it.name.contains(query, ignoreCase = true) }
        }

        // Applica l'ordinamento
        when (sort) {
            SortType.NAME -> filteredList.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            SortType.DATE -> filteredList.sortedWith(compareBy({ !it.isDirectory }, { -it.lastModified() }))
            SortType.SIZE -> filteredList.sortedWith(compareBy({ !it.isDirectory }, { -it.length() }))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadFilesForCurrentPath()
    }

    private fun loadFilesForCurrentPath() {
        val directory = File(_currentPath.value)
        _allFiles.value = directory.listFiles()?.toList() ?: emptyList()
        // L'ordinamento viene ora gestito dal `combine`
    }

    // NUOVO: Funzione per cambiare il tipo di ordinamento
    fun changeSortType(newSortType: SortType) {
        _sortType.value = newSortType
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun navigateTo(directory: File) {
        if (directory.isDirectory) {
            _currentPath.value = directory.path
            _searchQuery.value = ""
            loadFilesForCurrentPath()
        }
    }

    fun navigateUp() {
        val currentFile = File(_currentPath.value)
        val parentPath = currentFile.parent
        if (parentPath != null && parentPath.startsWith(rootPath)) {
            _currentPath.value = parentPath
            _searchQuery.value = ""
            loadFilesForCurrentPath()
        }
    }

    fun canNavigateUp(): Boolean {
        return _currentPath.value != rootPath && File(_currentPath.value).parent != null
    }

    fun deleteFile(file: File) {
        try {
            if (if (file.isDirectory) file.deleteRecursively() else file.delete()) {
                loadFilesForCurrentPath()
            }
        } catch (e: Exception) {
            println("Error deleting file: ${e.message}")
        }
    }

    fun renameFile(from: File, newName: String) {
        try {
            if (from.renameTo(File(from.parent, newName))) {
                loadFilesForCurrentPath()
            }
        } catch (e: Exception) {
            println("Error renaming file: ${e.message}")
        }
    }
}
