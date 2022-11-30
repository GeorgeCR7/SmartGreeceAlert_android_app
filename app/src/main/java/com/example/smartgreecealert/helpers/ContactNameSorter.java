package com.example.smartgreecealert.helpers;

import com.example.smartgreecealert.models.Contact;

import java.util.Comparator;

/**
 * Sorts contacts
 */
public class ContactNameSorter implements Comparator<Contact>
{
    @Override
    public int compare(Contact o1, Contact o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
    }
}
