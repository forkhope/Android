package com.example.myfragment;

import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.app.Fragment;
import android.widget.TextView;
import android.util.Log;

public class ArticleFragment extends Fragment {
    public final static String ARG_POSITION = "position";

    private final static String articles[] = {
        "The first article", "The second article"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        /* Fragment类没有 findViewById() 函数,该函数定义在Activity类中 */
        // TextView textview = (TextView) getActivity().findViewById(R.id.article_text);
        TextView textview = new TextView(getActivity());
        textview.setTextSize(40);
        Log.d("ArticleFragment", "getArguments(): " + getArguments());
        int position = getArguments().getInt(ARG_POSITION, 0) % 2;
        textview.setText(articles[position]);

        return textview;
        // return inflater.inflate(R.layout.article_view, container, false);
    }
}
