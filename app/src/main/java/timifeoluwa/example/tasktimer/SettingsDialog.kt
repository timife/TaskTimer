package timifeoluwa.example.tasktimer

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import kotlinx.android.synthetic.main.settings_dialog.*
import timifeoluwa.example.tasktimer.database.CONTENT_AUTHORITY
import timifeoluwa.example.tasktimer.database.CONTENT_AUTHORITY_URI
import java.util.*

private const val TAG = "SettingsDialog"

const val SETTINGS_FIRST_DAY_OF_WEEK = "FirstDay"
const val SETTINGS_IGNORE_LESS_THAN = "IgnoreLessThan"
const val SETTINGS_DEFAULT_IGNORE_LESS_THAN = 0

//                                0  1  2  3  4  5  6  7  8  9  10  11  12  13  14  15  16  17  18  19  20  21  22   23    24
private val deltas = intArrayOf(
    0,
    5,
    10,
    15,
    20,
    25,
    30,
    35,
    40,
    45,
    50,
    55,
    60,
    120,
    180,
    240,
    300,
    360,
    420,
    480,
    540,
    600,
    900,
    1800,
    2700
)

class SettingsDialog : AppCompatDialogFragment() {
    private val defaultFirstDayOfWeek = GregorianCalendar(Locale.getDefault()).firstDayOfWeek
    private var firstDay = defaultFirstDayOfWeek
    private var ignoreLessThan = SETTINGS_DEFAULT_IGNORE_LESS_THAN

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG,"onCreate: called")
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SettingsDialogStyle)
        retainInstance = true

    }

    /**
     * we use the onCreateView to inflate the dialog rather than use the dialogBuilder
     * because we want to have access to individual widgets in the dialog.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: called")
        return inflater.inflate(R.layout.settings_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: called")
        super.onViewCreated(view, savedInstanceState)

        dialog?.setTitle(R.string.action_settings)

        okButton.setOnClickListener {
            saveValues()
            dismiss()
        }
        /**
         * This is in response to the movement of the seekBar.
         */
        ignoreSeconds.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress < 12) {
                    ignoreSecondsTitle.text = getString(
                        R.string.settingsIgnoreSecondsTitle,
                        deltas[progress],
                        resources.getQuantityString(
                            R.plurals.settingsLittleUnits,
                            deltas[progress]
                        )
                    )
                } else {
                    val minutes = deltas[progress] / 60
                    ignoreSecondsTitle.text = getString(
                        R.string.settingsIgnoreSecondsTitle,
                        minutes,
                        resources.getQuantityString(R.plurals.settingsBigUnits, minutes)
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                //Not needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                //Not needed
            }
        })
        cancelButton.setOnClickListener { dismiss() }

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewRestored: called")
        super.onViewStateRestored(savedInstanceState)
        if(savedInstanceState == null) {
            readValues()

            firstDaySpinner.setSelection(firstDay - GregorianCalendar.SUNDAY)      //spinner values are zero-based.  //note that the firstDay is in calender numbering.
            //the seconds value need to be converted back to index before setting the seekBar.
            //convert seconds into an index into the time values array.
            val seekBarValue =
                deltas.binarySearch(ignoreLessThan)    //returns the index values of the array values returned for the ignoreLessThan.
            if (seekBarValue < 0) {
                //this shouldn't happen , the programmer's made a mistake
                throw IndexOutOfBoundsException("Value $seekBarValue not found in deltas array")
            }
            ignoreSeconds.max =
                deltas.size - 1    //this is to cope with the possibilities of the having an extended arrayList.
            Log.d(TAG, "onViewStateRestored: setting slider to $seekBarValue")
            ignoreSeconds.progress = seekBarValue   //to line 153


            /**
             * Covering the plural rules in seconds, minutes, and hours as the case may be.
             */
            if (ignoreLessThan < 60) {
                ignoreSecondsTitle.text= getString(
                    R.string.settingsIgnoreSecondsTitle,
                    ignoreLessThan,
                    resources.getQuantityString(R.plurals.settingsLittleUnits, ignoreLessThan)
                )
            } else {
                val minutes = ignoreLessThan / 60
                ignoreSecondsTitle.text = getString(
                    R.string.settingsIgnoreSecondsTitle,
                    minutes,
                    resources.getQuantityString(R.plurals.settingsBigUnits, minutes)
                )
            }
        }

    }

    private fun readValues() {   //to read the values saved in the sharedPreference, i.e.gets the default if no values are saved yet.
        //opposite of saveValues.
        with(getDefaultSharedPreferences(context)) {
            firstDay = getInt(SETTINGS_FIRST_DAY_OF_WEEK, defaultFirstDayOfWeek)
            ignoreLessThan = getInt(SETTINGS_IGNORE_LESS_THAN, SETTINGS_DEFAULT_IGNORE_LESS_THAN)
        }
        Log.d(TAG, "Retrieving first day = $firstDay, ignoreLessThan = $ignoreLessThan")
    }

    private fun saveValues() {
        val newFirstDay = firstDaySpinner.selectedItemPosition + GregorianCalendar.SUNDAY
        val newIgnoreLessThan = deltas[ignoreSeconds.progress]   //save the deltas array values.

        Log.d(TAG, "Saving first day = $newFirstDay, ignore seconds = $newIgnoreLessThan")

        with(getDefaultSharedPreferences(context).edit()) {
            if (newFirstDay != firstDay) {
                putInt(SETTINGS_FIRST_DAY_OF_WEEK, newFirstDay)
            }
            if (newIgnoreLessThan != ignoreLessThan) {
                putInt(SETTINGS_IGNORE_LESS_THAN, newIgnoreLessThan)
            }
            apply()
        }
    }

    override fun onDestroy() {
        Log.d(TAG,"onDestroy: called")
        super.onDestroy()

    }
}

object TasksContract {                //a singleton.

    internal const val TABLE_NAME = "Tasks"

    /**
     * The URI to access the task table.
     */
    val CONTENT_URI: Uri = Uri.withAppendedPath(
        CONTENT_AUTHORITY_URI,
        TABLE_NAME
    )

    const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$CONTENT_AUTHORITY.$TABLE_NAME"
    const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$CONTENT_AUTHORITY.$TABLE_NAME"

    //Tasks fields
    object Columns {
        const val ID = BaseColumns._ID
        const val TASK_NAME = "Name"
        const val TASK_DESCRIPTION = "Description"
        const val TASK_SORT_ORDER = "SortOrder"
    }

    fun getId(uri: Uri): Long {
        return ContentUris.parseId(uri)
    }

    fun buildUriFromId(id: Long): Uri {  //complementary method.
        return ContentUris.withAppendedId(CONTENT_URI, id)
    }
}