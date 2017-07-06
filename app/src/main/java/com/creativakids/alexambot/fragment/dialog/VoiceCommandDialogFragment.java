package com.creativakids.alexambot.fragment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;
import com.amazonaws.regions.Regions;
import com.creativakids.alexambot.R;
import com.creativakids.alexambot.activity.ControlActivity;

import java.util.Map;

/**
 * Dialog for the handling of the voice commands update
 *
 * Created by Mauricio Lara on 7/5/17.
 */
public class VoiceCommandDialogFragment extends AppCompatDialogFragment implements InteractiveVoiceView.InteractiveVoiceListener{

    // Logging tag
    private static final String LOG_TAG = VoiceCommandDialogFragment.class.getSimpleName();

    // Reference for the dialog views
    private InteractiveVoiceView voiceView;

    // Reference for the calling context
    private CommandCallback commandCallback;

    /**
     * Static constructor for the voice dialog fragment
     * */
    public static VoiceCommandDialogFragment newInstance(){
        return new VoiceCommandDialogFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Handling the calling callback
        try{
            commandCallback = (CommandCallback) context;
        }catch ( ClassCastException e ){
            throw new ClassCastException("Calling context: " + context.getClass().getSimpleName()
                    + " must implement " + CommandCallback.class.getSimpleName() );
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog( getContext(), R.style.NoTitleDialog );
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable( true );
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Creating the dialog view
        View baseView = inflater.inflate( R.layout.dialog_commands_view, container, false /* attachToRoot */);

        // Getting the reference for the interaction view
        voiceView = (InteractiveVoiceView) baseView.findViewById(R.id.interactions_view);

        return baseView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Init of the interactions view
        voiceView.setInteractiveVoiceListener( VoiceCommandDialogFragment.this /* InteractiveVoiceListener */);

        // Creating the credentials provider...
        CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(
                "us-east-1:0b419719-fde7-4799-a95a-8429bf714229",
                Regions.US_EAST_1);

        // Setting up the voice view with the interactions
        voiceView.getViewAdapter().setCredentialProvider(credentialsProvider);
        // Creating the interaction config for the bot
        voiceView.getViewAdapter().setInteractionConfig(
                new InteractionConfig(getString(R.string.bot_name),getString(R.string.bot_alias)));
        voiceView.getViewAdapter().setAwsRegion(Regions.US_EAST_1.getName());
    }


    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Removing the callback for avoid memory leaks
        commandCallback = null;
    }

    @Override
    public void dialogReadyForFulfillment(Map<String, String> slots, String intent) {
        Log.d(LOG_TAG, "Ready for fulfillment: " + intent );
    }

    @Override
    public void onResponse(Response response) {
        Log.d(LOG_TAG, "Response: " + response.getContentType() + " message " + response.getTextResponse() + " intent-name " + response.getIntentName() );

        int newCommand = -1;
        try{
            newCommand = Integer.parseInt( response.getTextResponse() );
        }catch ( Exception e ){
            // We handle the error case
            e.printStackTrace();
            newCommand = ControlActivity.COMMAND_STOP;
        }

        if( commandCallback != null ){
            commandCallback.onCommandViaAlexa( newCommand );
        }
    }

    @Override
    public void onError(String responseText, Exception e) {
        Log.d(LOG_TAG, "Error Response: " + responseText );

        if( commandCallback != null ){
            commandCallback.onCommandViaAlexa(ControlActivity.COMMAND_STOP );
        }
    }

    /**
     * Interface for the processing of the lex commands
     * */
    public interface CommandCallback{
        void onCommandViaAlexa( int lexCommand );
    }
}
