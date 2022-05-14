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
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    MainViewModel viewModel;

    Button btnStartStop;
    TextView tvTimer;
    LineChart realTimeChart, finalChart;
    private int VEL_MAX = 5;
    private int ACCEL_MAX = 10;
    private int DIST_MAX = 5;
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
        realTimeChart = findViewById(R.id.real_time_line_chart);
        realTimeChart.setTouchEnabled(true);
        realTimeChart.setPinchZoom(false);

        LineDataSet realTimeLineDataSet = new LineDataSet(new ArrayList<>(), "Ruggedness");
        LineData realTimeLineData = new LineData(realTimeLineDataSet);
        realTimeChart.setData(realTimeLineData);

        finalChart = findViewById(R.id.final_line_chart);
        finalChart.setTouchEnabled(true);
        finalChart.setPinchZoom(true);
        finalChart.setVisibility(View.INVISIBLE);

        LineDataSet finalLineDataSet = new LineDataSet(new ArrayList<>(), "Final Ruggedness");
        LineData finalLineData = new LineData(finalLineDataSet);
        finalChart.setData(finalLineData);
        btnStartStop.setOnClickListener(view -> viewModel.onStartStopClicked());
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
        viewModel.showingFinalEntriesLiveData.observe(
                MainActivity.this,
                allEntries -> {

                    if (allEntries.size() > 0) {
                        finalChart.setVisibility(View.VISIBLE);
                        realTimeChart.setVisibility(View.INVISIBLE);
                        LineDataSet lineDataSet = new LineDataSet(allEntries, "Final Ruggedness");
                        lineDataSet.setDrawCircles(false);
                        LineData data = new LineData(lineDataSet);
                        data.setDrawValues(false);
                        finalChart.setData(data);
                        finalChart.notifyDataSetChanged();
                        finalChart.invalidate();

//                        finalChart.getAxisLeft().setAxisMinimum(-DIST_MAX);
//                        finalChart.getAxisLeft().setAxisMaximum(DIST_MAX);

                        finalChart.getAxisRight().setEnabled(false);
                    } else {
                        finalChart.setVisibility(View.INVISIBLE);
                    }
                }
        );
        viewModel.showingRealTimeEntriesLiveData.observe(
                MainActivity.this,
                entries -> {
                    if (entries.size() > 0) {
                        LineDataSet lineDataSet = new LineDataSet(entries, "Ruggedness");
                        LineData data = new LineData(lineDataSet);
                        data.setValueTextColor(Color.BLUE);
                        data.setDrawValues(false);
                        realTimeChart.setData(data);
                        realTimeChart.notifyDataSetChanged();
                        realTimeChart.invalidate();
                        realTimeChart.setVisibility(View.VISIBLE);
                        realTimeChart.getAxisLeft().setAxisMinimum(-DIST_MAX);
                        realTimeChart.getAxisLeft().setAxisMaximum(DIST_MAX);


                        realTimeChart.getAxisRight().setEnabled(false);
                        finalChart.setVisibility(View.INVISIBLE);
                        // chart.getAxisRight().setAxisMinimum(20);
                        // chart.getAxisRight().setAxisMaximum(20);
                    } else {
                        realTimeChart.setVisibility(View.INVISIBLE);
                    }
                }
        );
    }
}