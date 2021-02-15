package com.example.uberriderremake

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object UserUtils {
    fun updateUser(view: View?, updateData: Map<String, Any>) {
        FirebaseDatabase
            .getInstance()
            .getReference(RIDER_INFO_REFERENCE).child(FirebaseAuth.getInstance().currentUser!!.uid)

            .updateChildren(updateData)
            .addOnFailureListener {
                Snackbar.make(view!!, it.message.toString(), Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener {
                Snackbar.make(view!!, "Update Information Successful", Snackbar.LENGTH_LONG)
            }
    }

    fun updateToken(context: Context, token: String) {
        val newToken = TokenModel(token)


        FirebaseDatabase
            .getInstance()
            .getReference(TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(newToken)
            .addOnFailureListener{
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener {

            }
    }
}