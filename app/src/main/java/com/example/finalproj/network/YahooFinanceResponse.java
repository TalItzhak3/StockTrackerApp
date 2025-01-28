package com.example.finalproj.network;

import java.util.List;

public class YahooFinanceResponse {
    private Chart chart;

    public static class Chart {
        private List<Result> result;
        private String error;

        public List<Result> getResult() { return result; }
        public String getError() { return error; }
    }

    public static class Result {
        private List<Long> timestamp;
        private Indicators indicators;

        public List<Long> getTimestamp() { return timestamp; }
        public Indicators getIndicators() { return indicators; }
    }

    public static class Indicators {
        private List<Quote> quote;

        public List<Quote> getQuote() { return quote; }
    }

    public static class Quote {
        private List<Double> close;
        private List<Double> open;
        private List<Double> high;
        private List<Double> low;
        private List<Long> volume;

        public List<Double> getClose() { return close; }
        public List<Double> getOpen() { return open; }
        public List<Double> getHigh() { return high; }
        public List<Double> getLow() { return low; }
        public List<Long> getVolume() { return volume; }
    }

    public Chart getChart() { return chart; }
}