package com.example.myfragment;

import android.app.Activity;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.app.FragmentManager;
import android.util.Log;

public class MainActivity extends Activity
    implements HeadlinesFragment.OnHeadlinesSelectedListener {
    private final static String LOG_TAG = "life-Activity";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "1: >>>>>>>>>> onCreate");
        setContentView(R.layout.main);
        
        if (findViewById(R.id.fragment_container) != null) {
            Log.d(LOG_TAG, "onCreate: savedInstanceState: " + savedInstanceState);
            if (savedInstanceState != null) {
                return;
            }

            HeadlinesFragment firstFragment = new HeadlinesFragment();

            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fragment_container, firstFragment).commit();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(LOG_TAG, "7: >>>>>>>>> onRestart");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "2: >>>>>>>>> onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "3: >>>>>>>>> onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "4: <<<<<<<<< onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "5: <<<<<<<<< onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "6: <<<<<<<<< onDestroy");
    }

    public void onArticleSelected(int position) {
        ArticleFragment newFragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putInt(ArticleFragment.ARG_POSITION, position);
        newFragment.setArguments(args);

        /* 之前 fragmentTransaction 执行过一次 commit() 函数,下面想要再次执行
         * commit()函数时,需要先执行一次beginTransaction().如果不这么做,运行时异常
         */
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, newFragment);
        fragmentTransaction.addToBackStack(null);

        fragmentTransaction.commit();
    }
}
