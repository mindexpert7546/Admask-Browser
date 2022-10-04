package com.actionadblocker.kundan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;

import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.actionadblocker.kundan.Adapters.DownloadsAdapter;
import com.actionadblocker.kundan.Configs.SettingsManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DownloadsActivity extends AppCompatActivity {

    private RecyclerView downloadsList;
    private DownloadsAdapter adapter;
    AdView adViewDetailAct;
    Context context;
    com.facebook.ads.AdView fbBannerContainerDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        context=this;
        downloadsList = findViewById(R.id.downloadsCompletedList);
        File videoFile = new File(SettingsManager.DOWNLOAD_FOLDER);
        if(videoFile.exists())
        {
            List<File> nonExistentFiles = new ArrayList<>(Arrays.asList(videoFile.listFiles()));
            if(!nonExistentFiles.isEmpty())
            {
                adapter=new DownloadsAdapter(nonExistentFiles,context);
                downloadsList.setAdapter(adapter);
                downloadsList.setLayoutManager(new GridLayoutManager(context, 2));
                downloadsList.setHasFixedSize(true);
            }
        }

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        adViewDetailAct=findViewById(R.id.adViewDetailAct);
        AudienceNetworkAds.initialize(this);
        if( getResources().getString(R.string.Ads).equals("ADMOB") ){
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewDetailAct.loadAd(adRequest);
        }
        else if (getResources().getString(R.string.Ads).equals("FACEBOOK")){
            adViewDetailAct.setVisibility(View.GONE);
            fbBannerContainerDetail=new com.facebook.ads.AdView(this, getResources().getString(R.string.FBBannerAdPlacemaneId_2), com.facebook.ads.AdSize.BANNER_HEIGHT_50);
            LinearLayout adContainer = (LinearLayout) findViewById(R.id.fbbannerContainerDetail);
            adContainer.addView(fbBannerContainerDetail);
            fbBannerContainerDetail.loadAd();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            DownloadsActivity.this.onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }
}