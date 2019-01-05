package com.example.david.gigfinder;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.david.gigfinder.adapters.MessageListAdapter;
import com.example.david.gigfinder.data.Artist;
import com.example.david.gigfinder.data.Message;
import com.example.david.gigfinder.data.User;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private ArrayList<Message> messageList = new ArrayList<Message>();
    private LinearLayout nameboxLayout;
    private ImageView backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        User testUser1 = new User();
        testUser1.setName("Friend");
        testUser1.setId(2);

        User testUser2 = new User();
        testUser2.setId(1);

        messageList.add(new Message("Hi how are you? This is just a test Message", testUser2, 1355270400000L));
        messageList.add(new Message("Reply Test Message",testUser1, 1355270400000L));

        nameboxLayout = (LinearLayout) findViewById(R.id.layout_namebox);
        nameboxLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatActivity.this, ArtistProfileActivity.class);
                startActivity(intent);
            }
        });

        backBtn = (ImageView) findViewById(R.id.backImg);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(this, messageList);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecycler.setAdapter(mMessageAdapter);

        mMessageAdapter.notifyDataSetChanged();
    }
}
