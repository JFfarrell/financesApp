package com.example.personalfinances.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/** A file inside the backup folder. */
data class FolderEntry(val name: String, val uri: Uri)

/**
 * The folder the user chose for automatic backups, accessed through Android's Storage Access
 * Framework. The user picks the folder once and the app keeps a persistable permission for it, so
 * later writes need no prompt. It works on any storage provider, including removable and
 * cloud-synced folders.
 *
 * Only the operations the backup needs are provided: read the folder's name, list its files,
 * write a file (replacing one of the same name) and delete a file.
 */
class BackupFolder(private val context: Context, private val treeUri: Uri) {
    private val resolver = context.contentResolver
    private val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
    private val folderUri: Uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
    private val childrenUri: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)

    /** The folder's display name (for example "Backups"), or null if it cannot be read. */
    fun displayName(): String? = try {
        resolver.query(folderUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    /** The files (and folders) directly inside the backup folder. */
    fun list(): List<FolderEntry> {
        val entries = mutableListOf<FolderEntry>()
        resolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                entries += FolderEntry(
                    name = cursor.getString(1) ?: continue,
                    uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(0))
                )
            }
        }
        return entries
    }

    /**
     * Writes [bytes] to a file called [name], replacing it if it exists. An existing file is looked
     * up first because creating a second file with the same name would make the provider rename
     * it ("name (1).json"). Returns false if the file could not be created or written.
     */
    fun write(name: String, bytes: ByteArray): Boolean {
        val target = list().firstOrNull { it.name == name }?.uri
            ?: DocumentsContract.createDocument(resolver, folderUri, "application/json", name)
            ?: return false
        // "wt" truncates, so replacing a longer file leaves no stale tail.
        val output = resolver.openOutputStream(target, "wt") ?: return false
        output.use { it.write(bytes) }
        return true
    }

    fun delete(entry: FolderEntry): Boolean = try {
        DocumentsContract.deleteDocument(resolver, entry.uri)
    } catch (e: Exception) {
        false
    }
}
