package com.example.smartgreecealert.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.smartgreecealert.R;
import com.example.smartgreecealert.models.Contact;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends ArrayAdapter<Contact> {
    private List<Contact> contacts;
    private ArrayList<String> savedContactsList;
    private Context context;
    private SharedPreferences.Editor contactsEditor;

    public ContactAdapter(List<Contact> contacts, ArrayList<String> savedContactsList, SharedPreferences.Editor contactsEditor, Context context) {
        super(context, R.layout.single_contact, contacts);
        this.context = context;
        this.contacts = contacts;
        this.savedContactsList = savedContactsList;
        this.contactsEditor = contactsEditor;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View row, @NonNull ViewGroup parent) {
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        if (row == null) row = inflater.inflate(R.layout.single_contact, parent, false);

        // Set contact name
        TextView contactName = row.findViewById(R.id.nameContact);
        contactName.setText(contacts.get(position).getName());

        // Set contact number
        TextView contactNumber = row.findViewById(R.id.numberContact);
        contactNumber.setText(contacts.get(position).getMobileNumber());

        CheckBox contactCheckbox = row.findViewById(R.id.checkboxContact);

        // Check checkbox if contact is in shared preferences
        contactCheckbox.setChecked(savedContactsList.contains(parsePhoneNumber(contactNumber.getText().toString())));

        // Add or remove contact from shared preferences
        contactCheckbox.setOnClickListener(event -> {
            String phoneNumber = parsePhoneNumber(contacts.get(position).getMobileNumber());
            if (contactCheckbox.isChecked()) {
                contactsEditor.putString(phoneNumber, phoneNumber);
                savedContactsList.add(phoneNumber);
            } else {
                contactsEditor.remove(phoneNumber);
                savedContactsList.remove(phoneNumber);
            }
            contactsEditor.apply();
        });

        return row;
    }

    public String parsePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("\\D+","");
    }
}
