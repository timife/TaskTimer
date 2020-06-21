package timifeoluwa.example.tasktimer.fragments

import android.database.Cursor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import timifeoluwa.example.tasktimer.R
import timifeoluwa.example.tasktimer.TasksContract
import timifeoluwa.example.tasktimer.database.Task
import java.lang.IllegalStateException


class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var tliName: TextView = view.findViewById(R.id.tli_name)
    var tliDescription: TextView = view.findViewById(R.id.tli_description)
    var tliEdit: ImageButton = view.findViewById(R.id.tli_edit)
    var tliDelete: ImageButton = view.findViewById(R.id.tli_delete)

}

private const val TAG = "CursorRecyclerViewAdapt"

class CursorRecyclerViewAdapter(
    private var cursor: Cursor?, private val listener: OnTaskClickListener
) : RecyclerView.Adapter<TaskViewHolder>() {

    interface OnTaskClickListener {
        fun onEditClick(task: Task)
        fun onDeleteClick(task: Task)
        fun onTaskLongClick(task: Task)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        Log.d(TAG, "onCreateViewHolder: new view requested")
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.task_list_items, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: starts")

        val cursor = cursor
        if (cursor == null || cursor.count == 0) {
            Log.d(TAG, "onBindViewHolder: providing instructions")
            holder.tliName.setText(R.string.instructions_heading)
            holder.tliDescription.setText(R.string.instructions)
            holder.tliEdit.visibility = View.GONE
            holder.tliDelete.visibility = View.GONE
        } else {
            if (!cursor.moveToPosition(position)) {               //check if we can move to another position.
                throw IllegalStateException("Couldn't move cursor")
            }
            //create a Task object from the data in the cursor
            val task = Task(
                //retrieving the data in the cursor and use it to set the texts in the textView widgets.
                cursor.getString(cursor.getColumnIndex(TasksContract.Columns.TASK_NAME)),
                cursor.getString(cursor.getColumnIndex(TasksContract.Columns.TASK_DESCRIPTION)),
                cursor.getInt(cursor.getColumnIndex(TasksContract.Columns.TASK_SORT_ORDER))
            )

            //Remember that the id isn't set in the constructor.
            task.id = cursor.getLong(cursor.getColumnIndex(TasksContract.Columns.ID))

            holder.tliName.text = task.name
            holder.tliDescription.text = task.description
            holder.tliEdit.visibility = View.VISIBLE
            holder.tliDelete.visibility = View.VISIBLE

            holder.tliEdit.setOnClickListener {
                listener.onEditClick(task)
            }
            holder.tliDelete.setOnClickListener {
                listener.onDeleteClick(task)
            }
            holder.tliName.setOnLongClickListener {
                listener.onTaskLongClick(task)
                true
            }
        }

    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: starts")
        val cursor = cursor
        val count = if (cursor == null || cursor.count == 0) {
            1
        } else {
            cursor.count
        }
        Log.d(TAG, "returning $count")
        return count
    }

    /**
     * Swap in a new Cursor, returning the old Cursor,
     * The returned old cursor is *not* closed.
     *
     * @param newCursor The new cursor to be used
     * @return Returns the previously set Cursor, or null if there wasn't
     * one.
     * If the given new Cursor is the same instance as the previously set
     * Cursor , null is also returned.
     *
     * What this does is to notify any observer such as the recyclerView that the
     * data from this adapter has changed
     */
    fun swapCursor(newCursor: Cursor?): Cursor? {
        if (newCursor == cursor) {
            return null
        }
        val numItems = itemCount   //save itemCount
        val oldCursor = cursor  //save oldCursor
        cursor = newCursor              //create newCursor
        if (newCursor != null) {
            //notify the observer about the new cursor
            notifyDataSetChanged()
        } else {
            //notify the observers about a lack of data set
            notifyItemRangeRemoved(0, numItems)
        }
        return oldCursor

    }

}