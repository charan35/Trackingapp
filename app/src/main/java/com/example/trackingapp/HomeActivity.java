package com.example.trackingapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackingapp.Interface.IFirebaseLoadDone;
import com.example.trackingapp.Interface.IRecyclerItemClickListener;
import com.example.trackingapp.Model.User;
import com.example.trackingapp.Service.MyLocationReceiver;
import com.example.trackingapp.Utils.Common;
import com.example.trackingapp.ViewHolder.UserViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.List;


public class HomeActivity extends AppCompatActivity implements IFirebaseLoadDone {

    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle actionBarDrawerToggle;
    NavigationView navigationView;

    TextView name, email, empid;
    ImageView navprofile;
    Toolbar toolbar;

    /////////////

    FirebaseRecyclerAdapter<User, UserViewHolder> adapter,searchAdapter;
    RecyclerView recycler_friend_list;
    IFirebaseLoadDone firebaseLoadDone;

    MaterialSearchBar searchBar;
    List<String> suggestList = new ArrayList<>();

    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,toolbar, R.string.open, R.string.close);
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        View navView = navigationView.inflateHeaderView(R.layout.header);

        name = (TextView) navView.findViewById(R.id.name);
        email = (TextView) navView.findViewById(R.id.email);
        empid = (TextView) navView.findViewById(R.id.empid);
        navprofile = (ImageView) navView.findViewById(R.id.navprofile);



        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                int id = menuItem.getItemId();
                switch (id) {
                    case R.id.nav_find_people:
                        Intent intent=new Intent(HomeActivity.this,AllPeopleActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.nav_add_people:
                        Intent intent1=new Intent(HomeActivity.this,FriendRequestActivity.class);
                        startActivity(intent1);
                        break;
                    case R.id.nav_sign_out:

                        break;
                }
                return false;
            }
        });



        searchBar = findViewById(R.id.material_search_bar);
        searchBar.setCardViewElevation(10);
        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> suggest = new ArrayList<>();
                for(String search: suggestList)
                {
                    if(search.toLowerCase().contains(searchBar.getText().toLowerCase()))
                        suggest.add(search);
                }
                searchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        searchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                if(!enabled)
                {
                    if(adapter != null)
                    {
                        recycler_friend_list.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                startSearch(text.toString());
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

        recycler_friend_list = findViewById(R.id.recycler_all_people);
        recycler_friend_list.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recycler_friend_list.setLayoutManager(layoutManager);
        recycler_friend_list.addItemDecoration(new DividerItemDecoration(this,((LinearLayoutManager) layoutManager).getOrientation()));


        updateLocation();

        firebaseLoadDone = this;

        loadFriendList();
        loadSearchData();


    }

    private void loadSearchData() {

    }

    private void loadFriendList() {
        Query query = FirebaseDatabase.getInstance()
                .getReference(Common.USER_INFORMATION)
                .child(Common.loggedUser.getUid())
                .child(Common.ACCEPT_LIST);

        FirebaseRecyclerOptions<User> options = new FirebaseRecyclerOptions.Builder<User>()
                .setQuery(query,User.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<User, UserViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull final User model) {
                holder.txt_user_email.setText(new StringBuilder(model.getEmail()));

                holder.setiRecyclerItemClickListener(new IRecyclerItemClickListener() {
                    @Override
                    public void onItemClickListener(View view, int position) {
                        Common.trackingUser = model;
                        startActivity(new Intent(HomeActivity.this,TrackingActivity.class));

                    }
                });
            }

            @NonNull
            @Override
            public UserViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
                View itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.layout_user,viewGroup,false);
                return new UserViewHolder(itemView);
            }
        };
        adapter.startListening();;
        recycler_friend_list.setAdapter(adapter);
    }

    @Override
    protected void onStop() {
        if(adapter != null)
            adapter.stopListening();
        if(searchAdapter != null)
            searchAdapter.stopListening();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(adapter != null)
            adapter.startListening();
        if(searchAdapter != null)
            searchAdapter.startListening();
    }

    private void updateLocation() {
        buildLocationRequest();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(HomeActivity.this, MyLocationReceiver.class);
        intent.setAction(MyLocationReceiver.ACTION);
        return PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setFastestInterval(3000);
        locationRequest.setInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startSearch(String search_value) {
        List<String> lstUserEmail = new ArrayList<>();
        Query query = FirebaseDatabase.getInstance()
                .getReference(Common.USER_INFORMATION)
                .child(Common.loggedUser.getUid())
                .child(Common.ACCEPT_LIST)
                .orderByChild("name")
                .startAt(search_value);

        FirebaseRecyclerOptions<User> options = new FirebaseRecyclerOptions.Builder<User>()
                .setQuery(query,User.class)
                .build();

        searchAdapter = new FirebaseRecyclerAdapter<User, UserViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull final User model) {
                holder.txt_user_email.setText(new StringBuilder(model.getEmail()));

                holder.setiRecyclerItemClickListener(new IRecyclerItemClickListener() {
                    @Override
                    public void onItemClickListener(View view, int position) {
                        Common.trackingUser = model;
                        startActivity(new Intent(HomeActivity.this,TrackingActivity.class));
                    }
                });
            }

            @NonNull
            @Override
            public UserViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
                View itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.layout_user,viewGroup,false);
                return new UserViewHolder(itemView);
            }
        };
        searchAdapter.startListening();;
        recycler_friend_list.setAdapter(searchAdapter);
    }

    @Override
    public void onFirebaseLoadUserNameDone(List<String> lstEmail) {
        searchBar.setLastSuggestions(lstEmail);
    }

    @Override
    public void onFirebaseLoadFailed(String message) {

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

    }
}