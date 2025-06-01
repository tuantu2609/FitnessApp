package com.example.fitnesstrackerapp.util;

import android.os.AsyncTask;

public class CalorieTask extends AsyncTask<Void, Void, double[]> {

    private final int steps;
    private final Callback callback;

    public interface Callback {
        void onResult(double calories, int points);
    }

    public CalorieTask(int steps, Callback callback) {
        this.steps = steps;
        this.callback = callback;
    }

    @Override
    protected double[] doInBackground(Void... voids) {
        double calories = steps * 0.04;
        int points = steps / 100;
        return new double[] { calories, points };
    }

    @Override
    protected void onPostExecute(double[] result) {
        if (callback != null) {
            callback.onResult(result[0], (int) result[1]);
        }
    }
}
