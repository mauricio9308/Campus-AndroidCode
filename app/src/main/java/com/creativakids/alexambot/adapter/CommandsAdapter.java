package com.creativakids.alexambot.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.creativakids.alexambot.R;

import java.util.ArrayList;

/**
 * Adapter for the list of the possible commands of the view
 *
 * Created by Mauricio Lara on 7/3/17.
 */
public class CommandsAdapter extends RecyclerView.Adapter<CommandsAdapter.CommandViewHolder>{


    // Reference for the different commands
    private ArrayList<String> commands;

    // Reference of the calling context
    private LayoutInflater layoutInflater;

    /**
     * Base constructor for the commands adapter
     * */
    public CommandsAdapter(@NonNull Context context, @NonNull ArrayList<String> commands ){
        // Setting the reference for the calling context
        layoutInflater = LayoutInflater.from( context );

        // Setting the reference for the commands
        this.commands = commands;
    }

    @Override
    public CommandViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // We create the reference for the view holder
        return new CommandViewHolder( layoutInflater.inflate( R.layout.item_command, parent, false /* attachToRoot */) );
    }

    @Override
    public void onBindViewHolder(CommandViewHolder holder, int position) {
        // Setting the reference for the command name
        holder.getTxtCommandName().setText( commands.get( position ) );
    }

    @Override
    public int getItemCount() {
        return commands == null ? 0 : commands.size();
    }

    /**
     * Representation for the possible commands to the mBot view
     * */
    class CommandViewHolder extends RecyclerView.ViewHolder{

        // Reference for the view
        private TextView txtCommandName;

        CommandViewHolder(View itemView) {
            super(itemView);

            // Getting the reference for the text view
            txtCommandName = (TextView) itemView.findViewById( R.id.txt_command_name );
        }

        TextView getTxtCommandName() {
            return txtCommandName;
        }

    }
}
