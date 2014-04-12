package com.example.ecgsimulator;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PlaceholderFragment extends Fragment
{
  private SeekBar seekbar;
  private Button uIButton;
  private InterestingEvent ie;

  
  public PlaceholderFragment()
  {
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState)
  {
    View rootView = inflater
        .inflate(R.layout.fragment_main, container, false);
    //TextView value = (TextView) findViewById(R.id.textview);
    
    uIButton = (Button) rootView.findViewById(R.id.sendBadST);
    updateSTSegmentButtonColor(Color.GREEN);
    
    seekbar = (SeekBar) rootView.findViewById(R.id.seekbar);
    seekbar.setOnSeekBarChangeListener( new OnSeekBarChangeListener()
    {
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
      {
        // TODO Auto-generated method stub
        updateTextValue("SeekBar value is "+ progress);
        ie.sliderChanged(progress);
      }

      public void onStartTrackingTouch(SeekBar seekBar)
      {
        // TODO Auto-generated method stub
      }


      public void onStopTrackingTouch(SeekBar seekBar)
      {
        // TODO Auto-generated method stub
      }
    });
    
    return rootView;
  }
  
  public void updateSTSegmentButtonColor(int color) {
    uIButton.setBackgroundColor(color);
  }
  
  public void updateTextValue(String text) {
    TextView value = (TextView) getView().findViewById(R.id.textview);
    value.setText(text);
  }
  
  public void setUpCallBack(InterestingEvent event)
  {
    ie = event;
  }
}
