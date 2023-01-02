package com.opensource.ssu_ppy;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ShowMatchedMeeting extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{
    FirebaseFirestore db;
    FirebaseFirestore meetingDb;
    FirebaseFirestore myDb;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser user = auth.getCurrentUser();
    MeetingAdapter adapter;
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;
    HashMap<String,Object> meetingItems;

    androidx.appcompat.widget.Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private View headerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_matched_meeting);


        db = FirebaseFirestore.getInstance();
        meetingDb = FirebaseFirestore.getInstance();
        myDb = FirebaseFirestore.getInstance();

        meetingItems = new HashMap<>();

        recyclerView = (RecyclerView) findViewById(R.id.show_matched_meeting_recylerView);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), new LinearLayoutManager(this).getOrientation());
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.white_line));
        recyclerView.addItemDecoration(dividerItemDecoration);
        adapter = new MeetingAdapter(getApplicationContext());
        getChatData(adapter,meetingItems);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();


        setSideNavBar();
        getHeaderInfo();
        //뷰 클릭시 매칭된 상대 프로필 확인
        adapter.setOnItemClickListener(new MeetingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MeetingAdapter.ViewHolder holder, View view, int position) {
                String roomNum = adapter.getNum(position);
                Intent intent = new Intent(getApplicationContext(),MeetChattingRoom.class);
                intent.putExtra("roomNum",roomNum);
                startActivity(intent);
            }
        });
        adapter.setOnItemLongClickListener(new MeetingAdapter.OnItemLongClickListener(){
            @Override
            public void onItemLongClick(MeetingAdapter.ViewHolder holder, View view, int position) {
                showMatchedMeetingDlg(adapter, position);
                adapter.notifyDataSetChanged();
            }
        });
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(),ShowAllOppositeSexProfile.class);
        finish();
        startActivity(intent);
        super.onBackPressed();
    }
    public void showMatchedMeetingDlg(MeetingAdapter adapter, int position) {
        final LinearLayout linear = (LinearLayout) View.inflate(ShowMatchedMeeting.this, R.layout.profile_show_dialog, null);
        AlertDialog.Builder dlg = new AlertDialog.Builder(ShowMatchedMeeting.this);
        dlg.setTitle("상대 프로필"); //제목

        FirebaseUser user = auth.getCurrentUser();

        TextView major = (TextView) linear.findViewById(R.id.majorText);
        TextView mbti = (TextView) linear.findViewById(R.id.mbtiText);
        TextView hobby = (TextView) linear.findViewById(R.id.hobbyText);



        //뷰 클릭시 정보 따와야함
        String majorText=(String)adapter.popItem("major");
        String mbtiText=(String)adapter.popItem("mbti");
        String hobbyText=(String)adapter.popItem("hobby");

        major.setText(majorText);
        mbti.setText(mbtiText);

        hobby.setText(String.valueOf(hobbyText));

        if(linear.getParent() != null) ((ViewGroup) linear.getParent()).removeView(linear); // 다이얼로그 여러번 생성시 중복된 뷰그룹 들어가 발생하는 에러처리 부분
        dlg.setView(linear);

        dlg.setPositiveButton("삭제",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //뷰 클릭시 따온 정보로 파이어스토어에서 삭제하기
                db.collection("meeting").document(adapter.getNum(position))
                        .collection("host")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if(task.isSuccessful()) {
                                    for(QueryDocumentSnapshot host : task.getResult()) {
                                        String hostId = (String) host.getId();
                                        if (hostId.equals(adapter.getOtherId(position))){
                                            meetingDb.collection("meeting").document(adapter.getNum(position))
                                                    .collection("guest").document(user.getUid())
                                                    .delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d(TAG, "DocumentSnapshot successfully deleted!");
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.w(TAG, "Error deleting document", e);
                                                        }
                                                    });
                                        }else{
                                            meetingDb.collection("meeting").document(adapter.getNum(position))
                                                    .collection("guest").document(adapter.getOtherId(position))
                                                    .delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d(TAG, "DocumentSnapshot successfully deleted!");
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.w(TAG, "Error deleting document", e);
                                                        }
                                                    });
                                        }

                                    }
                                }
                            }
                        });
            }
        });
        dlg.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        dlg.show();
    }
    public void getChatData(MeetingAdapter adapter, HashMap<String,Object> meetingItems) {
        FirebaseUser user = auth.getCurrentUser();

        db.collection("meeting")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            for(QueryDocumentSnapshot document : task.getResult()) {
                                meetingDb.collection("meeting").document(document.getId())
                                        .collection("host")
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if(task.isSuccessful()) {
                                                    for(QueryDocumentSnapshot host : task.getResult()) {
                                                        String hostId = (String) host.getId();
                                                        if (hostId.equals(user.getUid())){
                                                            meetingDb.collection("meeting").document(document.getId())
                                                                    .collection("guest")
                                                                    .get()
                                                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                            if(task.isSuccessful()) {
                                                                                for(QueryDocumentSnapshot guest : task.getResult()) {
                                                                                    String guestId = (String) guest.getId();
                                                                                    Boolean match = (boolean) guest.getData().get("matched");
                                                                                    if(match){
                                                                                        String item = (String) guest.getData().get("meetingName");
                                                                                        //리사이클러뷰에 프로필 정보 저장하는 HashMap
                                                                                        HashMap<String, Object> meetingObject = new HashMap<>();
                                                                                        meetingObject.put("meetingName",guest.getData().get("meetingName"));
                                                                                        meetingObject.put("hobby",guest.getData().get("hobby"));
                                                                                        meetingObject.put("major",guest.getData().get("major"));
                                                                                        meetingObject.put("mbti", guest.getData().get("mbti"));
                                                                                        meetingObject.put("peopleCount",guest.getData().get("peopleCount"));
                                                                                        String meetingName = (String) document.getData().get("meetingName");
                                                                                        String meetingNum = (String) document.getId();
                                                                                        String hostRoomId = (String) guest.getId();
                                                                                        meetingItems.putAll(meetingObject);
                                                                                        adapter.addItem(meetingName);
                                                                                        adapter.putItem(meetingItems);
                                                                                        adapter.addNum(meetingNum);
                                                                                        adapter.addOtherId(hostRoomId);
                                                                                    }
                                                                                }adapter.notifyDataSetChanged();
                                                                            }
                                                                        }
                                                                    });
                                                        } else if(!hostId.equals(user.getUid())){
                                                            meetingDb.collection("meeting").document(document.getId())
                                                                    .collection("guest")
                                                                    .get()
                                                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                            if(task.isSuccessful()) {
                                                                                for(QueryDocumentSnapshot guest : task.getResult()) {
                                                                                    String guestId = (String) guest.getId();
                                                                                    Boolean match = (boolean) guest.getData().get("matched");
                                                                                    if(guestId.equals(user.getUid())&&match){
                                                                                        String item = (String) guest.getData().get("meetingName");
                                                                                        //리사이클러뷰에 프로필 정보 저장하는 HashMap
                                                                                        HashMap<String, Object> meetingObject = new HashMap<>();
                                                                                        meetingObject.put("meetingName",guest.getData().get("meetingName"));
                                                                                        meetingObject.put("hobby",guest.getData().get("hobby"));
                                                                                        meetingObject.put("major",guest.getData().get("major"));
                                                                                        meetingObject.put("mbti", guest.getData().get("mbti"));
                                                                                        meetingObject.put("peopleCount",guest.getData().get("peopleCount"));
                                                                                        String meetingName = (String) document.getData().get("meetingName");
                                                                                        String meetingNum = (String) document.getId();
                                                                                        String hostRoomId = (String) guest.getId();
                                                                                        meetingItems.putAll(meetingObject);
                                                                                        adapter.addItem(meetingName);
                                                                                        adapter.putItem(meetingItems);
                                                                                        adapter.addNum(meetingNum);
                                                                                        adapter.addOtherId(hostRoomId);
                                                                                    }
                                                                                }adapter.notifyDataSetChanged();
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                }
                                            }
                                        });
                            }

                        }
                    }
                });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //추가된 소스, ToolBar에 menu.xml을 인플레이트함
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.basebar, menu);
        return true;
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.back) {
            onBackPressed();
            return true;
        }
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item1:
                Toast.makeText(getApplicationContext(),"로그아웃 하였습니다",Toast.LENGTH_SHORT).show();
                signOut();
                break;
            case R.id.menu_item2:
                Intent intent = new Intent(getApplicationContext(),ReviseProfile.class);
                startActivity(intent);
                break;
            case R.id.menu_item3:
                show_register_student_dlg();
                break;
        }
        return false;
    }

    public void setSideNavBar() {
        toolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView toolbarText = (TextView) findViewById(R.id.toolbar_title);
        toolbarText.setText("매칭된 목록");
        toolbar.setBackgroundColor(Color.parseColor("#E6CCF1"));

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_round_dehaze_24);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_menu_layout);
        navigationView = (NavigationView) findViewById(R.id.navigationView);
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
        );
        drawerLayout.addDrawerListener(drawerToggle);
        navigationView.setNavigationItemSelectedListener(this);
    }

    public void setHeaderView(UserModel temp) {
        headerView = navigationView.getHeaderView(0);
        View headerBody = headerView.findViewById(R.id.header_body);
        TextView headerName = (TextView) headerView.findViewById(R.id.header_name);
        TextView headerEmail = (TextView) headerView.findViewById(R.id.header_email);
        headerName.setText(temp.getName());
        headerEmail.setText(user.getEmail());
        if(temp.isIs_man()) headerBody.setBackgroundColor(Color.parseColor("#BFD0FB"));
        else headerBody.setBackgroundColor(Color.parseColor("#E6CCF1"));
    }

    public void getHeaderInfo() {
        System.out.println(user.getUid());
        db.collection("user").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        UserModel temp = document.toObject(UserModel.class);
                        setHeaderView(temp);
                    }
                }
            }
        });
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        finish();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void show_register_student_dlg() {
        AlertDialog.Builder deleteMemberDlg = new AlertDialog.Builder(this);
        deleteMemberDlg.setTitle("정말 탈퇴 하시겠습니까?");
        deleteMemberDlg.setIcon(R.drawable.ic_round_dehaze_24);
        deleteMemberDlg.setNegativeButton("네", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteMember();
            }
        });
        deleteMemberDlg.setPositiveButton("아니요", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        deleteMemberDlg.show();
    }

    private void deleteMember() {
        Toast.makeText(getApplicationContext(),"탈퇴 하였습니다",Toast.LENGTH_SHORT).show();
        auth = FirebaseAuth.getInstance();
        db.collection("user").document(user.getUid())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
        auth.getCurrentUser().delete();

        finish();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }
}
