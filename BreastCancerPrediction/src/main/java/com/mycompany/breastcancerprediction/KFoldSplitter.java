package com.mycompany.breastcancerprediction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KFoldSplitter<T> {
    private final List<T> data;
    private final int k;

    public KFoldSplitter(List<T> data, int k) {
        if (data == null || k < 2 || k > data.size()) {
            throw new IllegalArgumentException();
        }
        this.data = data;
        this.k = k;
    }

    public List<List<T>> split() {
        List<List<T>> folds = new ArrayList<>();
        List<T> copy = new ArrayList<>(data);
        Collections.shuffle(copy);

        int foldSize = data.size() / k;
        int remainder = data.size() % k;

        int startIndex = 0;
        for (int i = 0; i < k; i++) {
            int endIndex = startIndex + foldSize;
            if (i < remainder) {
                endIndex++;
            }
            List<T> fold = new ArrayList<>(copy.subList(startIndex, endIndex));
            folds.add(fold);
            startIndex = endIndex;
        }

        return folds;
    }
}