package com.example.smartgreecealert.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ListView;
import android.widget.Toast;

import com.example.smartgreecealert.R;
import com.example.smartgreecealert.adapters.ContactAdapter;
import com.example.smartgreecealert.helpers.ContactNameSorter;
import com.example.smartgreecealert.models.Contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContactActivity extends AppCompatActivity {

    private ListView contactsListView;
    private ContactAdapter contactAdapter;
    private List<Contact> contacts;
    private SharedPreferences savedContacts;
    private SharedPreferences.Editor contactsEditor;
    public static final String PREFS_CONTACTS = "Contacts";
    private ArrayList<String> savedContactsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
    }

    @Override
    protected void onStart() {
        super.onStart();
        contacts = getContacts(this);
        contactsListView = findViewById(R.id.listContact);
        savedContacts = getSharedPreferences(PREFS_CONTACTS, MODE_PRIVATE);
        Map<String, ?> keys = savedContacts.getAll();
        savedContactsList = new ArrayList<>();
        for(Map.Entry<String, ?> entry : keys.entrySet()) {
            savedContactsList.add(entry.getValue().toString());
        }
        contactsEditor = getSharedPreferences(PREFS_CONTACTS, MODE_PRIVATE).edit();
        contactsEditor.apply();

        // Add contacts to list
        contactAdapter = new ContactAdapter(contacts, savedContactsList, contactsEditor, this);
        contactsListView.setAdapter(contactAdapter);
    }


    /**
     * Gets user contacts (id, name and number)
     *
     * @param ctx Context
     * @return List<Contact>
     */
    public List<Contact> getContacts(Context ctx) {
        List<Contact> list = new ArrayList<>();
        ContentResolver contentResolver = ctx.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor cursorInfo = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    while (cursorInfo.moveToNext()) {
                        Contact info = new Contact(id,
                                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
                                cursorInfo.getString(cursorInfo.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                        list.add(info);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        list.sort(new ContactNameSorter());
                    }
                    cursorInfo.close();
                }
            }
            cursor.close();
        } else {
            Toast.makeText(ContactActivity.this, R.string.noContacts,
                    Toast.LENGTH_SHORT).show();
        }
        return list;
    }
}