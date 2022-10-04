package com.actionadblocker.kundan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.anthonycr.progress.AnimatedProgressBar;
import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.actionadblocker.kundan.Adapters.AutoCompleteAdapter;
import com.actionadblocker.kundan.Configs.SettingsManager;
import com.actionadblocker.kundan.Grabber.AdBlocker;
import com.actionadblocker.kundan.Utils.Commons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Timer;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    private AdBlocker adBlock;
    AnimatedProgressBar loadingPageProgress;
    CountDownTimer countDownTimer;
    AutoCompleteTextView etSearchBar;
    WebView simpleWebView;
    Context mContext;
    Timer timer=null;
    private SSLSocketFactory defaultSSLSF;

    Boolean isAllFabsVisible;
    ImageView btnHome, btnSearch,btnSearchCancel,btnSettings;

    private boolean isRedirected;
    AdView adViewMainAct;

    com.facebook.ads.AdView fbAdView;
    AppUpdateManager mAppUpdateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext=this;
        defaultSSLSF=HttpsURLConnection.getDefaultSSLSocketFactory();
        initComponents();
        setButtonClickEvents();
        wvGoToHome();
        try {
            onSharedIntent();
        }
        catch (Exception ex)
        {}
        checkAppUpdate();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        AudienceNetworkAds.initialize(this);

//Admob Ad added :
        if( getResources().getString(R.string.Ads).equals("ADMOB") ){
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewMainAct.loadAd(adRequest);
        }
        else if (getResources().getString(R.string.Ads).equals("FACEBOOK")){
            adViewMainAct.setVisibility(View.GONE);
            fbAdView=new com.facebook.ads.AdView(this, getResources().getString(R.string.FBBannerAdPlacemaneId_1), com.facebook.ads.AdSize.BANNER_HEIGHT_50);
            LinearLayout adContainer = (LinearLayout) findViewById(R.id.fbBannerContainer);
            adContainer.addView(fbAdView);
            fbAdView.loadAd();
        }


        havePermissionForWriteStorage();

        Intent appLinkIntent = getIntent();
        Uri appLinkData = appLinkIntent.getData();
        if (appLinkData != null)
        {
            etSearchBar.setText(appLinkData.toString());
            navigateBrowser();
        }
        PrepareForAdBlockers();
    }


    // To share the application
    private void onSharedIntent()
    {
        Intent receiverdIntent = getIntent();
        String receivedAction = receiverdIntent.getAction();
        String receivedType = receiverdIntent.getType();
        if (receivedAction.equals(Intent.ACTION_SEND))
        {
            if (receivedType.startsWith("text/"))
            {
                String receivedText = receiverdIntent
                        .getStringExtra(Intent.EXTRA_TEXT);
                if (receivedText != null) {
                    CheckUrls(receivedText);
                }
            }
        }
    }

    private void CheckUrls(String text){
        List<String> result= Commons.extractUrls(text);
        if(result.size()==0)
        {
            new SweetAlertDialog(mContext, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText(mContext.getString(R.string.Wait))
                    .setContentText(mContext.getString(R.string.NoUrlFound))
                    .show();
        }
        else
        {
            etSearchBar.setText(result.get(0));
            navigateBrowser();
        }
    }

    private void PrepareForAdBlockers() {

        File file = new File(mContext.getFilesDir(), "ad_filters.dat");
        try {
            if (file.exists()) {
                Log.d("debug", "file exists");
                FileInputStream fileInputStream = new FileInputStream(file);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                adBlock = (AdBlocker) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
            } else {
                adBlock = new AdBlocker();
                Log.d("debug", "file not exists");
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(adBlock);
                objectOutputStream.close();
                fileOutputStream.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            adBlock = new AdBlocker();
        }
        updateAdFilters();
    }

    private boolean havePermissionForWriteStorage() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.d("Permission Allowed", "true");
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 950);
                return false;
            } else {
                initFolers();
                return true;
            }
        } else {
            initFolers();
            return true;
        }
    }


    public void updateAdFilters() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                adBlock.update(mContext);
            }
        });

    }
    public boolean checkUrlIfAds(String url) {
        return adBlock.checkThroughFilters(url);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 950:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initFolers();
                } else {
                    Toast.makeText(mContext,getString(R.string.Permissiondenied), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    private void initFolers()
    {
        try {
            mkdirs(new File(SettingsManager.DOWNLOAD_FOLDER));
        }
        catch (Exception ex)
        {

        }
    }
    private void mkdirs(File dir)
    {
        if(!dir.exists())
            dir.mkdir();
    }




    private void  wvGoToHome(){
        simpleWebView.loadUrl( getResources().getString(R.string.index_page) );
    }

    private void initComponents(){
        loadingPageProgress=findViewById(R.id.loadingPageProgress);
        simpleWebView=findViewById(R.id.simpleWebView);
        simpleWebView.getSettings().setJavaScriptEnabled(true);
        simpleWebView.getSettings().setDomStorageEnabled(true);
        simpleWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        simpleWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        simpleWebView.setWebViewClient(new customWebClient());


        simpleWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(url));
                request.setMimeType(mimetype);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading file...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition,mimetype));
                request.allowScanningByMediaScanner();

                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS , "/" + SettingsManager.DOWNLOAD_FOLDER_DIR_NAME  + "/"+ URLUtil.guessFileName(
                                url, contentDisposition, mimetype));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "Downloading File",
                        Toast.LENGTH_LONG).show();

            }
        });

       adViewMainAct=findViewById(R.id.adViewMainAct);
        etSearchBar=findViewById(R.id.etSearchBar);
        int layout = android.R.layout.simple_list_item_1;
        AutoCompleteAdapter adapter = new AutoCompleteAdapter (mContext, layout);
        etSearchBar.setAdapter(adapter);


        btnHome=findViewById(R.id.btnHome);
        btnSearchCancel=findViewById(R.id.btnSearchCancel);
        btnSearch=findViewById(R.id.btnSearch);
        btnSettings=findViewById(R.id.btnSettings);

        registerForContextMenu(btnSettings);

        isAllFabsVisible=false;
    }
    @Override
    public void onCreateContextMenu(
            ContextMenu menu,
            View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.toolbar,menu);
    }



    private void setButtonClickEvents(){


        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wvGoToHome();
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateBrowser();
            }
        });

        btnSearchCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSearchbarText("");
            }
        });

        etSearchBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(hasFocus && etSearchBar.getText().toString().equals(getResources().getString(R.string.home)) ){
                    setSearchbarText("");
                }
            }
        });

        etSearchBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_ENTER:
                            navigateBrowser();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    btnSettings.showContextMenu(0,0);
                }
                else
                {
                    btnSettings.showContextMenu();
                }
            }
        });

        etSearchBar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                navigateBrowser();
            }
        });
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

// Menu item
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_pp: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle(getString(R.string.PrivacyPolicy));
                WebView webView = new WebView(this);
                webView.loadUrl("file:///android_asset/privacypolicy.html");
                webView.setWebViewClient(new WebViewClient());
                alert.setView(webView);
                alert.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                alert.show();
                return true;
            }
            case R.id.action_share_app:
            {
                Intent shareIntent =new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String ShareBody="Hi \nPlease check this Awesome Application. '"+ getResources().getString(R.string.app_name) +"'\nYou'll love it. \n\nhttps://play.google.com/store/apps/details?id=" + getPackageName();
                String ShareSub=getString(R.string.hithere);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT,ShareSub);
                shareIntent.putExtra(Intent.EXTRA_TEXT,ShareBody);
                startActivity( Intent.createChooser(shareIntent ,getString(R.string.Shareusing)) );
                return true;
            }
            case R.id.action_Downloads:
            {
                Intent intent = new Intent(this, DownloadsActivity.class);
                startActivity(intent);
                return true;
            }

        }
        return true;
    }


    //Search Browser

    private void navigateBrowser(){
        hideKeyboard();
        if(! Patterns.WEB_URL.matcher(etSearchBar.getText()).matches())
        {
            etSearchBar.setText("https://www.google.com/search?q=" + etSearchBar.getText());
        }
        simpleWebView.loadUrl(etSearchBar.getText().toString());
    }

    private void  startTimer(){
        final int secs = 10;
        loadingPageProgress.setProgress(0);
        countDownTimer= new CountDownTimer((secs +1) * 100, 1000)
        {
            @Override
            public final void onTick(final long millisUntilFinished)
            {
                if(loadingPageProgress.getProgress() < 80){
                    loadingPageProgress.setProgress(loadingPageProgress.getProgress() + 8);
                }
            }
            @Override
            public final void onFinish()
            {

            }
        };
        countDownTimer.start();
        loadingPageProgress.setVisibility(View.VISIBLE);
    }
    private void stopTimer(){
        try {
            countDownTimer.cancel();
        }
        catch (Exception ex){}
        loadingPageProgress.setProgress(100);

        new Handler(Looper.myLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                loadingPageProgress.setProgress(0);
                loadingPageProgress.setVisibility(View.GONE);

            }
        }, 500);

    }

    private void setSearchbarText(String text){
        if(text.equals(getResources().getString(R.string.index_page))){
            text=getResources().getString(R.string.home);
        }
        if(text.equals("")){
            btnSearchCancel.setVisibility(View.INVISIBLE);
            etSearchBar.requestFocus();
        }
        else
        {
            btnSearchCancel.setVisibility(View.VISIBLE);
        }
        etSearchBar.setText(text);
    }


    public class customWebClient extends WebViewClient
    {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String request)
        {
            String url=request;
            if ( (url.contains("ad") || url.contains("banner") || url.contains("pop")) && checkUrlIfAds(url)) {
                return new WebResourceResponse(null, null, null);
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url.startsWith("intent://")) {
                try {
                    Context context = simpleWebView.getContext();
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        PackageManager packageManager = context.getPackageManager();
                        ResolveInfo info = packageManager.resolveActivity(intent,
                                PackageManager.MATCH_DEFAULT_ONLY);
                        if ((intent != null) && ((intent.getScheme().equals("https"))
                                || (intent.getScheme().equals("http")))) {
                            String fallbackUrl = intent.getStringExtra(
                                    "browser_fallback_url");
                            simpleWebView.loadUrl(fallbackUrl);
                            return true;
                        }
                        if (info != null) {
                            context.startActivity(intent);
                        } else {
                            String fallbackUrl = intent.getStringExtra(
                                    "browser_fallback_url");
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(fallbackUrl));
                            context.startActivity(browserIntent);
                        }
                        return true;
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            startTimer();
            isRedirected = false;
            super.onPageStarted(view, url, favicon);
            if(timer !=null)
            {
                timer.cancel();
                timer=null;
            }
            setSearchbarText(url);
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            if (!isRedirected){
                stopTimer();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return super.shouldOverrideUrlLoading(view, request);
        }

    }

    public void popupSnackbarForCompleteUpdate() {
        try {
            Snackbar make = Snackbar.make(findViewById(R.id.simpleWebView), (CharSequence) "An update has just been downloaded.", 2);
            make.setAction((CharSequence) "RESTART", (View.OnClickListener) new View.OnClickListener() {
                public void onClick(View view) {
                    if (MainActivity.this.mAppUpdateManager != null) {
                        MainActivity.this.mAppUpdateManager.completeUpdate();
                    }
                }
            });
            make.setDuration(50000);
            make.show();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    InstallStateUpdatedListener installStateUpdatedListener = new InstallStateUpdatedListener() {
        public void onStateUpdate(InstallState installState) {
            try {
                if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                    MainActivity.this.popupSnackbarForCompleteUpdate();
                } else if (installState.installStatus() != InstallStatus.INSTALLED) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("InstallStateUpdatedListener: state: ");
                    sb.append(installState.installStatus());
                    Log.i("MainActivity", sb.toString());
                } else if (MainActivity.this.mAppUpdateManager != null) {
                    MainActivity.this.mAppUpdateManager.unregisterListener(MainActivity.this.installStateUpdatedListener);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void checkAppUpdate() {
        try {
            this.mAppUpdateManager = AppUpdateManagerFactory.create(this);
            this.mAppUpdateManager.registerListener(this.installStateUpdatedListener);
            this.mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
                public void onSuccess(AppUpdateInfo appUpdateInfo) {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        try {
                            MainActivity.this.mAppUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE, MainActivity.this, 201);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                    } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        MainActivity.this.popupSnackbarForCompleteUpdate();
                    } else {
                        Log.e("MainActivity", "checkForAppUpdateAvailability: something else");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (simpleWebView.copyBackForwardList().getCurrentIndex() > 0) {
            simpleWebView.goBack();
        }
        else {
            super.onBackPressed();
        }
    }

}