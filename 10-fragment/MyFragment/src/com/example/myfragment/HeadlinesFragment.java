package com.example.myfragment;

import android.os.Bundle;
import android.app.ListFragment;
import android.app.Activity;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.util.Log;

public class HeadlinesFragment extends ListFragment {
    public String items[] = {
        "article 1",
        "article 2",
    };

    public OnHeadlinesSelectedListener mCallback;

    public interface OnHeadlinesSelectedListener {
        public void onArticleSelected(int position);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d("life-Fragment", "HeadlinesFragment: onCreateView");
        return inflater.inflate(R.layout.headlines_view, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d("life-Fragment", "HeadlinesFragment: onAttach");

        try {
            mCallback = (OnHeadlinesSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement OnHeadlinesSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("life-Fragment", "HeadlinesFragment: onCreate");
        setListAdapter(new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1, items));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("life-Fragment", "HeadlinesFragment: onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("life-Fragment", "HeadlinesFragment: onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("life-Fragment", "HeadlinesFragment: onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("life-Fragment", "HeadlinesFragment: onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("life-Fragment", "HeadlinesFragment: onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("life-Fragment", "HeadlinesFragment: onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("life-Fragment", "HeadlinesFragment: onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("life-Fragment", "HeadlinesFragment: onDetach");
    }

    public void onListItemClick(ListView parent, View v,
        int position, long id) {
        mCallback.onArticleSelected(position);
    }
}
