package timifeoluwa.example.tasktimer.fragments

import android.app.Application
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timifeoluwa.example.tasktimer.database.Task
import timifeoluwa.example.tasktimer.TasksContract
import timifeoluwa.example.tasktimer.Timing
import timifeoluwa.example.tasktimer.database.CurrentTimingContract
import timifeoluwa.example.tasktimer.database.TimingsContract

private const val TAG = "TaskTimerViewModel"

class TaskTimerViewModel(application: Application) : AndroidViewModel(application) {

    private val contentObserver = object :
        ContentObserver(Handler()) {   //when a change is notified , the onChange function is called.Then the loadTask is called to reload the data and update the cursor live data.
        override fun onChange(
            selfChange: Boolean,
            uri: Uri?
        ) {     //the URI is passed into this onChange function.
            Log.d(TAG, "contentObserver.onChange: called. uri is $uri")
            loadTasks()
        }
    }

    private var currentTiming: Timing? = null

    private val databaseCursor = MutableLiveData<Cursor>()
    val cursor: LiveData<Cursor>
        get() = databaseCursor

    private val taskTiming = MutableLiveData<String>()
    val timing: LiveData<String>
        get() = taskTiming


    init {
        Log.d(TAG, "TaskTimerViewModel: created")
        getApplication<Application>().contentResolver.registerContentObserver(  //register the contentObserver.telling the URI that i want to observe changes in the content Provider
            TasksContract.CONTENT_URI,
            true,
            contentObserver
        )
        currentTiming =
            retrieveTiming()  //all these in main thread coz i want the UI to block there to make sure everything is loaded before user interaction.
        loadTasks()
    }


    private fun loadTasks() {   //this is to load data from the database, and then updates the mainActivityFragment livedata.
        val projection = arrayOf(
            TasksContract.Columns.ID,
            TasksContract.Columns.TASK_NAME,
            TasksContract.Columns.TASK_DESCRIPTION,
            TasksContract.Columns.TASK_SORT_ORDER
        )
//        <order by> Tasks.SortOrder , Tasks.Name
        val sortOrder =
            "${TasksContract.Columns.TASK_SORT_ORDER}, ${TasksContract.Columns.TASK_NAME}"
        GlobalScope.launch {
            val cursor = getApplication<Application>().contentResolver.query(
                TasksContract.CONTENT_URI,
                projection, null, null, sortOrder
            )
            databaseCursor.postValue(cursor)   //this is to assign the cursor value to the databaseCursor.created on a different thread to the one the mutableData was created on.
        }  //cursor should be closed. Value set from the background thread.
    }

    fun saveTask(task: Task): Task {
        val values = ContentValues()
        if (task.name.isNotEmpty()) {
            //Don't save a task with no name
            values.put(TasksContract.Columns.TASK_NAME, task.name)
            values.put(TasksContract.Columns.TASK_DESCRIPTION, task.description)
            values.put(
                TasksContract.Columns.TASK_SORT_ORDER,
                task.sortOrder
            ) //defaults to zero if empty


            if (task.id == 0L) {   //the task has no id, so we're inserting. meaning there's no similar id in the database , so it's adding to the database a new task.
                GlobalScope.launch {
                    Log.d(TAG, "saveTask: adding new Task")
                    val uri = getApplication<Application>().contentResolver?.insert(
                        TasksContract.CONTENT_URI,
                        values
                    )
                    if (uri != null) {
                        task.id = TasksContract.getId(uri)
                        Log.d(TAG, "saveTask: new id is ${task.id}")
                    }
                }
            } else {
                //task has an id, so we're updating.meaning there's an identical id in the database which it returns and rather updates.
                GlobalScope.launch {
                    Log.d(TAG, "saveTask: updating task")
                    getApplication<Application>().contentResolver?.update(
                        TasksContract.buildUriFromId(
                            task.id
                        ), values, null, null
                    )
                }
            }

        }
        return task
    }

    fun deleteTask(taskId: Long) {
        Log.d(TAG, "Deleting task")
        GlobalScope.launch {
            getApplication<Application>().contentResolver.delete(
                TasksContract.buildUriFromId(taskId),
                null,
                null
            )
        }
    }

    fun timeTask(task: Task) {
        Log.d(TAG, "timeTask: called")
        //Use the local variable, to allow smart casts
        val timingRecord = currentTiming

        if (timingRecord == null) {
            //no task being timed, start timing the new task
            currentTiming = Timing(task.id)
            saveTiming(currentTiming!!)
        } else {
            //We have a task being timed, so update its duration by saving it
            timingRecord.setDuration()
            saveTiming(timingRecord)
            if (task.id == timingRecord.taskId) {
                //the current task was tapped a second time, stop timing
                currentTiming = null
            } else {
                //a new task is being timed
                currentTiming = Timing(task.id)
                saveTiming(currentTiming!!)
            }
        }
        //Update the livedata
        taskTiming.value = if (currentTiming != null) task.name else null

    }


    private fun saveTiming(currentTiming: Timing) {
        Log.d(TAG, "saveTiming: called")

        //Are we updating or inserting a new row?
        val inserting = (currentTiming.duration == 0L)

        val values = ContentValues().apply {
            //since it is only the duration that changes.
            if (inserting) {
                put(TimingsContract.Columns.TIMING_TASK_ID, currentTiming.taskId)
                put(TimingsContract.Columns.TIMING_START_TIME, currentTiming.startTime)
            }
            put(TimingsContract.Columns.TIMING_DURATION, currentTiming.duration)

        }
        GlobalScope.launch {
            if (inserting) {
                val uri = getApplication<Application>().contentResolver.insert(
                    TimingsContract.CONTENT_URI,
                    values
                )
                if (uri != null) {
                    currentTiming.id =
                        TimingsContract.getId(uri)  //store the new id in the timings id in order to be able to update it when needed.
                }
            } else {
                getApplication<Application>().contentResolver.update(
                    TimingsContract.buildUriFromId(
                        currentTiming.id
                    ), values, null, null
                )
            }
        }

    }

    private fun retrieveTiming(): Timing? {
        Log.d(TAG, "retrieveTiming: starts")
        val timing: Timing?

        val timingCursor: Cursor? = getApplication<Application>().contentResolver.query(
            CurrentTimingContract.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        if (timingCursor != null && timingCursor.moveToFirst()) {
            //retrieving current timing.
            //We have an un-timed record
            val id =
                timingCursor.getLong(timingCursor.getColumnIndex(CurrentTimingContract.Columns.TIMING_ID))
            val taskId =
                timingCursor.getLong(timingCursor.getColumnIndex(CurrentTimingContract.Columns.TASK_ID))
            val startTime =
                timingCursor.getLong(timingCursor.getColumnIndex(CurrentTimingContract.Columns.START_TIME))
            val name =
                timingCursor.getString(timingCursor.getColumnIndex(CurrentTimingContract.Columns.TASK_NAME))
            timing = Timing(taskId, startTime, id)

            //Update the LiveData
            taskTiming.value = name
        } else {
            //No timing record found with zero duration
            timing = null
        }
        timingCursor?.close()
        Log.d(TAG, "retrieveTiming: returning")
        return timing

    }


    override fun onCleared() {
        Log.d(TAG, "onCleared: called")
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
    }
}
