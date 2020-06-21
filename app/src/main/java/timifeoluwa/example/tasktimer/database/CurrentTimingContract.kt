package timifeoluwa.example.tasktimer.database

import android.net.Uri
import timifeoluwa.example.tasktimer.TasksContract

object CurrentTimingContract{
    internal const val TABLE_NAME = "vwCurrentTiming"

    /**
     * The URI to access the timings table.
     */
    val CONTENT_URI: Uri = Uri.withAppendedPath(CONTENT_AUTHORITY_URI,
        TABLE_NAME
    )

    const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$CONTENT_AUTHORITY.$TABLE_NAME"
    const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$CONTENT_AUTHORITY.$TABLE_NAME"


    //Timings fields
    object Columns {
        const val TIMING_ID = TimingsContract.Columns.ID
        const val TASK_ID = TimingsContract.Columns.TIMING_TASK_ID
        const val START_TIME =
            TimingsContract.Columns.TIMING_START_TIME
        const val TASK_NAME  = TasksContract.Columns.TASK_NAME
    }
    /**
     * No unique id to get the rows , so no need to include functions to getIds.
     */
}