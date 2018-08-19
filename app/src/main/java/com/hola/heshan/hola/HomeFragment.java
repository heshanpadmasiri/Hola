package com.hola.heshan.hola;


import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.multidots.fingerprintauth.FingerPrintAuthCallback;
import com.multidots.fingerprintauth.FingerPrintAuthHelper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment implements FingerPrintAuthCallback{

    private Button authenticateButton;
    private Button permissionButton;
    private Button attendanceButton;
    private TextView titleTextView;
    private TextView bodyTextView;
    private SpinKitView spinKitView;
    private FingerPrintAuthHelper fingerPrintAuthHelper;
    private MaterialDialog fingerPrintAuthPrompt;
    private volatile int fingerPrintAuthPromptTask;
    private FirebaseFunctions firebaseFunctions;

    private volatile String companyId;

    private FirebaseServices firebaseServices;

    private final static String USER_ID = "test_user_1";
    private final static String DOOR_ID = "1";

    private final static int VALIDATE_USER = 0;
    private final static int REQUEST_ACCESS_PERMISSION = 1;
    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_home, container, false);
        firebaseFunctions = FirebaseFunctions.getInstance();
        fingerPrintAuthHelper = FingerPrintAuthHelper.getHelper(view.getContext(),this);

        authenticateButton = view.findViewById(R.id.btn_auth);
        permissionButton = view.findViewById(R.id.btn_permission);
        attendanceButton = view.findViewById(R.id.btn_attendance);
        titleTextView = view.findViewById(R.id.txt_title);
        bodyTextView = view.findViewById(R.id.txt_body);
        spinKitView = view.findViewById(R.id.spin_kit);
        spinKitView.setVisibility(View.INVISIBLE);
        firebaseServices = FirebaseServices.getInstance();
        authenticateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fingerPrintAuthPrompt = new MaterialDialog.Builder(v.getContext())
                        .title("Title")
                        .content("Content")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                Toast.makeText(getContext(),"positive",Toast.LENGTH_LONG).show();

                            }
                        })
                        .build();
                fingerPrintAuthPrompt.show();
                fingerPrintAuthPromptTask = VALIDATE_USER;
                fingerPrintAuthHelper.startAuth();
            }
        });

        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                spinKitView.setVisibility(View.VISIBLE);
                Task<DocumentSnapshot> doorDataTask = firebaseServices.getDoorData(DOOR_ID);
                doorDataTask.addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            final String reqPermission = (String) task.getResult().get("permission_level");
                            companyId = (String) task.getResult().get("company_id");
                            Task<DocumentSnapshot> userDataTask = firebaseServices.getUserData(USER_ID);
                            userDataTask.addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if(task.isSuccessful()){
                                        String userPermission = (String) task.getResult().get("permission_level");
                                        String userCompany = (String) task.getResult().get("company_id");
                                        spinKitView.setVisibility(View.INVISIBLE);
                                        if(reqPermission.equals(userPermission)){
                                            // todo : open the door
                                        } else if (!userCompany.equals(companyId)){

                                            updateText("No permission", "Request permission confirm by fingerPrint");
                                            fingerPrintAuthPromptTask = REQUEST_ACCESS_PERMISSION;
                                            fingerPrintAuthHelper.startAuth();
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });

        attendanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseServices.recordAttendance(USER_ID,"COMPANY1");
            }
        });
        return view;
    }

    private void updateText(String title,String body){
        titleTextView.setText(title);
        bodyTextView.setText(body);
    }

    @Override
    public void onNoFingerPrintHardwareFound() {

    }

    @Override
    public void onNoFingerPrintRegistered() {

    }

    @Override
    public void onBelowMarshmallow() {

    }

    @Override
    public void onAuthSuccess(FingerprintManager.CryptoObject cryptoObject) {
        Toast.makeText(getActivity(),"AuthSuccess", Toast.LENGTH_LONG).show();
        if(fingerPrintAuthPrompt != null){
            fingerPrintAuthPrompt.dismiss();
        }
        switch (fingerPrintAuthPromptTask){
            case REQUEST_ACCESS_PERMISSION:
                firebaseServices.requestPermission(USER_ID,companyId);
                updateText("","");
                break;
        }
    }

    @Override
    public void onAuthFailed(int errorCode, String errorMessage) {

        Toast.makeText(getActivity(),"AuthFailed", Toast.LENGTH_LONG).show();
    }
}
