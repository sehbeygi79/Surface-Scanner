package com.cps2.ca2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    MainViewModel viewModel;

    Button btnStartStop;
    TextView tvTimer;
    LineChart chart;

    LineDataSet lineDataSet;
    LineData lineData;
    private int VEL_MAX = 5;
    private int ACCEL_MAX = 10;
    private int DIST_MAX = 2;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        initViews();
        initObservers();
    }

    void initViews() {
        btnStartStop = findViewById(R.id.btn_start_stop);
        tvTimer = findViewById(R.id.tv_timer);
        chart = findViewById(R.id.line_chart);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(false);
        lineDataSet = new LineDataSet(new ArrayList<>(), "asdf");
        lineData = new LineData(lineDataSet);
        chart.setData(lineData);
        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewModel.onStartStopClicked();
            }
        });
    }

    void initObservers() {
        viewModel.startStopButtonTextLiveData.observe(
                MainActivity.this,
                buttonText -> {
                    btnStartStop.setText(buttonText);
                }
        );
        viewModel.timerTextLiveData.observe(
                MainActivity.this,
                timerText -> tvTimer.setText(timerText)
        );
        viewModel.showingEntriesLiveData.observe(
                MainActivity.this,
                entries -> {
                    if (entries.size() > 0) {
                        LineDataSet lineDataSet = new LineDataSet(entries, "asdf");
                        LineData data = new LineData(lineDataSet);
                        data.setValueTextColor(Color.BLUE);
                        chart.setData(data);
                        chart.notifyDataSetChanged();
                        chart.invalidate();
                        chart.setVisibility(View.VISIBLE);
                        chart.getAxisLeft().setAxisMinimum(-DIST_MAX);
                        chart.getAxisLeft().setAxisMaximum(DIST_MAX);

                        chart.getAxisRight().setEnabled(false);
                        // chart.getAxisRight().setAxisMinimum(20);
                        // chart.getAxisRight().setAxisMaximum(20);
                    } else {
                        chart.setVisibility(View.INVISIBLE);
                    }
                }
        );
    }
}