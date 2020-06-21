package timifeoluwa.example.tasktimer.database

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Task(val name: String, val description: String, val sortOrder: Int, var id: Long = 0) :
    Parcelable {
    //as an input, it doesn't have an id yet, so on comparison with the data base it fails and doesn't add to the database, so it needed to be removed from the primary constructor.
}