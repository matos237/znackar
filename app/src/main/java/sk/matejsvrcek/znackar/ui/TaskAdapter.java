package sk.matejsvrcek.znackar.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.model.data.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface MultiSelectListener {
        void onMultiSelectModeStarted();
    }

    private List<Task> tasks = new ArrayList<>();
    private final OnTaskClickListener listener;
    private final MultiSelectListener multiSelectListener;
    private final Set<Integer> selectedTaskIds = new HashSet<>();
    private boolean multiSelectMode = false;

    public TaskAdapter(OnTaskClickListener listener, MultiSelectListener multiSelectListener) {
        this.listener = listener;
        this.multiSelectListener = multiSelectListener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    public void setMultiSelectMode(boolean enabled) {
        multiSelectMode = enabled;
        if (!enabled) {
            selectedTaskIds.clear();
        }
        notifyDataSetChanged();
    }

    public Set<Integer> getSelectedTaskIds() {
        return selectedTaskIds;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        final Task task = tasks.get(position);
        holder.taskNameTextView.setText(task.name);
        holder.taskDescriptionTextView.setText(task.description);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String formatted = sdf.format(new Date(task.dateTime));
        holder.taskDateTextView.setText(formatted);


        // Task type icon
        if ("track".equals(task.type)) {
            holder.typeIcon.setImageResource(R.drawable.ic_map_black);
        } else if ("photo".equals(task.type)) {
            holder.typeIcon.setImageResource(R.drawable.ic_camera_alt_black);
        } else {
            holder.typeIcon.setImageDrawable(null);
        }

        // Export status icon
        if (task.isExported) {
            holder.exportStatusIcon.setImageResource(R.drawable.ic_check_green);
        } else {
            holder.exportStatusIcon.setImageResource(R.drawable.ic_cross_red);
        }

        // Multi-select checkbox visibility
        if (multiSelectMode) {
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(selectedTaskIds.contains(task.taskId));
        } else {
            holder.checkbox.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (multiSelectMode) {
                if (selectedTaskIds.contains(task.taskId)) {
                    selectedTaskIds.remove(task.taskId);
                } else {
                    selectedTaskIds.add(task.taskId);
                }
                notifyItemChanged(position);
            } else {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!multiSelectMode) {
                multiSelectMode = true;
                selectedTaskIds.add(task.taskId);
                notifyDataSetChanged();
                if (multiSelectListener != null) {
                    multiSelectListener.onMultiSelectModeStarted();
                }
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }


    // ViewHolder for the task items
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskNameTextView;
        TextView taskDescriptionTextView;
        TextView taskDateTextView;
        CheckBox checkbox;
        ImageView typeIcon;
        ImageView exportStatusIcon;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskNameTextView = itemView.findViewById(R.id.text_task_name);
            taskDescriptionTextView = itemView.findViewById(R.id.text_task_description);
            taskDateTextView = itemView.findViewById(R.id.text_task_date);
            checkbox = itemView.findViewById(R.id.checkbox_select);
            typeIcon = itemView.findViewById(R.id.image_type_icon);
            exportStatusIcon = itemView.findViewById(R.id.image_export_status);
        }
    }
}
