package com.lab2_contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                ContactsScreen()
            }
        }
    }
}

private data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String?,
    val email: String?,
)

@Composable
private fun ContactsScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }
    var permissionRequested by remember { mutableStateOf(false) }

    LaunchedEffect(hasPermission, permissionRequested) {
        if (!hasPermission && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val contacts by produceState(initialValue = emptyList<Contact>(), hasPermission) {
        value = if (hasPermission) context.fetchAllContacts() else emptyList()
    }

    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                !hasPermission -> PermissionState(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                )

                contacts.isEmpty() -> EmptyContactsState()

                else -> ContactsList(
                    contacts = contacts,
                    onContactClick = { selectedContact = it }
                )
            }
        }
    }

    selectedContact?.let { contact ->
        ContactDetailsDialog(
            contact = contact,
            onDismiss = { selectedContact = null }
        )
    }
}

@Composable
private fun PermissionState(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_contacts_permission),
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.grant_permission))
        }
    }
}

@Composable
private fun EmptyContactsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_contacts_found),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ContactsList(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = contacts, key = { it.id }) { contact ->
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContactClick(contact) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ContactDetailsDialog(
    contact: Contact,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = contact.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(
                        R.string.phone_template,
                        contact.phoneNumber ?: stringResource(R.string.not_specified)
                    )
                )
                Text(
                    text = stringResource(
                        R.string.email_template,
                        contact.email ?: stringResource(R.string.not_specified)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        }
    )
}

private fun Context.fetchAllContacts(): List<Contact> {
    val contactsMap = linkedMapOf<Long, Contact>()

    contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        ),
        null,
        null,
        "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        if (idIndex < 0 || nameIndex < 0) return emptyList()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val name = cursor.getString(nameIndex).orEmpty().ifBlank {
                getString(R.string.unnamed_contact)
            }
            contactsMap[id] = Contact(
                id = id,
                name = name,
                phoneNumber = readFirstPhoneByContactId(id),
                email = readFirstEmailByContactId(id)
            )
        }
    }

    return contactsMap.values.toList()
}

private fun Context.readFirstPhoneByContactId(contactId: Long): String? {
    return contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
    )?.use { cursor ->
        val columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        if (columnIndex < 0 || !cursor.moveToFirst()) return@use null
        cursor.getString(columnIndex)
    }
}

private fun Context.readFirstEmailByContactId(contactId: Long): String? {
    return contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
    )?.use { cursor ->
        val columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        if (columnIndex < 0 || !cursor.moveToFirst()) return@use null
        cursor.getString(columnIndex)
    }
}