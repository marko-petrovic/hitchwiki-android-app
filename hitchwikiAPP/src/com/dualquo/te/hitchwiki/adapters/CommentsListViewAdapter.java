package com.dualquo.te.hitchwiki.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.dualquo.te.hitchwiki.R;
import com.dualquo.te.hitchwiki.entities.PlaceInfoCompleteComment;
import com.dualquo.te.hitchwiki.misc.Utils;

public class CommentsListViewAdapter extends ArrayAdapter<PlaceInfoCompleteComment> 
{
  private Context context;
  private ArrayList<PlaceInfoCompleteComment> comments;
  private Typeface font; 

  public CommentsListViewAdapter(Context context, ArrayList<PlaceInfoCompleteComment> commentsForMarker) 
  {
    super(context, R.layout.rowlayout_comment, commentsForMarker);
    this.context = context;
    this.comments = commentsForMarker;
    
    font = Typeface.createFromAsset(context.getAssets(), "fonts/ubuntucondensed.ttf");
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) 
  {
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    
    View rowView = inflater.inflate(R.layout.rowlayout_comment, parent, false);
    
    TextView commentUserNameRow = (TextView) rowView.findViewById(R.id.rowlayout_comment_userName);
    TextView commentTimestampRow = (TextView) rowView.findViewById(R.id.rowlayout_comment_timestamp);
    TextView commentTextRow = (TextView) rowView.findViewById(R.id.rowlayout_comment_text);
    
    commentUserNameRow.setText("by " + comments.get(position).getUserName());
    commentTimestampRow.setText(comments.get(position).getDatetime());
    commentTextRow.setText(Utils.stringBeautifier(comments.get(position).getComment()));
    
    //setting proper font and size
    commentUserNameRow.setTypeface(font);
    commentUserNameRow.setTextSize(14);
    commentUserNameRow.setTextColor(Color.DKGRAY);
    
    commentTimestampRow.setTypeface(font);
    commentTimestampRow.setTextSize(14);
    commentTimestampRow.setTextColor(Color.DKGRAY);
    
    commentTextRow.setTypeface(font);
    commentTextRow.setTextSize(18);
    commentTextRow.setTextColor(Color.BLACK);

    return rowView;
  }
} 